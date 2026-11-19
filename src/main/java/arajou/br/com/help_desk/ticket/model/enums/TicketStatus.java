package arajou.br.com.help_desk.ticket.model.enums;

/**
 * Estados do ciclo de vida de um chamado. A transição entre eles é estrita e validada
 * por {@link arajou.br.com.help_desk.ticket.validator.TicketValidator}:
 * <pre>
 * PENDING --(técnico assume)--&gt; IN_PROGRESS --(técnico resolve)--&gt; RESOLVED --(cliente confirma)--&gt; CLOSED
 *    |                                                                  ^
 *    (cliente cancela)                                          (cliente reabre, volta pra IN_PROGRESS)
 *    v
 * CANCELLED
 * </pre>
 */
public enum TicketStatus {
    PENDING,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED
}
