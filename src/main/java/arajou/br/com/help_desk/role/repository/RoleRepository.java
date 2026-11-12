package arajou.br.com.help_desk.role.repository;

import arajou.br.com.help_desk.role.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
