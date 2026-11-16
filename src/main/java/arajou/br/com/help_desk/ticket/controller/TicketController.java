package arajou.br.com.help_desk.ticket.controller;

import arajou.br.com.help_desk.common.GenericController;
import arajou.br.com.help_desk.security.SecurityService;
import arajou.br.com.help_desk.ticket.mapper.TicketMapper;
import arajou.br.com.help_desk.ticket.model.Ticket;
import arajou.br.com.help_desk.ticket.model.dto.TicketCreatedDTO;
import arajou.br.com.help_desk.ticket.model.dto.TicketResponseDTO;
import arajou.br.com.help_desk.ticket.model.dto.TicketUpdatedDTO;
import arajou.br.com.help_desk.ticket.model.enums.TicketStatus;
import arajou.br.com.help_desk.ticket.model.enums.TicketTheme;
import arajou.br.com.help_desk.ticket.service.TicketService;
import arajou.br.com.help_desk.ticket.validator.TicketValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("tickets")
@RequiredArgsConstructor
public class TicketController implements GenericController {

    private final TicketService ticketService;
    private final TicketMapper ticketMapper;
    private final TicketValidator ticketValidator;
    private final SecurityService securityService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_TICKET')")
    public ResponseEntity<Void> create(@RequestBody @Valid TicketCreatedDTO ticketCreatedDTO){
        Ticket ticket = ticketMapper.toEntity(ticketCreatedDTO);
        Ticket saved = ticketService.save(ticket);
        URI location = generateLocationHeader(saved.getId());
        return ResponseEntity.created(location).build();
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_TICKETS','VIEW_PENDING_TICKETS','VIEW_REPORTS')")
    public ResponseEntity<TicketResponseDTO> findById(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketValidator.validateView(ticket, securityService.getAuthenticatedUser());
                    return ResponseEntity.ok(ticketMapper.toTicketResponseDTO(ticket));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("{id}")
    @PreAuthorize("hasAuthority('CREATE_TICKET')")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody @Valid TicketUpdatedDTO ticketUpdatedDTO) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.updateDescription(ticket, ticketUpdatedDTO.description());
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("{id}/assume")
    @PreAuthorize("hasAuthority('ASSUME_TICKET')")
    public ResponseEntity<Void> assume(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.assume(ticket);
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("{id}/resolve")
    @PreAuthorize("hasAuthority('RESOLVE_TICKET')")
    public ResponseEntity<Void> resolve(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.resolve(ticket);
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("{id}/close")
    @PreAuthorize("hasAuthority('CLOSE_TICKET')")
    public ResponseEntity<Void> close(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.close(ticket);
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("{id}/cancel")
    @PreAuthorize("hasAuthority('CANCEL_TICKET')")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.cancel(ticket);
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("{id}/reopen")
    @PreAuthorize("hasAuthority('REOPEN_TICKET')")
    public ResponseEntity<Void> reopen(@PathVariable Long id) {
        return ticketService.findById(id)
                .map(ticket -> {
                    ticketService.reopen(ticket);
                    return ResponseEntity.noContent().<Void>build();
                }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('VIEW_OWN_TICKETS','VIEW_PENDING_TICKETS','VIEW_REPORTS')")
    public ResponseEntity<Page<TicketResponseDTO>> search(
            @RequestParam(value = "status", required = false) TicketStatus status,
            @RequestParam(value = "theme", required = false) TicketTheme theme,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        Page<Ticket> result = ticketService.search(status, theme, page, pageSize);
        Page<TicketResponseDTO> dtoPage = result.map(ticketMapper::toTicketResponseDTO);

        return ResponseEntity.ok(dtoPage);
    }
}
