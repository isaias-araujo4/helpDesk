package arajou.br.com.help_desk.user.model;

import arajou.br.com.help_desk.role.model.Role;
import arajou.br.com.help_desk.security.Permission;
import arajou.br.com.help_desk.user.model.enums.Department;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"firstName", "lastName", "department"})
)// uniqueConstraints: impede cadastrar dois usuários com a mesma combinação de
// nome + sobrenome + setor (regra de negócio da spec, reforçada aqui no nível do banco).
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class) // Habilita o preenchimento automático dos campos @CreatedBy/@CreatedDate/@LastModifiedBy/@LastModifiedDate.
public class UserModel implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 25, nullable = false, name = "first_name")
    private String firstName;

    @Column(length = 25, nullable = false, name = "last_name")
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private Department department;

    @Column(length = 50, nullable = false, unique = true)
    private String email;

    //a senha é BCrypt
    @Column(length = 255, nullable = false)
    private String password;

    @Column(nullable = false, name = "must_change_password")
    private boolean mustChangePassword = true;

    // Pacote de permissões atribuído pelo GERENTE/ADM - substituiu o antigo enum fixo.
    // Nullable: um usuário pode, em tese, ter só permissões isoladas, sem nenhuma role.
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    // Permissões soltas atribuídas direto a este usuário, por cima do que a role já dá
    // (ex: um TECNICO que também pode ver relatórios, sem precisar de uma role nova pra isso).
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<Permission> extraPermissions;

    //usado para soft delete
    @Column(nullable = false)
    private Boolean active = true;

    //auditoria
    // Preenchido automaticamente pelo SpringSecurityAuditorAware na criação do registro.
    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    // Preenchido automaticamente com a data/hora da criação do registro.
    @CreatedDate
    @Column(name = "created_on")
    private LocalDate createdOn;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "deleted_on")
    private LocalDate deletedOn;

    // Preenchido automaticamente pelo SpringSecurityAuditorAware a cada atualização do registro.
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    // Preenchido automaticamente com a data/hora da última atualização do registro.
    @LastModifiedDate
    @Column(name = "updated_on")
    private LocalDate updatedOn;

    // Junta, numa lista só de GrantedAuthority: o nome da role (prefixado "ROLE_", pra
    // continuar funcionando com hasRole(...) se algum @PreAuthorize ainda usar isso),
    // cada Permission que a role carrega, e cada Permission isolada do próprio usuário.
    @Override
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

    @Override
    public String getUsername(){
        return email;
    }

    @Override
    public boolean isEnabled(){
        return active;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
