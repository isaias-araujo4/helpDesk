package arajou.br.com.help_desk.report.controller;

import arajou.br.com.help_desk.report.dto.ReportResponseDTO;
import arajou.br.com.help_desk.report.export.ReportExcelExporter;
import arajou.br.com.help_desk.report.export.ReportPdfExporter;
import arajou.br.com.help_desk.report.service.ReportService;
import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import arajou.br.com.help_desk.user.model.enums.Department;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoints de relatórios e métricas - todos exigem a permissão {@code VIEW_REPORTS}
 * (gerente). Oferece o relatório em JSON e em dois formatos de download (Excel e PDF),
 * todos aceitando os mesmos filtros de data/setor/tema/status.
 */
@RestController // Combina @Controller + @ResponseBody: cada método devolve diretamente o corpo da resposta, não uma view
@RequestMapping("reports") // Prefixo comum de rota para todos os endpoints desta classe
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
public class ReportController {

    private final ReportService reportService;
    private final ReportExcelExporter reportExcelExporter;
    private final ReportPdfExporter reportPdfExporter;

    /**
     * Gera o relatório agregado de chamados em JSON.
     *
     * @param startDate data mínima de criação (opcional)
     * @param endDate data máxima de criação (opcional)
     * @param department filtro opcional pelo setor do cliente
     * @param theme filtro opcional pelo tema do chamado
     * @param status filtro opcional pelo status do chamado
     * @return 200 com o relatório calculado
     */
    @GetMapping // Mapeia GET /reports para este método
    @PreAuthorize("hasAuthority('VIEW_REPORTS')") // Só executa se o usuário autenticado tiver essa permissão; senão, 403
    public ResponseEntity<ReportResponseDTO> generate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) TicketTheme theme,
            @RequestParam(required = false) TicketStatus status
    ) {
        ReportResponseDTO report = reportService.generate(startDate, endDate, department, theme, status);
        return ResponseEntity.ok(report);
    }

    /**
     * Gera o mesmo relatório, formatado como planilha .xlsx para download.
     *
     * @param startDate data mínima de criação (opcional)
     * @param endDate data máxima de criação (opcional)
     * @param department filtro opcional pelo setor do cliente
     * @param theme filtro opcional pelo tema do chamado
     * @param status filtro opcional pelo status do chamado
     * @return 200 com o arquivo .xlsx no corpo, e header Content-Disposition de anexo
     */
    @GetMapping("export/excel") // Mapeia GET /reports/export/excel para este método
    @PreAuthorize("hasAuthority('VIEW_REPORTS')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) TicketTheme theme,
            @RequestParam(required = false) TicketStatus status
    ) {
        ReportResponseDTO report = reportService.generate(startDate, endDate, department, theme, status);
        byte[] file = reportExcelExporter.export(report);

        return ResponseEntity.ok()
                // Media type oficial de arquivos .xlsx (Office Open XML Spreadsheet)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                // Content-Disposition: attachment faz o navegador baixar o arquivo em vez de tentar exibi-lo
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("relatorio.xlsx").build().toString())
                .body(file);
    }

    /**
     * Gera o mesmo relatório, formatado como documento .pdf para download.
     *
     * @param startDate data mínima de criação (opcional)
     * @param endDate data máxima de criação (opcional)
     * @param department filtro opcional pelo setor do cliente
     * @param theme filtro opcional pelo tema do chamado
     * @param status filtro opcional pelo status do chamado
     * @return 200 com o arquivo .pdf no corpo, e header Content-Disposition de anexo
     */
    @GetMapping("export/pdf") // Mapeia GET /reports/export/pdf para este método
    @PreAuthorize("hasAuthority('VIEW_REPORTS')")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) TicketTheme theme,
            @RequestParam(required = false) TicketStatus status
    ) {
        ReportResponseDTO report = reportService.generate(startDate, endDate, department, theme, status);
        byte[] file = reportPdfExporter.export(report);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                // Content-Disposition: attachment faz o navegador baixar o arquivo em vez de tentar exibi-lo
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("relatorio.pdf").build().toString())
                .body(file);
    }
}
