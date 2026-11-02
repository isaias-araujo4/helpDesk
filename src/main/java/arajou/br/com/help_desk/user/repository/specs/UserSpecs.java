package arajou.br.com.help_desk.user.repository.specs;

import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecs {

    public static Specification<UserModel> firstNameLike(String firstName){
        return ((root, query, cb) ->
                cb.like(cb.upper(root.get("firstName")), "%" + firstName.toUpperCase() + "%"));
    }

    public static Specification<UserModel> lastNameLike(String lastName){
        return (root, query, cb) ->
        cb.like(cb.upper(root.get("lastName")), "%" + lastName.toUpperCase() + "%");
    }

    public static Specification<UserModel> departmentEqual(Department department){
        return (root, query, cb) ->
                cb.equal(root.get("department"), department);
    }
}
