package arajou.br.com.help_desk.user.validator;

import arajou.br.com.help_desk.exception.DuplicatedRecordException;
import arajou.br.com.help_desk.exception.OperationNotAllowedException;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import arajou.br.com.help_desk.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do {@link UserValidator}, com o {@link UserRepository} mockado (sem
 * banco de verdade - a lógica de "é duplicado ou não" é testada isoladamente).
 */
@ExtendWith(MockitoExtension.class)
class UserValidatorTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserValidator userValidator;

    private UserModel newUser(String email) {
        UserModel user = new UserModel();
        user.setFirstName("Carlos");
        user.setLastName("Souza");
        user.setDepartment(Department.IT);
        user.setEmail(email);
        return user;
    }

    @Test
    void validate_newUser_withoutConflicts_doesNotThrow() {
        UserModel user = newUser("carlos@helpdesk.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByFirstNameAndLastNameAndDepartment(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> userValidator.validate(user));
    }

    @Test
    void validate_newUser_withDuplicateEmail_throws() {
        UserModel user = newUser("carlos@helpdesk.com");
        UserModel existing = newUser("carlos@helpdesk.com");
        existing.setId(99L);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(existing));

        assertThrows(DuplicatedRecordException.class, () -> userValidator.validate(user));
    }

    @Test
    void validate_newUser_withDuplicateNameAndDepartment_throws() {
        UserModel user = newUser("carlos.novo@helpdesk.com");
        UserModel existing = newUser("carlos.antigo@helpdesk.com");
        existing.setId(99L);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByFirstNameAndLastNameAndDepartment(anyString(), anyString(), any()))
                .thenReturn(Optional.of(existing));

        assertThrows(DuplicatedRecordException.class, () -> userValidator.validate(user));
    }

    @Test
    void validate_updatingUser_keepingOwnEmail_doesNotThrow() {
        UserModel user = newUser("carlos@helpdesk.com");
        user.setId(1L);
        // O e-mail encontrado pertence ao PRÓPRIO usuário sendo atualizado (mesmo id) -
        // não é duplicidade, é só ele mantendo o e-mail que já tinha.
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(userRepository.findByFirstNameAndLastNameAndDepartment(anyString(), anyString(), any()))
                .thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> userValidator.validate(user));
    }

    @Test
    void validate_updatingUser_withEmailBelongingToAnother_throws() {
        UserModel user = newUser("carlos@helpdesk.com");
        user.setId(1L);
        UserModel anotherUser = newUser("carlos@helpdesk.com");
        anotherUser.setId(2L);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(anotherUser));

        assertThrows(DuplicatedRecordException.class, () -> userValidator.validate(user));
    }

    @Test
    void validateReactivate_inactiveUser_doesNotThrow() {
        UserModel user = newUser("carlos@helpdesk.com");
        user.setActive(false);

        assertDoesNotThrow(() -> userValidator.validateReactivate(user));
    }

    @Test
    void validateReactivate_activeUser_throws() {
        UserModel user = newUser("carlos@helpdesk.com");
        user.setActive(true);

        assertThrows(OperationNotAllowedException.class, () -> userValidator.validateReactivate(user));
    }
}
