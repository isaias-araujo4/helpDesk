package arajou.br.com.help_desk.report.service;

import arajou.br.com.help_desk.report.dto.ReportResponseDTO;
import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import arajou.br.com.help_desk.ticket.repository.TicketRepository;
import arajou.br.com.help_desk.ticket.repository.specs.TicketSpecs;
import arajou.br.com.help_desk.user.model.enums.Department;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TicketRepository ticketRepository;

    public ReportResponseDTO generate(
            LocalDate startDate,
            LocalDate endDate,
            Department department,
            TicketTheme theme,
            TicketStatus status
    ) {
        Specification<Ticket> specs = Specification.where((root, query, cb) -> cb.conjunction());

        if (startDate != null) {
            specs = specs.and(TicketSpecs.createdOnAfterOrEqual(startDate.atStartOfDay()));
        }
        if (endDate != null) {
            specs = specs.and(TicketSpecs.createdOnBeforeOrEqual(endDate.atTime(LocalTime.MAX)));
        }
        if (department != null) {
            specs = specs.and(TicketSpecs.customerDepartmentEquals(department));
        }
        if (theme != null) {
            specs = specs.and(TicketSpecs.themeEquals(theme));
        }
        if (status != null) {
            specs = specs.and(TicketSpecs.statusEquals(status));
        }

        List<Ticket> tickets = ticketRepository.findAll(specs);

        long totalCreated = tickets.size();

        // "Atendido" = já passou pelo RESOLVED em algum momento (resolvedAt setado).
        // Um chamado reaberto perde isso até ser resolvido de novo - ver TicketService.reopen.
        long totalResolved = tickets.stream().filter(t -> t.getResolvedAt() != null).count();

        long totalCancelled = tickets.stream().filter(t -> t.getStatus() == TicketStatus.CANCELLED).count();

        OptionalDouble averageHours = tickets.stream()
                .filter(t -> t.getClosedAt() != null)
                .mapToDouble(t -> Duration.between(t.getCreatedOn(), t.getClosedAt()).toMinutes() / 60.0)
                .average();
        Double averageResolutionTimeInHours = averageHours.isPresent() ? averageHours.getAsDouble() : null;

        Map<Department, Long> ticketsByDepartment = tickets.stream()
                .collect(Collectors.groupingBy(t -> t.getCustomer().getDepartment(), Collectors.counting()));

        Map<TicketTheme, Long> ticketsByTheme = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getTheme, Collectors.counting()));

        TicketTheme mostRequestedTheme = ticketsByTheme.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        TicketTheme leastRequestedTheme = ticketsByTheme.entrySet().stream()
                .min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        Department departmentWithMostTickets = ticketsByDepartment.entrySet().stream()
                .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        Department departmentWithLeastTickets = ticketsByDepartment.entrySet().stream()
                .min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);

        return new ReportResponseDTO(
                totalCreated, totalResolved, totalCancelled, averageResolutionTimeInHours,
                ticketsByDepartment, ticketsByTheme, mostRequestedTheme, leastRequestedTheme,
                departmentWithMostTickets, departmentWithLeastTickets);
    }
}
