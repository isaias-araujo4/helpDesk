package arajou.br.com.help_desk.report.export;

import arajou.br.com.help_desk.report.dto.ReportResponseDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

// Monta uma planilha .xlsx em memória a partir do ReportResponseDTO já calculado -
// não recalcula nada, só formata o que o ReportService já produziu.
@Component
public class ReportExcelExporter {

    public byte[] export(ReportResponseDTO report) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Relatório");
            CellStyle boldStyle = boldStyle(workbook);

            int rowIndex = 0;
            rowIndex = writeSummary(sheet, boldStyle, report, rowIndex);
            rowIndex++;
            rowIndex = writeMap(sheet, boldStyle, "Chamados por setor", report.ticketsByDepartment(), rowIndex);
            rowIndex++;
            writeMap(sheet, boldStyle, "Chamados por tema", report.ticketsByTheme(), rowIndex);

            for (int i = 0; i < 2; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate Excel report", e);
        }
    }

    private int writeSummary(Sheet sheet, CellStyle boldStyle, ReportResponseDTO report, int rowIndex) {
        writeRow(sheet, rowIndex++, boldStyle, "Total de chamados criados", String.valueOf(report.totalCreated()));
        writeRow(sheet, rowIndex++, boldStyle, "Total de chamados atendidos", String.valueOf(report.totalResolved()));
        writeRow(sheet, rowIndex++, boldStyle, "Total de chamados cancelados", String.valueOf(report.totalCancelled()));
        writeRow(sheet, rowIndex++, boldStyle, "Tempo médio de finalização (horas)",
                report.averageResolutionTimeInHours() != null
                        ? String.format("%.1f", report.averageResolutionTimeInHours())
                        : "N/A");
        writeRow(sheet, rowIndex++, boldStyle, "Tema mais solicitado", stringOrNA(report.mostRequestedTheme()));
        writeRow(sheet, rowIndex++, boldStyle, "Tema menos solicitado", stringOrNA(report.leastRequestedTheme()));
        writeRow(sheet, rowIndex++, boldStyle, "Setor com mais chamados", stringOrNA(report.departmentWithMostTickets()));
        writeRow(sheet, rowIndex++, boldStyle, "Setor com menos chamados", stringOrNA(report.departmentWithLeastTickets()));
        return rowIndex;
    }

    private <K> int writeMap(Sheet sheet, CellStyle boldStyle, String title, Map<K, Long> data, int rowIndex) {
        Row titleRow = sheet.createRow(rowIndex++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title);
        titleCell.setCellStyle(boldStyle);

        for (Map.Entry<K, Long> entry : data.entrySet()) {
            writeRow(sheet, rowIndex++, null, entry.getKey().toString(), String.valueOf(entry.getValue()));
        }
        return rowIndex;
    }

    private void writeRow(Sheet sheet, int rowIndex, CellStyle labelStyle, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        if (labelStyle != null) {
            labelCell.setCellStyle(labelStyle);
        }
        row.createCell(1).setCellValue(value);
    }

    private String stringOrNA(Object value) {
        return value != null ? value.toString() : "N/A";
    }

    private CellStyle boldStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }
}
