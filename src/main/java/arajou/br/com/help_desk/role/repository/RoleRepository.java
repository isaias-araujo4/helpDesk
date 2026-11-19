package arajou.br.com.help_desk.role.repository;

import arajou.br.com.help_desk.role.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório de {@link Role}. Não define nenhum método próprio - os métodos de CRUD
 * básico (save, findById, findAll, delete, etc.) já vêm prontos herdados de
 * {@link JpaRepository}, o Spring Data gera a implementação automaticamente.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {
}
