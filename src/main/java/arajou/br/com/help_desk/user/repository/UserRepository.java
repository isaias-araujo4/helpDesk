package arajou.br.com.help_desk.user.repository;

import arajou.br.com.help_desk.user.model.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// JpaRepository já dá de graça: save, findById, findAll, delete, etc.
// Só precisamos declarar as consultas específicas que não vêm prontas.
public interface UserRepository extends JpaRepository<UserModel, Long> {

    // Query method: o Spring Data lê o nome do método e gera a query sozinho
    // (equivalente a "select u from UserModel u where u.email = :email").
    // Usado pelo UserValidator (checar e-mail duplicado) e, futuramente, pela Security (login).
    Optional<UserModel> findByEmail(String email);
}
