package arajou.br.com.help_desk.user.repository;

import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserModel, Long>, JpaSpecificationExecutor<UserModel> {


    Optional<UserModel> findByEmail(String email);

    Optional<UserModel>  findByFirstNameAndLastNameAndDepartment(String firstName, String lastName, Department department);


}
