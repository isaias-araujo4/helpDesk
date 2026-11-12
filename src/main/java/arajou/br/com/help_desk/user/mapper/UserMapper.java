package arajou.br.com.help_desk.user.mapper;

import arajou.br.com.help_desk.role.repository.RoleRepository;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.dto.UserCreatedDTO;
import arajou.br.com.help_desk.user.model.dto.UserResponseDTO;
import arajou.br.com.help_desk.user.model.dto.UserUpdatedDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

// componentModel = "spring": faz o MapStruct gerar a implementação como um @Component,
// para poder ser injetada em outras classes (ex: UserService) via construtor.
// Precisa ser "abstract class" (não interface) porque usa o roleRepository nas
// expressões abaixo - igual o LivroMapper da referência usa AutorRepository.
@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    protected RoleRepository roleRepository;

    // Converte o DTO de cadastro (dados vindos do cliente) na entidade que será persistida.
    // O MapStruct casa os campos automaticamente pelo nome (firstName -> firstName, etc.).
    // "role" é especial: o DTO só manda o roleId, então buscamos a Role de verdade no banco.
    @Mapping(target = "role", expression = "java( dto.roleId() != null ? roleRepository.findById(dto.roleId()).orElse(null) : null )")
    public abstract UserModel toEntity(UserCreatedDTO dto);

    // Atualiza uma entidade já existente com os dados do DTO de atualização (usado no
    // PUT /users/{id}), em vez de copiar campo por campo manualmente no Controller.
    @Mapping(target = "role", expression = "java( dto.roleId() != null ? roleRepository.findById(dto.roleId()).orElse(null) : null )")
    public abstract void updateEntityFromDTO(UserUpdatedDTO dto, @MappingTarget UserModel entity);

    // Converte a entidade salva no banco no DTO de resposta da API.
    // Como UserResponseDTO não tem campo "password", o MapStruct simplesmente o ignora
    // na conversão - a senha nunca é exposta na resposta.
    @Mapping(target = "roleId", expression = "java( user.getRole() != null ? user.getRole().getId() : null )")
    @Mapping(target = "roleName", expression = "java( user.getRole() != null ? user.getRole().getName() : null )")
    public abstract UserResponseDTO toUserResponseDTO(UserModel user);
}
