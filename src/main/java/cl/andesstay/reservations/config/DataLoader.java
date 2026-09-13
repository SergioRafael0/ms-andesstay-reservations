package cl.andesstay.reservations.config;

import cl.andesstay.reservations.model.Reservation;
import cl.andesstay.reservations.repository.ReservationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class DataLoader {
    @Bean
    public CommandLineRunner initData(ReservationRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.saveAll(List.of(
                    build(1L, "Huésped Demo 1", "oid-demo-1", LocalDate.now().plusDays(5), LocalDate.now().plusDays(8), Reservation.ReservationStatus.CONFIRMADA),
                    build(2L, "Huésped Demo 2", "oid-demo-2", LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), Reservation.ReservationStatus.CREADA)
                ));
            }
        };
    }

    private Reservation build(Long unitId, String name, String oid, LocalDate in, LocalDate out, Reservation.ReservationStatus status) {
        Reservation r = new Reservation();
        r.setUnitId(unitId);
        r.setGuestName(name);
        r.setGuestOid(oid);
        r.setCheckIn(in);
        r.setCheckOut(out);
        r.setStatus(status);
        return r;
    }
}
