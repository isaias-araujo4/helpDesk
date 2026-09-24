package arajou.br.com.help_desk.user.model;

import arajou.br.com.help_desk.user.model.enums.Department;
import arajou.br.com.help_desk.user.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
public class UserModel {

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

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private UserRole role;

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
}
