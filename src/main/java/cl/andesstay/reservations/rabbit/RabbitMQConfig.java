package cl.andesstay.reservations.rabbit;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static cl.andesstay.reservations.rabbit.RabbitMQTopology.*;

/**
 * Declaración de la topología RabbitMQ de AndesStay en el productor.
 *
 * Spring AMQP (a través de RabbitAdmin, auto-configurado por Spring Boot)
 * declara estos recursos en el clúster al abrir la primera conexión.
 * Si los recursos ya existen con los mismos argumentos, no hace nada.
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Conversor JSON para los mensajes publicados: el payload (ReservationEventMessage)
     * se serializa como JSON, que es el contrato documentado entre productor y consumidor.
     */
    @Bean
    public org.springframework.amqp.support.converter.MessageConverter jacksonMessageConverter() {
        return new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter();
    }

    // ---------------- Exchanges ----------------

    @Bean
    public DirectExchange cmdDirectExchange() {
        return new DirectExchange(EXCHANGE_DIRECT, true, false);
    }

    @Bean
    public TopicExchange cmdTopicExchange() {
        return new TopicExchange(EXCHANGE_TOPIC, true, false);
    }

    @Bean
    public DirectExchange cmdDeadLetterExchange() {
        return new DirectExchange(EXCHANGE_DEAD, true, false);
    }

    // ---------------- Colas funcionales (con DLX + retención) ----------------

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(QUEUE_EMAIL)
                .withArguments(mainQueueArguments(DLQ_RK_EMAIL))
                .build();
    }

    @Bean
    public Queue housekeepingQueue() {
        return QueueBuilder.durable(QUEUE_HOUSEKEEPING)
                .withArguments(mainQueueArguments(DLQ_RK_HOUSEKEEPING))
                .build();
    }

    @Bean
    public Queue voucherQueue() {
        return QueueBuilder.durable(QUEUE_VOUCHER)
                .withArguments(mainQueueArguments(DLQ_RK_VOUCHER))
                .build();
    }

    // ---------------- DLQ (retención sin DLX: sin bucles) ----------------

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(DLQ_EMAIL).withArguments(dlqArguments()).build();
    }

    @Bean
    public Queue housekeepingDlq() {
        return QueueBuilder.durable(DLQ_HOUSEKEEPING).withArguments(dlqArguments()).build();
    }

    @Bean
    public Queue voucherDlq() {
        return QueueBuilder.durable(DLQ_VOUCHER).withArguments(dlqArguments()).build();
    }

    // ---------------- Bindings en cmd.direct (coincidencia exacta) ----------------

    @Bean
    public Binding emailDirectBinding() {
        return BindingBuilder.bind(emailQueue()).to(cmdDirectExchange()).with(RK_EMAIL_SEND);
    }

    @Bean
    public Binding housekeepingDirectBinding() {
        return BindingBuilder.bind(housekeepingQueue()).to(cmdDirectExchange()).with(RK_HOUSEKEEPING_TICKET);
    }

    @Bean
    public Binding voucherDirectBinding() {
        return BindingBuilder.bind(voucherQueue()).to(cmdDirectExchange()).with(RK_VOUCHER_GEN);
    }

    // ---------------- Bindings en cmd.topic (por patrón) ----------------

    @Bean
    public Binding emailTopicBinding() {
        return BindingBuilder.bind(emailQueue()).to(cmdTopicExchange()).with(TOPIC_PATTERN_EMAIL);
    }

    @Bean
    public Binding housekeepingTopicBinding() {
        return BindingBuilder.bind(housekeepingQueue()).to(cmdTopicExchange()).with(TOPIC_PATTERN_HOUSEKEEPING);
    }

    @Bean
    public Binding voucherTopicBinding() {
        return BindingBuilder.bind(voucherQueue()).to(cmdTopicExchange()).with(TOPIC_PATTERN_VOUCHER);
    }

    // ---------------- Bindings de las DLQ en cmd.dead.dlx ----------------

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDlq()).to(cmdDeadLetterExchange()).with(DLQ_RK_EMAIL);
    }

    @Bean
    public Binding housekeepingDlqBinding() {
        return BindingBuilder.bind(housekeepingDlq()).to(cmdDeadLetterExchange()).with(DLQ_RK_HOUSEKEEPING);
    }

    @Bean
    public Binding voucherDlqBinding() {
        return BindingBuilder.bind(voucherDlq()).to(cmdDeadLetterExchange()).with(DLQ_RK_VOUCHER);
    }
}
