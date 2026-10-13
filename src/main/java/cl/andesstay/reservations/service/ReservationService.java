package cl.andesstay.reservations.service;

import cl.andesstay.reservations.dto.ReservationDTO;
import cl.andesstay.reservations.dto.ReservationEventMessage;
import cl.andesstay.reservations.exception.BusinessException;
import cl.andesstay.reservations.model.Reservation;
import cl.andesstay.reservations.model.Reservation.ReservationStatus;
import cl.andesstay.reservations.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository repository;
    private final ReservationEventPublisher eventPublisher;

    public ReservationService(ReservationRepository repository, ReservationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public List<ReservationDTO> findAll() {
        return repository.findAll().stream()
            .map(ReservationDTO::fromEntity)
            .collect(Collectors.toList());
    }

    public List<ReservationDTO> findByGuestOid(String oid) {
        if (oid == null || oid.isEmpty()) return List.of();
        return repository.findByGuestOid(oid).stream()
            .map(ReservationDTO::fromEntity)
            .collect(Collectors.toList());
    }

    public ReservationDTO findById(Long id) {
        Reservation r = repository.findById(id)
            .orElseThrow(() -> new BusinessException("Reserva no encontrada: " + id));
        return ReservationDTO.fromEntity(r);
    }

    public ReservationDTO create(ReservationDTO dto) {
        if (dto.getCheckIn() == null || dto.getCheckOut() == null)
            throw new BusinessException("Fechas inválidas");
        if (!dto.getCheckOut().isAfter(dto.getCheckIn()))
            throw new BusinessException("Check-out debe ser posterior a check-in");

        Reservation r = dto.toEntity();
        r.setStatus(ReservationStatus.CREADA);
        Reservation saved = repository.save(r);
        return ReservationDTO.fromEntity(saved);
    }

    public ReservationDTO changeStatus(Long id, String newStatusStr) {
        Reservation r = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Reserva no encontrada: " + id));

        ReservationStatus newStatus;
        try {
            newStatus = ReservationStatus.valueOf(newStatusStr);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Estado inválido: " + newStatusStr);
        }

        ReservationStatus previousStatus = r.getStatus();
        validateTransition(previousStatus, newStatus);
        r.setStatus(newStatus);
        Reservation saved = repository.save(r);

        // La operación síncrona terminó: ahora se desacoplan las tareas asíncronas.
        publishDomainEvents(saved, previousStatus, newStatus);
        return ReservationDTO.fromEntity(saved);
    }

    /**
     * Publica los comandos asíncronos correspondientes a la transición ejecutada.
     * Solo acciones reales del dominio generan mensajes (guía EP2 §4.6):
     *   → CONFIRMADA        : email de confirmación + generación del voucher
     *   → CHECKIN_PENDIENTE : ticket de housekeeping (preparación de la unidad)
     *   → CHECKOUT          : ticket de housekeeping (limpieza tras la salida)
     *   → CANCELADA         : email de aviso de cancelación
     */
    private void publishDomainEvents(Reservation saved, ReservationStatus from, ReservationStatus to) {
        switch (to) {
            case CONFIRMADA -> {
                eventPublisher.publish(buildMessage(saved, "RESERVATION_CONFIRMED",
                                "Confirmación de reserva AndesStay"),
                        cl.andesstay.reservations.rabbit.RabbitMQTopology.EXCHANGE_DIRECT,
                        cl.andesstay.reservations.rabbit.RabbitMQTopology.RK_EMAIL_SEND);
                eventPublisher.publish(buildMessage(saved, "VOUCHER_GENERATE",
                                "Generación del voucher de la reserva"),
                        cl.andesstay.reservations.rabbit.RabbitMQTopology.EXCHANGE_DIRECT,
                        cl.andesstay.reservations.rabbit.RabbitMQTopology.RK_VOUCHER_GEN);
                // Demostración del exchange tipo topic: key compatible con el
                // patrón "email.*" (casa con "email.urgent").
                eventPublisher.publish(buildMessage(saved, "URGENT_NOTIFICATION",
                                "Notificación prioritaria (demo cmd.topic)"),
                        cl.andesstay.reservations.rabbit.RabbitMQTopology.EXCHANGE_TOPIC,
                        cl.andesstay.reservations.rabbit.RabbitMQTopology.RK_EMAIL_URGENT);
            }
            case CHECKIN_PENDIENTE -> eventPublisher.publish(
                    buildMessage(saved, "HOUSEKEEPING_PREPARATION",
                            "Ticket de preparación de la unidad antes de la llegada"),
                    cl.andesstay.reservations.rabbit.RabbitMQTopology.EXCHANGE_DIRECT,
                    cl.andesstay.reservations.rabbit.RabbitMQTopology.RK_HOUSEKEEPING_TICKET);
            case CHECKOUT -> eventPublisher.publish(
                    buildMessage(saved, "HOUSEKEEPING_CLEANING",
                            "Ticket de limpieza de la unidad tras la salida"),
                    cl.andesstay.reservations.rabbit.RabbitMQTopology.EXCHANGE_DIRECT,
                    cl.andesstay.reservations.rabbit.RabbitMQTopology.RK_HOUSEKEEPING_TICKET);
            case CANCELADA -> eventPublisher.publish(
                    buildMessage(saved, "RESERVATION_CANCELLED",
                            "Aviso de cancelación de la reserva"),
                    cl.andesstay.reservations.rabbit.RabbitMQTopology.EXCHANGE_DIRECT,
                    cl.andesstay.reservations.rabbit.RabbitMQTopology.RK_EMAIL_SEND);
            default -> log.debug("Sin eventos asíncronos para la transición {} → {}", from, to);
        }
    }

    private ReservationEventMessage buildMessage(Reservation r, String eventType, String details) {
        ReservationEventMessage m = new ReservationEventMessage();
        m.setEventId(UUID.randomUUID().toString());
        m.setCorrelationId("res-" + r.getId() + "-" + eventType);
        m.setEventType(eventType);
        m.setOccurredAt(LocalDateTime.now());
        m.setReservationId(r.getId());
        m.setUnitId(r.getUnitId());
        m.setGuestName(r.getGuestName());
        m.setGuestOid(r.getGuestOid());
        m.setCheckIn(r.getCheckIn());
        m.setCheckOut(r.getCheckOut());
        m.setStatus(r.getStatus() != null ? r.getStatus().name() : null);
        m.setDetails(details);
        return m;
    }

    /**
     * Regla de negocio AndesStay:
     * No se puede hacer check-in sin CONFIRMAR.
     * CREADA → CONFIRMADA → CHECKIN_PENDIENTE → EN_ESTADIA → CHECKOUT
     * Cualquier estado puede pasar a CANCELADA (excepto CHECKOUT/EN_ESTADIA).
     */
    private void validateTransition(ReservationStatus current, ReservationStatus next) {
        if (current == next) {
            throw new BusinessException("La reserva ya está en estado " + current);
        }

        if (next == ReservationStatus.CANCELADA) {
            if (current == ReservationStatus.CHECKOUT || current == ReservationStatus.EN_ESTADIA) {
                throw new BusinessException("No se puede cancelar una reserva en " + current);
            }
            return;
        }

        switch (current) {
            case CREADA:
                if (next != ReservationStatus.CONFIRMADA)
                    throw new BusinessException("Desde CREADA solo se puede pasar a CONFIRMADA");
                break;
            case CONFIRMADA:
                if (next != ReservationStatus.CHECKIN_PENDIENTE && next != ReservationStatus.CANCELADA)
                    throw new BusinessException("Desde CONFIRMADA solo se puede pasar a CHECKIN_PENDIENTE (regla: no check-in sin confirmar)");
                break;
            case CHECKIN_PENDIENTE:
                if (next != ReservationStatus.EN_ESTADIA)
                    throw new BusinessException("Desde CHECKIN_PENDIENTE solo se puede pasar a EN_ESTADIA");
                break;
            case EN_ESTADIA:
                if (next != ReservationStatus.CHECKOUT)
                    throw new BusinessException("Desde EN_ESTADIA solo se puede pasar a CHECKOUT");
                break;
            case CHECKOUT:
            case CANCELADA:
                throw new BusinessException("La reserva está en estado terminal: " + current);
        }
    }
}
