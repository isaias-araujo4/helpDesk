package arajou.br.com.help_desk.ticket.mapper;

import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.dto.TicketCreatedDTO;
import arajou.br.com.help_desk.ticket.model.dto.TicketResponseDTO;
import arajou.br.com.help_desk.ticket.model.dto.TicketUpdatedDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    Ticket toEntity(TicketCreatedDTO ticketCreatedDTO);

    void updateEntityFromDTO(TicketUpdatedDTO ticketUpdatedDTO, @MappingTarget Ticket entity);

    @Mapping(target = "customerId", expression = "java( ticket.getCustomer() != null ? ticket.getCustomer().getId() : null )")
    @Mapping(target = "customerName", expression = "java( ticket.getCustomer() != null ? ticket.getCustomer().getFirstName() + \" \" + ticket.getCustomer().getLastName() : null )")
    @Mapping(target = "technicianId", expression = "java( ticket.getTechnician() != null ? ticket.getTechnician().getId() : null )")
    @Mapping(target = "technicianName", expression = "java( ticket.getTechnician() != null ? ticket.getTechnician().getFirstName() + \" \" + ticket.getTechnician().getLastName() : null )")
    TicketResponseDTO toTicketResponseDTO(Ticket ticket);
}
