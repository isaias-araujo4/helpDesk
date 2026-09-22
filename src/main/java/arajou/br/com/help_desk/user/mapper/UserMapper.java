package arajou.br.com.help_desk.user.mapper;

import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.dto.UserCreatedDTO;
import arajou.br.com.help_desk.user.model.dto.UserResponseDTO;
import org.mapstruct.Mapper;

// componentModel = "spring": faz o MapStruct gerar a implementação como um @Component,
// para poder ser injetada em outras classes (ex: UserService) via construtor.
@Mapper(componentModel = "spring")
public interface UserMapper {

    // Converte o DTO de cadastro (dados vindos do cliente) na entidade que será persistida.
    // O MapStruct casa os campos automaticamente pelo nome (firstName -> firstName, etc.).
    UserModel toEntity(UserCreatedDTO userCreatedDTO);

    // Converte a entidade salva no banco no DTO de resposta da API.
    // Como UserResponseDTO não tem campo "password", o MapStruct simplesmente o ignora
    // na conversão - a senha nunca é exposta na resposta.
    UserResponseDTO toUserResponseDTO(UserModel userModel);
}
