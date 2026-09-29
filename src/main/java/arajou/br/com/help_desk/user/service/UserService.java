package arajou.br.com.help_desk.user.service;


import arajou.br.com.help_desk.email.EmailService;
import arajou.br.com.help_desk.security.TemporaryPasswordGenerator;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.repository.UserRepository;
import arajou.br.com.help_desk.user.validator.UserValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

// Orquestra as operações de usuário: chama o Validator antes de persistir, e concentra
// as regras de senha temporária, criptografia e soft delete. O Controller só chama esses
// métodos - quem decide "pode ou não pode" é o Validator, quem executa é este Service.
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final EmailService emailService;
    // Mesmo bean SpringSecurityAuditorAware, injetado aqui pelo tipo da interface -
    // usado manualmente no soft delete (deletedBy não é automático como createdBy/updatedBy).
    private final AuditorAware<String> auditorAware;


    //método para buscar usuário por id
    public Optional<UserModel> findById(Long id){
        return userRepository.findById(id);
    }

    // Cadastro de um novo usuário (feito pelo MANAGER). O cliente nunca envia senha:
    // o sistema gera uma temporária, salva já criptografada e manda por e-mail em texto puro
    // (só nesse momento ela existe em texto puro, antes de virar hash).
    public UserModel save(UserModel userModel){
        userValidator.validate(userModel);

        String temporaryPassword = temporaryPasswordGenerator.generate();
        userModel.setPassword(passwordEncoder.encode(temporaryPassword));
        // Força a troca de senha no próximo login - ver UserModel.mustChangePassword.
        userModel.setMustChangePassword(true);

        UserModel saved = userRepository.save(userModel);

        // Enviada depois do save: só faz sentido notificar o usuário se o cadastro
        // realmente foi persistido com sucesso.
        emailService.sendTemporaryPassword(saved.getEmail(), temporaryPassword);

        return saved;
    }

    // Atualização de dados de um usuário já existente (feita pelo MANAGER).
    public void update(UserModel userModel){
        if (userModel.getId() == null){
            throw new IllegalArgumentException("to update, the user must already be saved");
        }
        userValidator.validate(userModel);
        userRepository.save(userModel);
    }

    // Troca a senha temporária (ou qualquer senha atual) por uma definitiva escolhida
    public void changePassword(UserModel userModel, String newPassword){
        userModel.setPassword(passwordEncoder.encode(newPassword));
        userModel.setMustChangePassword(false);
        userRepository.save(userModel);
    }

    // Soft delete: nunca remove a linha do banco, só marca como inativo e registra
    // quem/quando desativou. deletedBy/deletedOn não são automáticos (diferente de
    // createdBy/updatedBy), por isso são preenchidos manualmente aqui.
    public void delete(UserModel userModel){
        userModel.setActive(false);
        userModel.setDeletedBy(auditorAware.getCurrentAuditor().orElse("system"));
        userModel.setDeletedOn(LocalDate.now());
        userRepository.save(userModel);
    }
}
