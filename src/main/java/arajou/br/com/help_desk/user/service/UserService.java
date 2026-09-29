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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final EmailService emailService;
    private final AuditorAware<String> auditorAware;


    public Optional<UserModel> findById(Long id){
        return userRepository.findById(id);
    }

    public UserModel save(UserModel userModel){
        userValidator.validate(userModel);

        String temporaryPassword = temporaryPasswordGenerator.generate();
        userModel.setPassword(passwordEncoder.encode(temporaryPassword));
        userModel.setMustChangePassword(true);

        UserModel saved = userRepository.save(userModel);

        emailService.sendTemporaryPassword(saved.getEmail(), temporaryPassword);

        return saved;
    }

    public void update(UserModel userModel){
        if (userModel.getId() == null){
            throw new IllegalArgumentException("to update, the user must already be saved");
        }
        userValidator.validate(userModel);
        userRepository.save(userModel);
    }


    public void changePassword(UserModel userModel, String newPassword){
        userModel.setPassword(passwordEncoder.encode(newPassword));
        userModel.setMustChangePassword(false);
        userRepository.save(userModel);
    }
}
