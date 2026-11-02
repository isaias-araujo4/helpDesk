package arajou.br.com.help_desk.user.repository.specs;

import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class UserSpecs {

    public static Specification<UserModel> firstNameLike(String firstName){
        return ((root, query, cb) ->
                cb.like(cb.upper(root.get("firstName")), "%" + firstName.toUpperCase() + "%"));
    }

    public static Specification<UserModel> lastNameLike(String lastName){
        return (root, query, cb) ->
        cb.like(cb.upper(root.get("lastName")), "%" + lastName.toUpperCase() + "%");
    }

    public static Specification<UserModel> departmentIn(List<Department> departments){
        return (root, query, cb) ->
                root.get("department").in(departments);
    }
}
