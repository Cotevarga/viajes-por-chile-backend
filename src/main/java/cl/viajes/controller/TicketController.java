package cl.viajes.controller;

import cl.viajes.dto.TicketResponse;
import cl.viajes.dto.TicketUpdateRequest;
import cl.viajes.service.PurchaseService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final PurchaseService purchaseService;

    public TicketController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PutMapping("/{ticketCode}")
    public TicketResponse update(@PathVariable String ticketCode, @Valid @RequestBody TicketUpdateRequest request,
                                 Authentication authentication) {
        return purchaseService.updateTicket(ticketCode, request, "Usuario #" + authentication.getName());
    }
}
