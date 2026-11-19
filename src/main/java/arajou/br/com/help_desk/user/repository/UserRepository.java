package arajou.br.com.help_desk.user.repository;

import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Repositório de {@link UserModel}. Além do CRUD básico herdado de {@link JpaRepository},
 * expõe {@link JpaSpecificationExecutor} para permitir busca paginada com filtros dinâmicos
 * (ver {@link arajou.br.com.help_desk.user.repository.specs.UserSpecs}).
 */
public interface UserRepository extends JpaRepository<UserModel, Long>, JpaSpecificationExecutor<UserModel> {

    /**
     * Busca um usuário pelo e-mail - usado no login e na verificação de duplicidade.
     *
     * @param email e-mail a buscar
     * @return o usuário, se existir
     */
    Optional<UserModel> findByEmail(String email);

    /**
     * Busca um usuário pela combinação exata de nome + sobrenome + setor - usado para
     * validar a regra de unicidade dessa combinação (ver {@code UserValidator}).
     * Spring Data gera a query automaticamente a partir do nome do método.
     *
     * @param firstName nome
     * @param lastName sobrenome
     * @param department setor
     * @return o usuário, se existir algum com essa combinação exata
     */
    Optional<UserModel>  findByFirstNameAndLastNameAndDepartment(String firstName, String lastName, Department department);

}
