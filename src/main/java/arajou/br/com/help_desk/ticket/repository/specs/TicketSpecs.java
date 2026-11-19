package arajou.br.com.help_desk.ticket.repository.specs;

import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import arajou.br.com.help_desk.user.model.enums.Department;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Filtros reutilizáveis de busca para {@link Ticket}, combinados dinamicamente tanto em
 * {@code TicketService.search(...)} (listagem paginada) quanto em
 * {@code ReportService.generate(...)} (cálculo de métricas), via
 * {@link org.springframework.data.jpa.domain.Specification}.
 */
public class TicketSpecs {

    /**
     * Filtro exato por status.
     *
     * @param status status a buscar
     * @return a Specification correspondente
     */
    public static Specification<Ticket> statusEquals(TicketStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Filtro exato por tema.
     *
     * @param theme tema a buscar
     * @return a Specification correspondente
     */
    public static Specification<Ticket> themeEquals(TicketTheme theme) {
        return (root, query, cb) -> cb.equal(root.get("theme"), theme);
    }

    /**
     * Filtro pelo id do cliente dono do chamado - usado para um cliente ver só os
     * próprios chamados.
     *
     * @param customerId id do cliente
     * @return a Specification correspondente
     */
    public static Specification<Ticket> customerIdEquals(Long customerId) {
        return (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
    }

    /**
     * Usado pro técnico: vê chamados ainda sem dono (PENDING, pra escolher o que assumir)
     * OU os que ele mesmo já assumiu (pra acompanhar o próprio trabalho).
     *
     * @param technicianId id do técnico logado
     * @return a Specification correspondente
     */
    public static Specification<Ticket> pendingOrAssignedTo(Long technicianId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("status"), TicketStatus.PENDING),
                cb.equal(root.get("technician").get("id"), technicianId)
        );
    }

    /**
     * Filtro "criado a partir de" uma data (inclusive) - usado no relatório.
     *
     * @param start data/hora mínima de criação
     * @return a Specification correspondente
     */
    public static Specification<Ticket> createdOnAfterOrEqual(LocalDateTime start){
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdOn"), start);
    }

    /**
     * Filtro "criado até" uma data (inclusive) - usado no relatório.
     *
     * @param end data/hora máxima de criação
     * @return a Specification correspondente
     */
    public static Specification<Ticket> createdOnBeforeOrEqual(LocalDateTime end){
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdOn"), end);
    }

    /**
     * Filtro pelo setor do cliente dono do chamado - usado no relatório (o chamado em si
     * não tem campo de setor, é derivado do cliente).
     *
     * @param department setor a filtrar
     * @return a Specification correspondente
     */
    public static Specification<Ticket> customerDepartmentEquals(Department department){
        return (root, query, cb) -> cb.equal(root.get("customer").get("department"), department);
    }
}
