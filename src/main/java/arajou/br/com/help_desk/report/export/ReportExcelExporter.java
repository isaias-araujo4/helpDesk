package arajou.br.com.help_desk.report.export;

import arajou.br.com.help_desk.report.dto.ReportResponseDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Monta uma planilha .xlsx em memória a partir do {@link ReportResponseDTO} já calculado -
 * não recalcula nada, só formata o que o {@code ReportService} já produziu. Usa o Apache
 * POI, biblioteca padrão de mercado em Java para ler/escrever arquivos Excel.
 */
@Component // Registra a classe como bean gerenciado pelo Spring, injetável no ReportController
public class ReportExcelExporter {

    /**
     * Gera os bytes do arquivo .xlsx a partir do relatório: um resumo (totais/médias) e
     * duas tabelas de agrupamento (por setor e por tema).
     *
     * @param report relatório já calculado pelo {@code ReportService}
     * @return o conteúdo binário do arquivo Excel, pronto para ser devolvido como download
     */
    public byte[] export(ReportResponseDTO report) {
        // try-with-resources: garante que o Workbook libera seus recursos internos mesmo
        // se algo der errado no meio da montagem da planilha.
        try (Workbook workbook = new XSSFWorkbook()) { // XSSFWorkbook: implementação para o formato .xlsx (Office Open XML)
            Sheet sheet = workbook.createSheet("Relatório");
            CellStyle boldStyle = boldStyle(workbook);

            int rowIndex = 0;
            rowIndex = writeSummary(sheet, boldStyle, report, rowIndex);
            rowIndex++; // Linha em branco entre as seções
            rowIndex = writeMap(sheet, boldStyle, "Chamados por setor", report.ticketsByDepartment(), rowIndex);
            rowIndex++;
            writeMap(sheet, boldStyle, "Chamados por tema", report.ticketsByTheme(), rowIndex);

            // Ajusta a largura das duas colunas (rótulo/valor) ao conteúdo mais longo de cada uma.
            for (int i = 0; i < 2; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out); // Serializa a planilha montada em memória para bytes
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate Excel report", e);
        }
    }

    // Escreve a seção de totais/médias, uma linha "rótulo | valor" por métrica.
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

    // Escreve um título em negrito seguido de uma linha "chave | quantidade" para cada
    // entrada do mapa (usado tanto para o agrupamento por setor quanto por tema).
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

    // Escreve uma linha de duas colunas (rótulo na primeira, valor na segunda).
    private void writeRow(Sheet sheet, int rowIndex, CellStyle labelStyle, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        if (labelStyle != null) {
            labelCell.setCellStyle(labelStyle);
        }
        row.createCell(1).setCellValue(value);
    }

    // Converte pra "N/A" os campos que podem vir nulos no relatório (ex: nenhum chamado
    // fechado ainda, ou nenhum chamado de um determinado tema).
    private String stringOrNA(Object value) {
        return value != null ? value.toString() : "N/A";
    }

    // Cria um estilo de célula com fonte em negrito, reaproveitado nos rótulos/títulos.
    private CellStyle boldStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }
}
