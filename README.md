# 🏨 AndesStay — Microservicio de Reservas

Caso 5 EP1 — DSY1107 Cloud Native I.

Microservicio **Spring Boot 3** que gestiona las **reservas hoteleras**. Maneja el ciclo de vida completo:

```
CREADA → CONFIRMADA → CHECKIN_PENDIENTE → EN_ESTADIA → CHECKOUT
            ↓               ↓
        CANCELADA       CANCELADA
```

## 📡 Endpoints

| Método | Path                            | Descripción                            |
|--------|---------------------------------|----------------------------------------|
| GET    | `/api/reservations`             | Listar todas (Recep/Admin) o por `?guestOid=` (Huésped) |
| GET    | `/api/reservations/{id}`        | Obtener por ID                         |
| POST   | `/api/reservations`             | Crear reserva (cualquier rol)          |
| PATCH  | `/api/reservations/{id}/status` | Cambiar estado (Recep/Admin)           |

## 🔑 Regla de negocio

**No se puede hacer check-in sin CONFIRMAR la reserva.** Implementada en `ReservationService.validateTransition()`.

## 🗄️ Persistencia

**H2 en memoria**. Para ver la consola: `http://localhost:8081/h2-console`
- JDBC URL: `jdbc:h2:mem:reservations`
- User: `sa`
- Password: (vacío)

## 🚀 Comandos

```bash
mvn clean package -DskipTests
java -jar target/ms-andesstay-reservations.jar
```

## 🧪 Pruebas rápidas (con curl)

```bash
# Listar
curl http://localhost:8081/api/reservations

# Crear
curl -X POST http://localhost:8081/api/reservations \
  -H "Content-Type: application/json" \
  -d '{"unitId":1,"guestName":"Juan","checkIn":"2026-10-01","checkOut":"2026-10-05"}'

# Cambiar estado
curl -X PATCH http://localhost:8081/api/reservations/1/status \
  -H "Content-Type: application/json" \
  -d '{"status":"CONFIRMADA"}'
```

## 📁 Estructura

```
src/main/java/cl/andesstay/reservations/
├── ReservationsApplication.java
├── config/
│   ├── CorsConfig.java
│   └── DataLoader.java              # Carga datos de ejemplo
├── controller/
│   └── ReservationController.java
├── dto/
│   └── ReservationDTO.java
├── exception/
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
├── model/
│   └── Reservation.java             # Entidad JPA
├── repository/
│   └── ReservationRepository.java
└── service/
    └── ReservationService.java      # Regla de negocio
```
