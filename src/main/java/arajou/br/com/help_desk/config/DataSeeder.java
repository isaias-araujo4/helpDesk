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

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;

    @Override
    public void run(String... args){
        if (userRepository.count() > 0){
            return;
        }

        Role customerRole = createRole("CUSTOMER", Set.of(
                Permission.CREATE_TICKET,
                Permission.VIEW_OWN_TICKETS,
                Permission.REOPEN_TICKET,
                Permission.CLOSE_TICKET,
                Permission.CANCEL_TICKET,
                Permission.CREATE_COMPLAINT
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

    private Role createRole(String name, Set<Permission> permissions){
        Role role = new Role();
        role.setName(name);
        role.setDescription(name + " role");
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }
}
