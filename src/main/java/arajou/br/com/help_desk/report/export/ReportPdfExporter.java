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

// Monta um PDF em memória a partir do ReportResponseDTO já calculado, usando o OpenPDF -
// mesma lógica do ReportExcelExporter, só muda o formato de saída.
@Component
public class ReportPdfExporter {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);

    public byte[] export(ReportResponseDTO report) {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
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

    private <K> PdfPTable mapTable(Map<K, Long> data) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);

        for (Map.Entry<K, Long> entry : data.entrySet()) {
            addRow(table, entry.getKey().toString(), String.valueOf(entry.getValue()));
        }

        return table;
    }

    private void addRow(PdfPTable table, String label, String value) {
        table.addCell(new PdfPCell(new Paragraph(label, LABEL_FONT)));
        table.addCell(new PdfPCell(new Paragraph(value, VALUE_FONT)));
    }

    private String stringOrNA(Object value) {
        return value != null ? value.toString() : "N/A";
    }
}
