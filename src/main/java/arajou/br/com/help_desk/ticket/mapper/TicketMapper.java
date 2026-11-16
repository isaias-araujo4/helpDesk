package arajou.br.com.help_desk.ticket.mapper;

import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.dto.TicketCreatedDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    Ticket toEntity(TicketCreatedDTO ticketCreatedDTO);

    @Mapping(target = "customerId", expression = "java( ticket.getCustomer() != null ? ticket.getCustomer().getId() : null )")
    @Mapping(target = "customerName", expression = "java( ticket.getCustomer() != null ? ticket.getCustomer().getFirstName() + \" \" + ticket.getCustomer().getLastName() : null )")
    @Mapping(target = "technicianId", expression = "java( ticket.getTechnician() != null ? ticket.getTechnician().getId() : null )")
    @Mapping(target = "technicianName", expression = "java( ticket.getTechnician() != null ? ticket.getTechnician().getFirstName() + \" \" + ticket.getTechnician().getLastName() : null )")
    TicketResponseDTO toTicketResponseDTO(Ticket ticket);
}
