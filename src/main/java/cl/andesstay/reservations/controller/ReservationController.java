package cl.andesstay.reservations.controller;

import cl.andesstay.reservations.dto.ReservationDTO;
import cl.andesstay.reservations.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReservationDTO> list(@RequestParam(required = false) String guestOid) {
        if (guestOid != null && !guestOid.isEmpty()) {
            return service.findByGuestOid(guestOid);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ReservationDTO getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDTO create(@RequestBody @Valid ReservationDTO dto) {
        return service.create(dto);
    }

    @PatchMapping("/{id}/status")
    public ReservationDTO changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isEmpty()) {
            throw new IllegalArgumentException("Falta el campo 'status' en el body");
        }
        return service.changeStatus(id, newStatus);
    }
}
