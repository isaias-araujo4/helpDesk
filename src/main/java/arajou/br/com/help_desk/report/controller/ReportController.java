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

@RestController
@RequestMapping("reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportExcelExporter reportExcelExporter;
    private final ReportPdfExporter reportPdfExporter;

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_REPORTS')")
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

    @GetMapping("export/excel")
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
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("relatorio.xlsx").build().toString())
                .body(file);
    }

    @GetMapping("export/pdf")
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
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("relatorio.pdf").build().toString())
                .body(file);
    }
}
