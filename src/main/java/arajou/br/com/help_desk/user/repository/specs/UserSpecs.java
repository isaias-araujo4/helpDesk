package arajou.br.com.help_desk.user.repository.specs;

import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Filtros reutilizáveis de busca para {@link UserModel}, combinados dinamicamente em
 * {@code UserService.search(...)} via {@link org.springframework.data.jpa.domain.Specification}
 * (só entram no WHERE da query os filtros que o cliente realmente informou).
 */
public class UserSpecs {

    /**
     * Filtro "contém" (case-insensitive) pelo primeiro nome.
     *
     * @param firstName trecho do nome a buscar
     * @return a Specification correspondente
     */
    public static Specification<UserModel> firstNameLike(String firstName){
        // upper() dos dois lados + "%texto%": busca "contém", ignorando maiúsculas/minúsculas.
        return ((root, query, cb) ->
                cb.like(cb.upper(root.get("firstName")), "%" + firstName.toUpperCase() + "%"));
    }

    /**
     * Filtro "contém" (case-insensitive) pelo sobrenome.
     *
     * @param lastName trecho do sobrenome a buscar
     * @return a Specification correspondente
     */
    public static Specification<UserModel> lastNameLike(String lastName){
        return (root, query, cb) ->
        cb.like(cb.upper(root.get("lastName")), "%" + lastName.toUpperCase() + "%");
    }

    /**
     * Filtro "está em" uma lista de setores - permite selecionar vários setores de uma vez
     * (OR entre eles), em vez de um filtro exato por um único setor.
     *
     * @param departments setores a incluir no resultado
     * @return a Specification correspondente
     */
    public static Specification<UserModel> departmentIn(List<Department> departments){
        return (root, query, cb) ->
                root.get("department").in(departments);
    }
}
