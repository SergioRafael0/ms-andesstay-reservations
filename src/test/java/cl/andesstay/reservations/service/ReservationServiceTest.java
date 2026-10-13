package cl.andesstay.reservations.service;

import cl.andesstay.reservations.dto.ReservationDTO;
import cl.andesstay.reservations.dto.ReservationEventMessage;
import cl.andesstay.reservations.exception.BusinessException;
import cl.andesstay.reservations.model.Reservation;
import cl.andesstay.reservations.model.Reservation.ReservationStatus;
import cl.andesstay.reservations.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas de la máquina de estados de reservas AndesStay (EP1).
 * Regla central: NO se puede hacer check-in sin haber confirmado antes la reserva.
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository repository;

    @Mock
    private ReservationEventPublisher eventPublisher;

    private ReservationService service;

    @BeforeEach
    void setUp() {
        service = new ReservationService(repository, eventPublisher);
    }

    private Reservation reservaConEstado(ReservationStatus status) {
        Reservation r = new Reservation();
        r.setId(1L);
        r.setUnitId(1L);
        r.setGuestName("Huésped Prueba");
        r.setGuestOid("oid-test");
        r.setCheckIn(LocalDate.now().plusDays(5));
        r.setCheckOut(LocalDate.now().plusDays(8));
        r.setStatus(status);
        return r;
    }

    private void transicionPermitida(ReservationStatus desde, ReservationStatus hacia) {
        Reservation r = reservaConEstado(desde);
        when(repository.findById(1L)).thenReturn(Optional.of(r));
        when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        ReservationDTO dto = service.changeStatus(1L, hacia.name());
        assertEquals(hacia, ReservationStatus.valueOf(dto.getStatus()));
    }

    private void transicionRechazada(ReservationStatus desde, ReservationStatus hacia) {
        Reservation r = reservaConEstado(desde);
        when(repository.findById(1L)).thenReturn(Optional.of(r));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.changeStatus(1L, hacia.name()));
        assertNotNull(ex.getMessage());
    }

    @Test
    void flujoCompletoDeEstadosEsValido() {
        transicionPermitida(ReservationStatus.CREADA, ReservationStatus.CONFIRMADA);
        transicionPermitida(ReservationStatus.CONFIRMADA, ReservationStatus.CHECKIN_PENDIENTE);
        transicionPermitida(ReservationStatus.CHECKIN_PENDIENTE, ReservationStatus.EN_ESTADIA);
        transicionPermitida(ReservationStatus.EN_ESTADIA, ReservationStatus.CHECKOUT);
    }

    @Test
    void reglaCentral_noSePuedeHacerCheckinSinConfirmar() {
        // Desde CREADA no se puede saltar directo a CHECKIN_PENDIENTE ni a EN_ESTADIA
        transicionRechazada(ReservationStatus.CREADA, ReservationStatus.CHECKIN_PENDIENTE);
        transicionRechazada(ReservationStatus.CREADA, ReservationStatus.EN_ESTADIA);
        // Desde CREADA tampoco se puede ir a CHECKOUT directamente
        transicionRechazada(ReservationStatus.CREADA, ReservationStatus.CHECKOUT);
    }

    @Test
    void desdeCreadaSoloSePermiteConfirmarOCancelar() {
        transicionPermitida(ReservationStatus.CREADA, ReservationStatus.CONFIRMADA);
        transicionPermitida(ReservationStatus.CREADA, ReservationStatus.CANCELADA);
        transicionRechazazada_checkin(ReservationStatus.CREADA);
    }

    private void transicionRechazazada_checkin(ReservationStatus desde) {
        transicionRechazada(desde, ReservationStatus.CHECKIN_PENDIENTE);
    }

    @Test
    void desdeConfirmadaNoSePuedeVolverACrearNiSaltarAEstadia() {
        transicionRechazada(ReservationStatus.CONFIRMADA, ReservationStatus.CREADA);
        transicionRechazada(ReservationStatus.CONFIRMADA, ReservationStatus.EN_ESTADIA);
        transicionPermitida(ReservationStatus.CONFIRMADA, ReservationStatus.CANCELADA);
    }

    @Test
    void noSePuedeCancelarDuranteLaEstadiaNiTrasCheckout() {
        transicionRechazada(ReservationStatus.EN_ESTADIA, ReservationStatus.CANCELADA);
        transicionRechazada(ReservationStatus.CHECKOUT, ReservationStatus.CANCELADA);
        transicionPermitida(ReservationStatus.CHECKIN_PENDIENTE, ReservationStatus.CANCELADA);
    }

    @Test
    void estadosTerminalesNoAceptanCambios() {
        transicionRechazada(ReservationStatus.CHECKOUT, ReservationStatus.CONFIRMADA);
        transicionRechazada(ReservationStatus.CANCELADA, ReservationStatus.CONFIRMADA);
        transicionRechazada(ReservationStatus.CHECKOUT, ReservationStatus.CHECKOUT);
    }

    @Test
    void noSePermiteRepetirElMismoEstado() {
        transicionRechazada(ReservationStatus.CONFIRMADA, ReservationStatus.CONFIRMADA);
        transicionRechazada(ReservationStatus.CREADA, ReservationStatus.CREADA);
    }

    @Test
    void estadoInvalidoRechazaLaTransicion() {
        Reservation r = reservaConEstado(ReservationStatus.CREADA);
        when(repository.findById(1L)).thenReturn(Optional.of(r));
        assertThrows(BusinessException.class, () -> service.changeStatus(1L, "NO_EXISTE"));
    }

    @Test
    void crearReservaFuerzaEstadoCreada() {
        when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        ReservationDTO dto = new ReservationDTO();
        dto.setGuestName("María López");
        dto.setGuestOid("oid-1");
        dto.setUnitId(2L);
        dto.setCheckIn(LocalDate.now().plusDays(10));
        dto.setCheckOut(LocalDate.now().plusDays(12));
        ReservationDTO saved = service.create(dto);
        assertEquals("CREADA", saved.getStatus());
    }

    @Test
    void crearReservaConFechasInvalidasFalla() {
        ReservationDTO dto = new ReservationDTO();
        dto.setGuestName("María López");
        dto.setCheckIn(LocalDate.now().plusDays(10));
        dto.setCheckOut(LocalDate.now().plusDays(10)); // igual, no posterior
        assertThrows(BusinessException.class, () -> service.create(dto));

        ReservationDTO dto2 = new ReservationDTO();
        dto2.setGuestName("María López");
        assertThrows(BusinessException.class, () -> service.create(dto2)); // sin fechas
    }

    @Test
    void reservaInexistenteLanzaErrorDeNegocio() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.findById(99L));
    }

    @Test
    void confirmarReservaPublicaEmailVoucherYNotificacionTopic() {
        Reservation r = reservaConEstado(ReservationStatus.CREADA);
        when(repository.findById(1L)).thenReturn(Optional.of(r));
        when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, "CONFIRMADA");

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher, times(3)).publish(
                any(ReservationEventMessage.class), exchangeCaptor.capture(), routingKeyCaptor.capture());

        assertTrue(routingKeyCaptor.getAllValues().contains("email.send"));
        assertTrue(routingKeyCaptor.getAllValues().contains("voucher.gen"));
        assertTrue(routingKeyCaptor.getAllValues().contains("email.urgent"));
        assertTrue(exchangeCaptor.getAllValues().contains("cmd.direct"));
        assertTrue(exchangeCaptor.getAllValues().contains("cmd.topic"));
    }

    @Test
    void checkoutPublicaTicketDeHousekeeping() {
        Reservation r = reservaConEstado(ReservationStatus.EN_ESTADIA);
        when(repository.findById(1L)).thenReturn(Optional.of(r));
        when(repository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.changeStatus(1L, "CHECKOUT");

        verify(eventPublisher).publish(any(ReservationEventMessage.class),
                eq("cmd.direct"), eq("housekeeping.ticket"));
    }

    @Test
    void transicionRechazadaNoPublicaEventos() {
        Reservation r = reservaConEstado(ReservationStatus.CREADA);
        when(repository.findById(1L)).thenReturn(Optional.of(r));

        assertThrows(BusinessException.class,
                () -> service.changeStatus(1L, "CHECKIN_PENDIENTE"));
        verifyNoInteractions(eventPublisher);
    }
}
