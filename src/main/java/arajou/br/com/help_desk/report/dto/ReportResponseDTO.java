package arajou.br.com.help_desk.report.dto;

import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import arajou.br.com.help_desk.user.model.enums.Department;

import java.util.Map;

/**
 * Resultado agregado do relatório de chamados, calculado por
 * {@code ReportService.generate(...)} sobre o conjunto de chamados filtrado por data/
 * setor/tema/status. Servida tanto como JSON ({@code GET /reports}) quanto formatada em
 * Excel/PDF pelos exportadores.
 *
 * @param totalCreated total de chamados no filtro aplicado
 * @param totalResolved total que já passou por RESOLVED (ver nota no ReportService sobre chamados reabertos)
 * @param totalCancelled total com status CANCELLED
 * @param averageResolutionTimeInHours tempo médio entre criação e fechamento, em horas (null se nenhum chamado foi fechado)
 * @param ticketsByDepartment quantidade de chamados por setor do cliente
 * @param ticketsByTheme quantidade de chamados por tema
 * @param mostRequestedTheme tema com mais chamados (null se não houver nenhum)
 * @param leastRequestedTheme tema com menos chamados (null se não houver nenhum)
 * @param departmentWithMostTickets setor que mais abriu chamados (null se não houver nenhum)
 * @param departmentWithLeastTickets setor que menos abriu chamados (null se não houver nenhum)
 */
public record ReportResponseDTO(
        long totalCreated,
        long totalResolved,
        long totalCancelled,
        Double averageResolutionTimeInHours,
        Map<Department, Long> ticketsByDepartment,
        Map<TicketTheme, Long> ticketsByTheme,
        TicketTheme mostRequestedTheme,
        TicketTheme leastRequestedTheme,
        Department departmentWithMostTickets,
        Department departmentWithLeastTickets
) {
}
