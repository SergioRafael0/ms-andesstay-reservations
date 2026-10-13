package cl.andesstay.reservations.rabbit;

/**
 * Topología RabbitMQ del caso AndesStay (EP2).
 *
 * Constantes centralizadas para que exchanges, colas, DLQ y routing keys
 * estén definidas en UN solo lugar y no se reescriban valores por el código.
 *
 * IMPORTANTE: esta misma definición existe en ms-andesstay-notify y en
 * ms-andesstay-rabbit-admin. Los tres módulos deben declararla idéntica:
 * si un módulo declarara una cola con argumentos diferentes, RabbitMQ
 * rechazaría la redeclaración (PRECONDITION_FAILED).
 */
public final class RabbitMQTopology {

    private RabbitMQTopology() {}

    // ---------------- Exchanges ----------------
    public static final String EXCHANGE_DIRECT = "cmd.direct";
    public static final String EXCHANGE_TOPIC = "cmd.topic";
    public static final String EXCHANGE_DEAD = "cmd.dead.dlx";

    // ---------------- Colas funcionales ----------------
    public static final String QUEUE_EMAIL = "q.cmd.email";
    public static final String QUEUE_HOUSEKEEPING = "q.cmd.housekeeping";
    public static final String QUEUE_VOUCHER = "q.cmd.voucher";

    // ---------------- Dead Letter Queues ----------------
    public static final String DLQ_EMAIL = "q.cmd.email.dlq";
    public static final String DLQ_HOUSEKEEPING = "q.cmd.housekeeping.dlq";
    public static final String DLQ_VOUCHER = "q.cmd.voucher.dlq";

    // ---------------- Routing keys (cmd.direct, coincidencia exacta) ----------------
    public static final String RK_EMAIL_SEND = "email.send";
    public static final String RK_HOUSEKEEPING_TICKET = "housekeeping.ticket";
    public static final String RK_VOUCHER_GEN = "voucher.gen";

    // ---------------- Patrones (cmd.topic) ----------------
    // email.*            → casa con "email.<un-segmento>", p. ej. "email.urgent"
    // housekeeping.#     → casa con "housekeeping.<cero-o-mas-segmentos>"
    // voucher.*          → casa con "voucher.<un-segmento>", p. ej. "voucher.regenerate"
    public static final String TOPIC_PATTERN_EMAIL = "email.*";
    public static final String TOPIC_PATTERN_HOUSEKEEPING = "housekeeping.#";
    public static final String TOPIC_PATTERN_VOUCHER = "voucher.*";

    // Routing keys de demostración del exchange topic (compatibles con los patrones)
    public static final String RK_EMAIL_URGENT = "email.urgent";
    public static final String RK_HOUSEKEEPING_PRIORITY = "housekeeping.priority";
    public static final String RK_VOUCHER_REGENERATE = "voucher.regenerate";

    // ---------------- Routing keys de las DLQ (hacia cmd.dead.dlx) ----------------
    public static final String DLQ_RK_EMAIL = "email.dead";
    public static final String DLQ_RK_HOUSEKEEPING = "housekeeping.dead";
    public static final String DLQ_RK_VOUCHER = "voucher.dead";

    // ---------------- Retención y limpieza ----------------
    // Colas principales: máximo 1000 mensajes y TTL de 7 días.
    public static final int MAX_LENGTH_MAIN = 1000;
    public static final long TTL_MAIN_MS = 604_800_000L;
    // DLQ: máximo 5000 mensajes y TTL de 14 días. SIN dead-letter exchange
    // para evitar bucles infinitos de mensajes muertos.
    public static final int MAX_LENGTH_DLQ = 5000;
    public static final long TTL_DLQ_MS = 1_209_600_000L;

    // Argumentos de las colas principales (DLX hacia cmd.dead.dlx)
    public static final java.util.Map<String, Object> mainQueueArguments(String dlqRoutingKey) {
        java.util.Map<String, Object> args = new java.util.HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_DEAD);
        args.put("x-dead-letter-routing-key", dlqRoutingKey);
        args.put("x-max-length", MAX_LENGTH_MAIN);
        args.put("x-message-ttl", TTL_MAIN_MS);
        return args;
    }

    // Argumentos de las DLQ (sin DLX: evita bucles; solo retención)
    public static final java.util.Map<String, Object> dlqArguments() {
        java.util.Map<String, Object> args = new java.util.HashMap<>();
        args.put("x-max-length", MAX_LENGTH_DLQ);
        args.put("x-message-ttl", TTL_DLQ_MS);
        return args;
    }
}
