package arajou.br.com.help_desk.user.controller;

import arajou.br.com.help_desk.common.GenericController;
import arajou.br.com.help_desk.exception.ResourceNotFoundException;
import arajou.br.com.help_desk.security.Permission;
import arajou.br.com.help_desk.user.mapper.UserMapper;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.dto.UserCreatedDTO;
import arajou.br.com.help_desk.user.model.dto.UserResponseDTO;
import arajou.br.com.help_desk.user.model.dto.UserUpdatedDTO;
import arajou.br.com.help_desk.user.model.enums.Department;
import arajou.br.com.help_desk.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Endpoints de gestão de usuários - todos exigem a permissão {@code MANAGE_USERS}
 * (na prática, só o MANAGER tem essa permissão por padrão).
 */
@RestController // Combina @Controller + @ResponseBody: cada método devolve diretamente o corpo da resposta (JSON), não uma view
@RequestMapping("users") // Prefixo comum de rota para todos os endpoints desta classe
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
public class UserController implements GenericController {

    // Limite máximo de itens por página - evita que um cliente peça pageSize=999999 e
    // force a aplicação a carregar a tabela inteira de uma vez.
    private static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Cadastra um novo usuário. O cliente nunca envia senha - o sistema gera uma
     * temporária e envia por e-mail.
     *
     * @param userCreatedDTO dados do novo usuário
     * @return 201 com o header {@code Location} apontando para o usuário criado
     */
    @PostMapping // Mapeia POST /users para este método
    @PreAuthorize("hasAuthority('MANAGE_USERS')") // Só executa se o usuário autenticado tiver essa permissão; senão, 403
    public ResponseEntity<Void> create(@RequestBody @Valid UserCreatedDTO userCreatedDTO){
        UserModel user = userMapper.toEntity(userCreatedDTO);
        UserModel saved = userService.save(user);
        URI location = generateLocationHeader(saved.getId());
        return ResponseEntity.created(location).build();
    }

    /**
     * Busca um usuário por id.
     *
     * @param id id do usuário
     * @return 200 com os dados do usuário, ou 404 se não existir
     */
    @GetMapping("{id}") // Mapeia GET /users/{id} para este método
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id){
        UserModel user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return ResponseEntity.ok(userMapper.toUserResponseDTO(user));
    }

    /**
     * Atualiza os dados de um usuário já existente (inclusive a role dele).
     *
     * @param id id do usuário a atualizar
     * @param userUpdatedDTO novos dados
     * @return 204 em caso de sucesso, ou 404 se o usuário não existir
     */
    @PutMapping("{id}") // Mapeia PUT /users/{id} para este método
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody @Valid UserUpdatedDTO userUpdatedDTO){
        UserModel userModel = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        userMapper.updateEntityFromDTO(userUpdatedDTO, userModel);
        userService.update(userModel);

        return ResponseEntity.noContent().build();
    }

    /**
     * Desativa um usuário (soft delete - o registro nunca é removido do banco).
     *
     * @param id id do usuário a desativar
     * @return 204 em caso de sucesso, ou 404 se o usuário não existir
     */
    @DeleteMapping("{id}") // Mapeia DELETE /users/{id} para este método
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        UserModel userModel = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        userService.delete(userModel);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reativa um usuário previamente desativado (soft delete revertido) - mesmo registro,
     * mesmo e-mail, histórico preservado.
     *
     * @param id id do usuário a reativar
     * @return 204 em caso de sucesso, 404 se não existir, ou 409 se já estiver ativo
     */
    @PostMapping("{id}/reactivate") // Mapeia POST /users/{id}/reactivate para este método
    @PreAuthorize("hasAuthority('REACTIVATE_USER')")
    public ResponseEntity<Void> reactivate(@PathVariable Long id){
        UserModel userModel = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        userService.reactivate(userModel);
        return ResponseEntity.noContent().build();
    }

    /**
     * Busca paginada de usuários, com filtros opcionais por nome, sobrenome e setor(es).
     *
     * @param firstName filtro opcional por trecho do nome
     * @param lastName filtro opcional por trecho do sobrenome
     * @param departments filtro opcional por um ou mais setores
     * @param page número da página (começando em 0)
     * @param pageSize tamanho da página
     * @return 200 com a página de usuários encontrados
     */
    @GetMapping // Mapeia GET /users para este método
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<Page<UserResponseDTO>> search(
            @RequestParam(value = "firstName", required = false) String firstName,
            @RequestParam(value = "lastName", required = false) String lastName,
            @RequestParam(value = "department", required = false)List<Department> departments,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize){

        Page<UserModel> result = userService.search(
                firstName, lastName, departments, page, Math.min(pageSize, MAX_PAGE_SIZE));
        Page<UserResponseDTO> userResponseDTOPage = result.map(userMapper::toUserResponseDTO);

        return ResponseEntity.ok(userResponseDTOPage);
    }

    /**
     * Concede uma permissão isolada a um usuário, além das que a role dele já dá.
     *
     * @param id id do usuário
     * @param permission permissão a conceder
     * @return 204 em caso de sucesso, ou 404 se o usuário não existir
     */
    @PostMapping("{id}/permissions/{permission}") // Mapeia POST /users/{id}/permissions/{permission}
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<Void> addPermission(@PathVariable Long id, @PathVariable Permission permission){
        UserModel user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        userService.addPermission(user, permission);
        return ResponseEntity.noContent().build();
    }

    /**
     * Remove uma permissão isolada de um usuário.
     *
     * @param id id do usuário
     * @param permission permissão a remover
     * @return 204 em caso de sucesso, ou 404 se o usuário não existir
     */
    @DeleteMapping("{id}/permissions/{permission}") // Mapeia DELETE /users/{id}/permissions/{permission}
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<Void> removePermission(@PathVariable Long id, @PathVariable Permission permission){
        UserModel user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        userService.removePermission(user, permission);
        return ResponseEntity.noContent().build();
    }

}
