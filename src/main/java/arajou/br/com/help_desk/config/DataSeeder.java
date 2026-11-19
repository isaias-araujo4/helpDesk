package arajou.br.com.help_desk.config;

import arajou.br.com.help_desk.role.model.Role;
import arajou.br.com.help_desk.role.repository.RoleRepository;
import arajou.br.com.help_desk.security.Permission;
import arajou.br.com.help_desk.security.TemporaryPasswordGenerator;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import arajou.br.com.help_desk.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Popula o banco com os dados mínimos para a aplicação funcionar logo no primeiro start:
 * as 3 roles padrão (CUSTOMER, TECHNICIAN, MANAGER) com suas permissões, e um usuário
 * MANAGER inicial (para poder logar e cadastrar os demais usuários pela API).
 */
@Component // Registra a classe como bean gerenciado pelo Spring
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
@Slf4j // Lombok: gera o campo "log" (logger) para a classe
public class DataSeeder implements CommandLineRunner { // CommandLineRunner: o Spring Boot chama run() automaticamente logo após a aplicação subir

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    /**
     * Executado uma única vez, automaticamente, após o contexto do Spring subir.
     * Não faz nada se já existir algum usuário (evita recriar o seed em todo restart).
     *
     * @param args argumentos de linha de comando repassados pelo Spring Boot (não usados aqui)
     */
    @Override // Sobrescreve o método da interface CommandLineRunner
    public void run(String... args){
        if (userRepository.count() > 0){
            return;
        }

        Role customerRole = createRole("CUSTOMER", Set.of(
                Permission.CREATE_TICKET,
                Permission.VIEW_OWN_TICKETS,
                Permission.REOPEN_TICKET,
                Permission.CLOSE_TICKET,
                Permission.CANCEL_TICKET
        ));

        Set<Permission> technicianPermissions = Set.of(
                Permission.VIEW_PENDING_TICKETS,
                Permission.ASSUME_TICKET,
                Permission.COMMENT_TICKET,
                Permission.RESOLVE_TICKET
        );
        createRole("TECHNICIAN", technicianPermissions);

        Set<Permission> managerPermissions = new HashSet<>(technicianPermissions);
        managerPermissions.add(Permission.MANAGE_USERS);
        managerPermissions.add(Permission.VIEW_AUDIT_LOGS);
        managerPermissions.add(Permission.VIEW_REPORTS);
        Role managerRole = createRole("MANAGER", managerPermissions);

        String temporaryPassword = temporaryPasswordGenerator.generate();

        UserModel admin = new UserModel();
        admin.setFirstName("Admin");
        admin.setLastName("Admin");
        admin.setDepartment(Department.IT);
        admin.setEmail("admin@helpdesk.com");
        admin.setPassword(passwordEncoder.encode(temporaryPassword));
        admin.setMustChangePassword(true);
        admin.setRole(managerRole);
        admin.setActive(true);

        userRepository.save(admin);

        log.info("==============================================");
        log.info("Initial MANAGER user created.");
        log.info("Email: {}", admin.getEmail());
        log.info("Temporary password: {}", temporaryPassword);
        log.info("==============================================");
    }

    /**
     * Cria e persiste uma {@link Role} com o nome e o conjunto de permissões informados.
     *
     * @param name nome da role (ex: "CUSTOMER")
     * @param permissions permissões concedidas a quem tiver essa role
     * @return a role já salva no banco, com id gerado
     */
    private Role createRole(String name, Set<Permission> permissions){
        Role role = new Role();
        role.setName(name);
        role.setDescription(name + " role");
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }
}
