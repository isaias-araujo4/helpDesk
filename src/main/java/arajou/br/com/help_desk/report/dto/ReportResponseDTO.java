package arajou.br.com.help_desk.report.dto;

import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import arajou.br.com.help_desk.user.model.enums.Department;

import java.util.Map;

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
