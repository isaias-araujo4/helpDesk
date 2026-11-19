package arajou.br.com.help_desk.report.export;

import arajou.br.com.help_desk.report.dto.ReportResponseDTO;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Monta um PDF em memória a partir do {@link ReportResponseDTO} já calculado, usando o
 * OpenPDF (fork de código aberto do iText clássico) - mesma lógica do
 * {@link ReportExcelExporter}, só muda o formato de saída.
 */
@Component // Registra a classe como bean gerenciado pelo Spring, injetável no ReportController
public class ReportPdfExporter {

    // Fontes reutilizadas em cada nível de texto do documento (título, seção, rótulo, valor).
    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);

    /**
     * Gera os bytes do arquivo .pdf a partir do relatório: um título, um resumo
     * (totais/médias) e duas tabelas de agrupamento (por setor e por tema).
     *
     * @param report relatório já calculado pelo {@code ReportService}
     * @return o conteúdo binário do arquivo PDF, pronto para ser devolvido como download
     */
    public byte[] export(ReportResponseDTO report) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // Conecta o documento ao stream de saída em memória (em vez de um arquivo em disco).
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Relatório de Chamados", TITLE_FONT));
            document.add(Chunk.NEWLINE);
            document.add(summaryTable(report));

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Chamados por setor", SECTION_FONT));
            document.add(mapTable(report.ticketsByDepartment()));

            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Chamados por tema", SECTION_FONT));
            document.add(mapTable(report.ticketsByTheme()));

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }

        return out.toByteArray();
    }

    // Monta a tabela de duas colunas (rótulo/valor) com os totais e médias do relatório.
    private PdfPTable summaryTable(ReportResponseDTO report) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        addRow(table, "Total de chamados criados", String.valueOf(report.totalCreated()));
        addRow(table, "Total de chamados atendidos", String.valueOf(report.totalResolved()));
        addRow(table, "Total de chamados cancelados", String.valueOf(report.totalCancelled()));
        addRow(table, "Tempo médio de finalização (horas)",
                report.averageResolutionTimeInHours() != null
                        ? String.format("%.1f", report.averageResolutionTimeInHours())
                        : "N/A");
        addRow(table, "Tema mais solicitado", stringOrNA(report.mostRequestedTheme()));
        addRow(table, "Tema menos solicitado", stringOrNA(report.leastRequestedTheme()));
        addRow(table, "Setor com mais chamados", stringOrNA(report.departmentWithMostTickets()));
        addRow(table, "Setor com menos chamados", stringOrNA(report.departmentWithLeastTickets()));

        return table;
    }

    // Monta uma tabela de duas colunas (chave/quantidade) a partir de um mapa de
    // agrupamento (usado tanto para setor quanto para tema).
    private <K> PdfPTable mapTable(Map<K, Long> data) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        for (Map.Entry<K, Long> entry : data.entrySet()) {
            addRow(table, entry.getKey().toString(), String.valueOf(entry.getValue()));
        }

        return table;
    }

    // Adiciona uma linha de duas células à tabela: rótulo em negrito, valor em fonte normal.
    private void addRow(PdfPTable table, String label, String value) {
        table.addCell(new PdfPCell(new Paragraph(label, LABEL_FONT)));
        table.addCell(new PdfPCell(new Paragraph(value, VALUE_FONT)));
    }

    // Converte pra "N/A" os campos que podem vir nulos no relatório.
    private String stringOrNA(Object value) {
        return value != null ? value.toString() : "N/A";
    }
}
