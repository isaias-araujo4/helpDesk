package arajou.br.com.help_desk.security;

/**
 * Catálogo fixo de todas as permissões que o sistema reconhece. Diferente das roles
 * (que são configuráveis em banco, ver {@link arajou.br.com.help_desk.role.model.Role}),
 * esse enum é parte do código - adicionar uma permissão nova exige deploy.
 * <p>
 * Cada valor vira um {@code GrantedAuthority} do Spring Security (ver
 * {@link arajou.br.com.help_desk.user.model.UserModel#getAuthorities()}) e é checado
 * nos controllers via {@code @PreAuthorize("hasAuthority('...')")}.
 */
public enum Permission {
    CREATE_TICKET,
    VIEW_OWN_TICKETS,
    REOPEN_TICKET,
    CLOSE_TICKET,
    CANCEL_TICKET,
    VIEW_PENDING_TICKETS,
    ASSUME_TICKET,
    COMMENT_TICKET,
    RESOLVE_TICKET,
    MANAGE_USERS,
    REACTIVATE_USER,
    VIEW_AUDIT_LOGS,
    VIEW_REPORTS
}
