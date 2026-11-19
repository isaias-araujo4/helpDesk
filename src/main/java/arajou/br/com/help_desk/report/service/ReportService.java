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

/**
 * Calcula as métricas agregadas do relatório de chamados. Filtra os chamados no banco
 * (data de criação + setor/tema/status opcionais, via {@link Specification}) e calcula as
 * métricas em Java sobre essa lista já filtrada - mais simples de escrever/entender que
 * várias queries JPQL de agregação, e suficiente pra escala desse projeto.
 */
@Service // Registra a classe como bean de camada de serviço, gerenciado pelo Spring
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
public class ReportService {

    private final TicketRepository ticketRepository;

    /**
     * Gera o relatório agregado de chamados dentro do período e filtros informados.
     * {@code startDate}/{@code endDate} filtram pela data de CRIAÇÃO do chamado - o
     * relatório de "novembro" é sobre os chamados ABERTOS em novembro, e todas as
     * métricas (atendidos, cancelados, tempo médio etc.) são calculadas sobre esse mesmo
     * grupo, mantendo os números comparáveis entre si.
     *
     * @param startDate data mínima de criação (opcional - sem filtro se nulo)
     * @param endDate data máxima de criação (opcional - sem filtro se nulo)
     * @param department filtro opcional pelo setor do cliente
     * @param theme filtro opcional pelo tema do chamado
     * @param status filtro opcional pelo status do chamado
     * @return o relatório agregado
     */
    public ReportResponseDTO generate(
            LocalDate startDate,
            LocalDate endDate,
            Department department,
            TicketTheme theme,
            TicketStatus status
    ) {
        // conjunction(): condição neutra "sempre verdadeira", ponto de partida pra ir
        // encadeando os filtros opcionais abaixo com .and(...) sem precisar de if/else aninhado.
        Specification<Ticket> specs = Specification.where((root, query, cb) -> cb.conjunction());

        // atStartOfDay()/atTime(LocalTime.MAX): converte a data (só dia) recebida na
        // requisição no intervalo de datetime correspondente ao dia inteiro (00:00:00 até
        // 23:59:59.999999999), já que createdOn na entidade é LocalDateTime.
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

        // Busca de uma vez só todos os chamados que batem com os filtros (sem paginação -
        // o relatório precisa do conjunto inteiro pra calcular as métricas corretamente).
        List<Ticket> tickets = ticketRepository.findAll(specs);

        long totalCreated = tickets.size();

        // "Atendido" = já passou pelo RESOLVED em algum momento (resolvedAt setado).
        // Um chamado reaberto perde isso até ser resolvido de novo - ver TicketService.reopen.
        long totalResolved = tickets.stream().filter(t -> t.getResolvedAt() != null).count();

        long totalCancelled = tickets.stream().filter(t -> t.getStatus() == TicketStatus.CANCELLED).count();

        // Média de horas entre criação e fechamento, considerando só os chamados já
        // CLOSED (closedAt != null). OptionalDouble.average() vem vazio se não houver
        // nenhum chamado fechado no filtro - nesse caso a métrica fica null, não zero.
        OptionalDouble averageHours = tickets.stream()
                .filter(t -> t.getClosedAt() != null)
                .mapToDouble(t -> Duration.between(t.getCreatedOn(), t.getClosedAt()).toMinutes() / 60.0)
                .average();
        Double averageResolutionTimeInHours = averageHours.isPresent() ? averageHours.getAsDouble() : null;

        // groupingBy + counting: agrupa os chamados por setor/tema e conta quantos caem
        // em cada grupo, equivalente a um "GROUP BY ... COUNT(*)" feito em memória.
        Map<Department, Long> ticketsByDepartment = tickets.stream()
                .collect(Collectors.groupingBy(t -> t.getCustomer().getDepartment(), Collectors.counting()));

        Map<TicketTheme, Long> ticketsByTheme = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getTheme, Collectors.counting()));

        // A partir dos agrupamentos acima, extrai qual chave teve o maior/menor valor.
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
