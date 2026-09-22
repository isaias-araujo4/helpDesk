package arajou.br.com.help_desk.user.model;

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
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 25, nullable = false)
    private String firstName;

    @Column(length = 25, nullable = false)
    private String lastName;

    @Column(length = 50, nullable = false, unique = true)
    private String email;

    //a senha é BCrypt
    @Column(length = 255, nullable = false)
    private String password;

    //usado para  soft delete
    @Column(nullable = false)
    private Boolean active = true;

    //auditoria
    @CreatedBy 
    private String createdBY;

    @CreatedDate 
    private LocalDate createdOn;

    private  String deletedBy;

    private LocalDate deletedOn;

    @LastModifiedBy 
    private  String updatedBy;

    @LastModifiedDate 
    private LocalDate updatedOn;
}
