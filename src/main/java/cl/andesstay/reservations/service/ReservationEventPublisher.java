package cl.andesstay.reservations.service;

import cl.andesstay.reservations.dto.ReservationEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Productor de mensajes de AndesStay.
 *
 * Publica comandos asíncronos después de que una acción válida del dominio
 * haya sido persistida (p. ej. CONFIRMADA → email de confirmación al huésped).
 *
 * Política ante fallos del broker: la operación de negocio YA fue persistida,
 * por lo que un fallo de publicación NO se propaga al cliente; se registra el
 * error para trazabilidad. (En producción se usaría un outbox transaccional;
 * aquí queda documentada la limitación.)
 */
@Service
public class ReservationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservationEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ReservationEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica un mensaje en el exchange/routing key indicados registrando
     * la información de correlación que exige la evaluación.
     */
    public void publish(ReservationEventMessage message, String exchange, String routingKey) {
        log.info("Publicando mensaje{}Exchange: {}{}Routing Key: {}{}eventId: {}{}correlationId: {}",
                System.lineSeparator(), exchange, System.lineSeparator(), routingKey,
                System.lineSeparator(), message.getEventId(), System.lineSeparator(), message.getCorrelationId());
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("Mensaje publicado OK — eventId: {} exchange: {} routingKey: {}",
                    message.getEventId(), exchange, routingKey);
        } catch (Exception e) {
            log.error("ERROR al publicar mensaje — eventId: {} exchange: {} routingKey: {}: {}",
                    message.getEventId(), exchange, routingKey, e.getMessage(), e);
        }
    }
}
