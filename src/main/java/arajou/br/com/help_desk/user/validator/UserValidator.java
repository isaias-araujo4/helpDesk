package arajou.br.com.help_desk.user.validator;

import arajou.br.com.help_desk.exception.DuplicatedRecordException;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Concentra as regras de negócio de validação do User, separadas do Service
// (o Service só orquestra; quem decide se pode salvar ou não é o Validator).
@Component
@RequiredArgsConstructor
public class UserValidator {

    private final UserRepository userRepository;

    // Ponto de entrada chamado pelo Service antes de salvar/atualizar um usuário.
    public void validate(UserModel userModel){
        if (emailAlreadyExists(userModel)){
            throw new DuplicatedRecordException("E-mail já cadastrado!");
        }

        if (nameAndDepartmentAlreadyExists(userModel)){
            throw new DuplicatedRecordException("Já existe um usuário com esse nome, sobrenome e setor!");
        }
    }

    // Regra: não pode existir outro usuário com o mesmo e-mail.
    private boolean emailAlreadyExists(UserModel userModel){
        Optional<UserModel> found = userRepository.findByEmail(userModel.getEmail());

        // userModel.getId() == null -> é um cadastro novo.
        // Se já existe alguém com esse e-mail, é duplicado.
        if (userModel.getId() == null){
            return found.isPresent();
        }

        // Caso contrário, é uma atualização: só é duplicado se o e-mail encontrado
        // pertencer a outro usuário (id diferente do que está sendo atualizado) -
        // senão nunca seria possível salvar o próprio usuário sem trocar o e-mail.
        return found.map(UserModel::getId).stream().anyMatch(id -> !id.equals(userModel.getId()));
    }

    // Regra: não pode existir outro usuário com a mesma combinação de nome + sobrenome + setor
    // (regra explícita da especificação do projeto, reforçada também via unique constraint no banco).
    private boolean nameAndDepartmentAlreadyExists(UserModel userModel){
        Optional<UserModel> found = userRepository.findByFirstNameAndLastNameAndDepartment(
                userModel.getFirstName(), userModel.getLastName(), userModel.getDepartment());

        // Mesma lógica de create vs update do emailAlreadyExists: no cadastro, qualquer
        // resultado já é duplicidade; na atualização, só conta se pertencer a outro usuário.
        if (userModel.getId() == null){
            return found.isPresent();
        }

        return found.map(UserModel::getId).stream().anyMatch(id -> !id.equals(userModel.getId()));
    }
}
