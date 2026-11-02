package arajou.br.com.help_desk.user.controller;

import arajou.br.com.help_desk.common.GenericController;
import arajou.br.com.help_desk.user.mapper.UserMapper;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.dto.UserCreatedDTO;
import arajou.br.com.help_desk.user.model.dto.UserResponseDTO;
import arajou.br.com.help_desk.user.model.dto.UserUpdatedDTO;
import arajou.br.com.help_desk.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController implements GenericController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> create(@RequestBody @Valid UserCreatedDTO userCreatedDTO){
        UserModel user = userMapper.toEntity(userCreatedDTO);
        UserModel saved = userService.save(user);
        URI location = generateLocationHeader(saved.getId());
        return ResponseEntity.created(location).build();
    }

    @GetMapping("{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id){
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(userMapper.toUserResponseDTO(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody @Valid UserUpdatedDTO userUpdatedDTO){
        return userService.findById(id)
                .map(userModel -> {
                    userModel.setFirstName(userUpdatedDTO.firstName());
                    userModel.setLastName(userUpdatedDTO.lastName());
                    userModel.setDepartment(userUpdatedDTO.department());
                    userModel.setEmail(userUpdatedDTO.email());
                    userModel.setRole(userUpdatedDTO.role());

                    userService.update(userModel);

                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        return userService.findById(id)
                .map(userModel -> {
                    userService.delete(userModel);
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
