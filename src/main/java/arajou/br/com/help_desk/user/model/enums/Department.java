package arajou.br.com.help_desk.user.model.enums;

/**
 * Setores da empresa aos quais um usuário pode pertencer. Também usado para agrupar
 * chamados nas métricas de relatório (ver {@link arajou.br.com.help_desk.report.service.ReportService}).
 */
public enum Department {
    HR,
    MARKETING,
    IT,
    PURCHASING
}
