package arajou.br.com.help_desk.user.model;

import arajou.br.com.help_desk.role.model.Role;
import arajou.br.com.help_desk.security.Permission;
import arajou.br.com.help_desk.user.model.enums.Department;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Entidade central do sistema: representa tanto um registro de usuário (dados pessoais,
 * setor, auditoria, soft delete) quanto, por implementar {@link UserDetails} diretamente,
 * o próprio objeto de autenticação usado pelo Spring Security (ver
 * {@link arajou.br.com.help_desk.security.CustomUserDetailsService}).
 */
@Entity // Marca a classe como uma entidade JPA, mapeada para uma tabela do banco
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"firstName", "lastName", "department"})
)// uniqueConstraints: impede cadastrar dois usuários com a mesma combinação de
// nome + sobrenome + setor (regra de negócio da spec, reforçada aqui no nível do banco).
@Data // Lombok: gera getters, setters, equals/hashCode, toString e um construtor sem argumentos
@AllArgsConstructor // Lombok: gera um construtor com todos os campos
@NoArgsConstructor // Lombok: gera um construtor sem argumentos (exigido pelo JPA para instanciar a entidade)
@EntityListeners(AuditingEntityListener.class) // Habilita o preenchimento automático dos campos @CreatedBy/@CreatedDate/@LastModifiedBy/@LastModifiedDate.
public class UserModel implements UserDetails {

    @Id // Chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Geração delegada ao banco (auto-incremento)
    private Long id;

    @Column(length = 25, nullable = false, name = "first_name")
    private String firstName;

    @Column(length = 25, nullable = false, name = "last_name")
    private String lastName;

    @Enumerated(EnumType.STRING) // Persiste o enum pelo nome (ex: "IT"), não pelo índice numérico - mais legível e seguro contra reordenação do enum
    @Column(length = 30, nullable = false)
    private Department department;

    @Column(length = 50, nullable = false, unique = true)
    private String email;

    // A senha é BCrypt. @JsonIgnore: defesa extra contra vazar o hash caso essa entidade
    // seja serializada por engano em algum response (hoje isso nunca acontece - só vai
    // pro cliente via DTO - mas essa anotação garante que mesmo um erro futuro não vaze
    // a senha). @ToString.Exclude: evita que o hash apareça em logs via toString() do Lombok.
    @JsonIgnore
    @ToString.Exclude
    @Column(length = 255, nullable = false)
    private String password;

    @Column(nullable = false, name = "must_change_password")
    private boolean mustChangePassword = true;

    // Pacote de permissões atribuído pelo GERENTE/ADM - substituiu o antigo enum fixo.
    // Nullable: um usuário pode, em tese, ter só permissões isoladas, sem nenhuma role.
    @ManyToOne // Relacionamento N:1 - vários usuários podem ter a mesma role
    @JoinColumn(name = "role_id") // Nome da coluna de chave estrangeira na tabela "users"
    private Role role;

    // Permissões soltas atribuídas direto a este usuário, por cima do que a role já dá
    // (ex: um TECNICO que também pode ver relatórios, sem precisar de uma role nova pra isso).
    @ElementCollection(fetch = FetchType.EAGER) // EAGER: as permissões já vêm carregadas junto com o usuário, sem precisar de outra consulta
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id")) // Nome da tabela auxiliar e a FK que aponta de volta para "users"
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<Permission> extraPermissions;

    //usado para soft delete
    @Column(nullable = false)
    private Boolean active = true;

    //auditoria
    // Preenchido automaticamente pelo SpringSecurityAuditorAware na criação do registro.
    @CreatedBy // Spring Data Auditing: preenche automaticamente com o retorno de AuditorAware.getCurrentAuditor() ao criar o registro
    @Column(name = "created_by")
    private String createdBy;

    // Preenchido automaticamente com a data/hora da criação do registro.
    @CreatedDate // Spring Data Auditing: preenche automaticamente com a data/hora no momento da criação
    @Column(name = "created_on")
    private LocalDate createdOn;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "deleted_on")
    private LocalDate deletedOn;

    // Preenchido automaticamente pelo SpringSecurityAuditorAware a cada atualização do registro.
    @LastModifiedBy // Spring Data Auditing: preenche automaticamente a cada UPDATE do registro
    @Column(name = "updated_by")
    private String updatedBy;

    // Preenchido automaticamente com a data/hora da última atualização do registro.
    @LastModifiedDate // Spring Data Auditing: preenche automaticamente com a data/hora a cada UPDATE
    @Column(name = "updated_on")
    private LocalDate updatedOn;

    /**
     * Junta, numa lista só de {@link GrantedAuthority}, tudo que define o que este usuário
     * pode fazer: o nome da role (prefixado "ROLE_", pra continuar funcionando com
     * {@code hasRole(...)} se algum {@code @PreAuthorize} ainda usar isso), cada
     * {@link Permission} que a role carrega, e cada permissão isolada do próprio usuário.
     * É essa lista que o Spring Security consulta em {@code @PreAuthorize("hasAuthority('...')")}.
     *
     * @return o conjunto de authorities (role + permissões) deste usuário
     */
    @Override // Sobrescreve o método da interface UserDetails
    public Collection<? extends GrantedAuthority> getAuthorities(){
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (role != null){
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            role.getPermissions().forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission.name())));
        }

        if (extraPermissions != null){
            extraPermissions.forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission.name())));
        }

        return authorities;
    }

    // "Username" do Spring Security, mas neste sistema o login é feito por e-mail.
    @Override
    public String getUsername(){
        return email;
    }

    // Reaproveita o próprio campo "active" (soft delete) - um usuário desativado também
    // fica automaticamente impedido de logar, sem precisar de uma flag separada.
    @Override
    public boolean isEnabled(){
        return active;
    }

    // Sempre true: este projeto não implementa expiração de conta.
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // Sempre true: bloqueio de conta por tentativas de login é tratado à parte, por
    // LoginAttemptService, não por essa flag do Spring Security.
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // Sempre true: este projeto não implementa expiração periódica de senha.
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
