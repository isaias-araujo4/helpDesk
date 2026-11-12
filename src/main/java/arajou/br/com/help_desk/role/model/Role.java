package arajou.br.com.help_desk.role.model;

import arajou.br.com.help_desk.security.Permission;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Entity
@Table (name = "roles")
@Data
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permissions")
    private Set<Permission> permissions;
}
