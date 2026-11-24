package cl.viajes.controller;

import cl.viajes.service.PurchaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/availability")
public class SeatAvailabilityController {

    private final PurchaseService purchaseService;

    public SeatAvailabilityController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping("/seats")
    public List<Integer> occupiedSeats(
            @RequestParam String slug,
            @RequestParam String scheduleId
    ) {
        return purchaseService.occupiedSeats(slug, scheduleId);
    }
}
