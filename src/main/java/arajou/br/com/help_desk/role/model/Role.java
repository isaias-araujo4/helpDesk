package arajou.br.com.help_desk.role.model;

import arajou.br.com.help_desk.security.Permission;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

/**
 * Entidade de banco que representa um "pacote" configurável de permissões (ex: CUSTOMER,
 * TECHNICIAN, MANAGER), atribuído a um ou mais usuários. Diferente de {@link Permission}
 * (que é um enum fixo no código), roles são dados - podem ser consultadas/geridas sem
 * precisar alterar o código, embora neste projeto só sejam criadas pelo
 * {@link arajou.br.com.help_desk.config.DataSeeder}.
 */
@Entity // Marca a classe como uma entidade JPA, mapeada para uma tabela do banco
@Table (name = "roles") // Define o nome da tabela
@Data // Lombok: gera getters, setters, equals/hashCode, toString e um construtor sem argumentos
public class Role {

    @Id // Chave primária
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Geração delegada ao banco (auto-incremento)
    private Long id;

    @Column(nullable = false, unique = true) // Não permite duas roles com o mesmo nome
    private String name;

    private String description;

    // Mapeia o Set<Permission> para uma tabela auxiliar separada ("role_permissions"),
    // já que não é um relacionamento com outra entidade, só uma coleção de valores simples.
    @ElementCollection(fetch = FetchType.EAGER) // EAGER: as permissões já vêm carregadas junto com a role, sem precisar de outra consulta
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id")) // Nome da tabela auxiliar e a FK que aponta de volta para "roles"
    @Enumerated(EnumType.STRING) // Persiste o enum pelo nome (ex: "CREATE_TICKET"), não pelo índice numérico - mais legível e seguro contra reordenação do enum
    @Column(name = "permissions")
    private Set<Permission> permissions;
}
