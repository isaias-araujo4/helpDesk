package arajou.br.com.help_desk.user.service;


import arajou.br.com.help_desk.auditlog.model.enums.AuditAction;
import arajou.br.com.help_desk.auditlog.model.enums.AuditEntityType;
import arajou.br.com.help_desk.auditlog.service.AuditLogService;
import arajou.br.com.help_desk.email.EmailService;
import arajou.br.com.help_desk.security.Permission;
import arajou.br.com.help_desk.security.TemporaryPasswordGenerator;
import arajou.br.com.help_desk.user.model.UserModel;
import arajou.br.com.help_desk.user.model.enums.Department;
import arajou.br.com.help_desk.user.repository.UserRepository;
import arajou.br.com.help_desk.user.validator.UserValidator;
import arajou.br.com.help_desk.user.repository.specs.UserSpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Orquestra as operações de usuário: chama o {@link UserValidator} antes de persistir, e
 * concentra as regras de senha temporária, criptografia e soft delete. O Controller só
 * chama esses métodos - quem decide "pode ou não pode" é o Validator, quem executa é este Service.
 */
@Service // Registra a classe como bean de camada de serviço, gerenciado pelo Spring
@RequiredArgsConstructor // Lombok: gera um construtor com todos os campos "final" (injeção via construtor)
@Slf4j // Lombok: gera o campo "log" (logger) para a classe
public class UserService {

    private final UserRepository userRepository;
    private final UserValidator userValidator;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final EmailService emailService;
    // Mesmo bean SpringSecurityAuditorAware, injetado aqui pelo tipo da interface -
    // usado manualmente no soft delete (deletedBy não é automático como createdBy/updatedBy).
    private final AuditorAware<String> auditorAware;
    private final AuditLogService auditLogService;

    /**
     * Busca um usuário pelo id.
     *
     * @param id id do usuário
     * @return o usuário, se existir
     */
    public Optional<UserModel> findById(Long id){
        return userRepository.findById(id);
    }

    /**
     * Cadastro de um novo usuário (feito pelo MANAGER). O cliente nunca envia senha:
     * o sistema gera uma temporária, salva já criptografada e manda por e-mail em texto puro
     * (só nesse momento ela existe em texto puro, antes de virar hash).
     *
     * @param userModel usuário a ser criado (ainda sem id/senha)
     * @return o usuário já salvo, com id gerado
     */
    public UserModel save(UserModel userModel){
        userValidator.validate(userModel);

        String temporaryPassword = temporaryPasswordGenerator.generate();
        userModel.setPassword(passwordEncoder.encode(temporaryPassword));
        // Força a troca de senha no próximo login - ver UserModel.mustChangePassword.
        userModel.setMustChangePassword(true);

        UserModel saved = userRepository.save(userModel);

        // Enviada depois do save: só faz sentido notificar o usuário se o cadastro
        // realmente foi persistido com sucesso. Falha no envio (ex: limite do provedor de
        // e-mail) NÃO pode derrubar o cadastro - o usuário já existe no banco nesse ponto.
        // Loga a senha temporária como rede de segurança: sem isso, uma falha de e-mail faria
        // a senha se perder pra sempre (ela só existe em texto puro aqui, em memória).
        try {
            emailService.sendTemporaryPassword(saved.getEmail(), temporaryPassword);
        } catch (RuntimeException e) {
            log.error("Failed to send temporary password email to {}. Temporary password: {}",
                    saved.getEmail(), temporaryPassword, e);
        }

        auditLogService.record(AuditAction.USER_CREATED, AuditEntityType.USER, saved.getId(), null);
        return saved;
    }

    /**
     * Atualização de dados de um usuário já existente (feita pelo MANAGER).
     *
     * @param userModel usuário já carregado do banco, com os novos dados já aplicados
     * @throws IllegalArgumentException se o usuário ainda não tiver sido salvo (sem id)
     */
    public void update(UserModel userModel){
        if (userModel.getId() == null){
            throw new IllegalArgumentException("to update, the user must already be saved");
        }
        userValidator.validate(userModel);
        userRepository.save(userModel);
        auditLogService.record(AuditAction.USER_UPDATED, AuditEntityType.USER, userModel.getId(), null);
    }

    /**
     * Troca a senha temporária (ou qualquer senha atual) por uma definitiva escolhida
     * pelo próprio usuário, e desliga a flag de troca obrigatória.
     *
     * @param userModel usuário autenticado trocando a própria senha
     * @param newPassword nova senha, em texto puro (será criptografada aqui)
     */
    public void changePassword(UserModel userModel, String newPassword){
        userModel.setPassword(passwordEncoder.encode(newPassword));
        userModel.setMustChangePassword(false);
        userRepository.save(userModel);
    }

    /**
     * Soft delete: nunca remove a linha do banco, só marca como inativo e registra
     * quem/quando desativou. deletedBy/deletedOn não são automáticos (diferente de
     * createdBy/updatedBy), por isso são preenchidos manualmente aqui.
     *
     * @param userModel usuário a ser desativado
     */
    public void delete(UserModel userModel){
        userModel.setActive(false);
        userModel.setDeletedBy(auditorAware.getCurrentAuditor().orElse("system"));
        userModel.setDeletedOn(LocalDate.now());
        userRepository.save(userModel);
        auditLogService.record(AuditAction.USER_DELETED, AuditEntityType.USER, userModel.getId(), null);
    }

    /**
     * Busca paginada de usuários, com filtros opcionais e independentes entre si (só
     * entram no WHERE os que forem informados).
     *
     * @param firstName filtro opcional por trecho do nome
     * @param lastName filtro opcional por trecho do sobrenome
     * @param departments filtro opcional por uma lista de setores
     * @param page número da página (começando em 0)
     * @param pageSize tamanho da página
     * @return a página de resultados
     */
    public Page<UserModel> search(
            String firstName,
            String lastName,
            List<Department> departments,
            Integer page,
            Integer pageSize
    ){
        Specification<UserModel> specs = Specification.where((root, query, cb) -> cb.conjunction());

        if (firstName != null){
            specs = specs.and(UserSpecs.firstNameLike(firstName));
        }

        if (lastName != null){
            specs = specs.and(UserSpecs.lastNameLike(lastName));
        }

        if (departments != null && !departments.isEmpty()){
            specs = specs.and(UserSpecs.departmentIn(departments));
        }

        Pageable pageable = PageRequest.of(page, pageSize);

        return userRepository.findAll(specs, pageable);
    }


    /**
     * Concede uma permissão isolada ao usuário, além das que a role dele já dá.
     *
     * @param userModel usuário a receber a permissão
     * @param permission permissão a conceder
     */
    public void addPermission(UserModel userModel, Permission permission){
        if (userModel.getExtraPermissions() == null){
            userModel.setExtraPermissions(new HashSet<>());
        }

        userModel.getExtraPermissions().add(permission);
        userRepository.save(userModel);
        auditLogService.record(AuditAction.PERMISSION_GRANTED, AuditEntityType.USER, userModel.getId(), permission.name());
    }

    /**
     * Remove uma permissão isolada do usuário. Não afeta as permissões que vêm da role
     * dele - só as concedidas individualmente via {@link #addPermission}.
     *
     * @param userModel usuário a perder a permissão
     * @param permission permissão a remover
     */
    public  void removePermission(UserModel userModel, Permission permission){
        if (userModel.getExtraPermissions() != null){
            userModel.getExtraPermissions().remove(permission);
            userRepository.save(userModel);
            auditLogService.record(AuditAction.PERMISSION_REVOKED, AuditEntityType.USER, userModel.getId(), permission.name());
        }
    }
}
