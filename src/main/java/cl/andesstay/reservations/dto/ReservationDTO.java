package cl.andesstay.reservations.dto;

import cl.andesstay.reservations.model.Reservation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ReservationDTO {
    private Long id;
    private Long unitId;
    @NotBlank
    private String guestName;
    private String guestOid;
    @NotNull
    private LocalDate checkIn;
    @NotNull
    private LocalDate checkOut;
    private String status;

    public static ReservationDTO fromEntity(Reservation r) {
        ReservationDTO dto = new ReservationDTO();
        dto.id = r.getId();
        dto.unitId = r.getUnitId();
        dto.guestName = r.getGuestName();
        dto.guestOid = r.getGuestOid();
        dto.checkIn = r.getCheckIn();
        dto.checkOut = r.getCheckOut();
        dto.status = r.getStatus().name();
        return dto;
    }

    public Reservation toEntity() {
        Reservation r = new Reservation();
        r.setUnitId(this.unitId);
        r.setGuestName(this.guestName);
        r.setGuestOid(this.guestOid);
        r.setCheckIn(this.checkIn);
        r.setCheckOut(this.checkOut);
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
}
