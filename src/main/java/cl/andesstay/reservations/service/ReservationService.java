package cl.andesstay.reservations.service;

import cl.andesstay.reservations.dto.ReservationDTO;
import cl.andesstay.reservations.exception.BusinessException;
import cl.andesstay.reservations.model.Reservation;
import cl.andesstay.reservations.model.Reservation.ReservationStatus;
import cl.andesstay.reservations.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
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

        validateTransition(r.getStatus(), newStatus);
        r.setStatus(newStatus);
        return ReservationDTO.fromEntity(repository.save(r));
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
