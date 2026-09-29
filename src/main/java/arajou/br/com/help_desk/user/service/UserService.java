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

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final EmailService emailService;
    private final AuditorAware<String> auditorAware;

    public UserModel save(UserModel userModel){
        userValidator.validate(userModel);

        String temporaryPassword = temporaryPasswordGenerator.generate();
        userModel.setPassword(passwordEncoder.encode(temporaryPassword));
        userModel.setMustChangePassword(true);

        UserModel saved = userRepository.save(userModel);

        emailService.sendTemporaryPassword(saved.getEmail(), temporaryPassword);

        return saved;
    }

    
}
