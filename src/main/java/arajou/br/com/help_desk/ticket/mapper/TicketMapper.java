package arajou.br.com.help_desk.ticket.mapper;

import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.dto.TicketCreatedDTO;
import arajou.br.com.help_desk.ticket.model.dto.TicketResponseDTO;
import arajou.br.com.help_desk.ticket.model.dto.TicketUpdatedDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Converte entre {@link Ticket} e os DTOs de chamado. Diferente do {@code UserMapper}, é
 * uma {@code interface} simples (não precisa de {@code abstract class}), porque não
 * depende de nenhum repositório - o cliente/técnico do chamado nunca vêm de um id enviado
 * pelo DTO, são resolvidos pelo {@code TicketService}/{@code SecurityService}.
 */
@Mapper(componentModel = "spring") // Faz o MapStruct gerar a implementação como um @Component, injetável via construtor
public interface TicketMapper {

    /**
     * Converte o DTO de abertura de chamado na entidade a ser persistida. title/description/
     * theme têm o mesmo nome nos dois lados, então o MapStruct mapeia automaticamente;
     * customer e status ficam com seus valores padrão (nulo e PENDING), preenchidos depois
     * pelo Service.
     *
     * @param ticketCreatedDTO dados de abertura vindos da requisição
     * @return a entidade pronta para ser salva (ainda sem customer)
     */
    Ticket toEntity(TicketCreatedDTO ticketCreatedDTO);

    /**
     * Atualiza a descrição de uma entidade já existente com os dados do DTO de edição.
     *
     * @param ticketUpdatedDTO novos dados vindos da requisição
     * @param entity entidade já carregada do banco, atualizada em memória (não salva aqui)
     */
    void updateEntityFromDTO(TicketUpdatedDTO ticketUpdatedDTO, @MappingTarget Ticket entity);

    /**
     * Converte a entidade salva no banco no DTO de resposta da API, resolvendo também o
     * id/nome do cliente e do técnico (quando houver).
     *
     * @param ticket entidade a converter
     * @return o DTO de resposta, pronto para ser serializado em JSON
     */
    @Mapping(target = "customerId", expression = "java( ticket.getCustomer() != null ? ticket.getCustomer().getId() : null )")
    @Mapping(target = "customerName", expression = "java( ticket.getCustomer() != null ? ticket.getCustomer().getFirstName() + \" \" + ticket.getCustomer().getLastName() : null )")
    @Mapping(target = "technicianId", expression = "java( ticket.getTechnician() != null ? ticket.getTechnician().getId() : null )")
    @Mapping(target = "technicianName", expression = "java( ticket.getTechnician() != null ? ticket.getTechnician().getFirstName() + \" \" + ticket.getTechnician().getLastName() : null )")
    TicketResponseDTO toTicketResponseDTO(Ticket ticket);
}
