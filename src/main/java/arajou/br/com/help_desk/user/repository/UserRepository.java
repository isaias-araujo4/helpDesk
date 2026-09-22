package arajou.br.com.help_desk.user.repository;

import arajou.br.com.help_desk.user.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserModel, Long> {
}
