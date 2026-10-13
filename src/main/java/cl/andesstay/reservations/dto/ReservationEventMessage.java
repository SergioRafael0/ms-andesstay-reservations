package cl.andesstay.reservations.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Contrato del mensaje asíncrono de AndesStay (productor → consumidor).
 *
 * Reglas:
 * - eventId: UUID único del mensaje (trazabilidad e idempotencia en el consumidor).
 * - correlationId: agrupa los mensajes de un mismo proceso de negocio.
 * - No se envían datos sensibles (ni tokens, ni contraseñas, ni datos de pago).
 */
public class ReservationEventMessage {

    private String eventId;
    private String correlationId;
    private String eventType;
    private LocalDateTime occurredAt;

    private Long reservationId;
    private Long unitId;
    private String guestName;
    private String guestOid;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String status;
    private String details;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Long getUnitId() { return unitId; }
    public void setUnitId(Long unitId) { this.unitId = unitId; }
    public String getGuestName() { return guestName; }
    public void setGuestName(String guestName) { this.guestName = guestName; }
    public String getGuestOid() { return guestOid; }
    public void setGuestOid(String guestOid) { this.guestOid = guestOid; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
