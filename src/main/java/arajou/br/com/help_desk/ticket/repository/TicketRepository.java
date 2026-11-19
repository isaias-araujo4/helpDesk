package arajou.br.com.help_desk.ticket.repository;

import arajou.br.com.help_desk.ticket.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositório de {@link Ticket}. Além do CRUD básico herdado de {@link JpaRepository},
 * expõe {@link JpaSpecificationExecutor} para permitir busca com filtros dinâmicos
 * (ver {@link arajou.br.com.help_desk.ticket.repository.specs.TicketSpecs}), usado tanto
 * na listagem de chamados quanto no cálculo dos relatórios.
 */
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
}
