package cl.viajes.service;

import cl.viajes.dto.PassengerRequest;
import cl.viajes.dto.PurchaseItem;
import cl.viajes.dto.PurchaseRequest;
import cl.viajes.dto.PurchaseResponse;
import cl.viajes.model.Product;
import cl.viajes.repository.ProductRepository;
import cl.viajes.repository.SaleRepository;
import cl.viajes.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceLongDistanceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private SaleRepository saleRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private ActivityLogService activityLog;

    private PurchaseService purchaseService;

    @BeforeEach
    void setUp() {
        purchaseService = new PurchaseService(productRepository, saleRepository, ticketRepository, activityLog,
                org.mockito.Mockito.mock(cl.viajes.repository.UserRepository.class));
    }

    @Test
    void chargesSalonCamaAndAcceptsItsLowerDeckSeatAtThe400KmThreshold() {
        Product product = longDistanceProduct(400);
        when(productRepository.findBySlugForUpdate("la-serena")).thenReturn(Optional.of(product));
        when(ticketRepository.findByProductIdAndScheduleDateAndDepartureTimeAndStatus(
                "PROD-03", java.time.LocalDate.of(2099, 4, 20), java.time.LocalTime.of(9, 30), "activo"
        )).thenReturn(List.of());
        when(saleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PurchaseResponse result = purchaseService.createPurchase(new PurchaseRequest(List.of(
                new PurchaseItem("la-serena", "schedule-1", false, null, null, "salon",
                        List.of(new PassengerRequest("Ana Pérez", "123456789", 71)))
        )), null);

        assertEquals(25_000, result.totalAmount());
        assertEquals(71, result.tickets().get(0).seatNumber());
        assertEquals(400, result.tickets().get(0).distanceKm());
        assertEquals(25_000, result.tickets().get(0).pricePaid());
    }

    @Test
    void rejectsLowerDeckSeatForRoutesBelow400Km() {
        Product product = longDistanceProduct(399);
        when(productRepository.findBySlugForUpdate("la-serena")).thenReturn(Optional.of(product));
        when(ticketRepository.findByProductIdAndScheduleDateAndDepartureTimeAndStatus(
                "PROD-03", java.time.LocalDate.of(2099, 4, 20), java.time.LocalTime.of(9, 30), "activo"
        )).thenReturn(List.of());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
                purchaseService.createPurchase(new PurchaseRequest(List.of(
                        new PurchaseItem("la-serena", "schedule-1", false, null, null, "salon",
                                List.of(new PassengerRequest("Ana Pérez", "123456789", 71)))
                )), null)
        );

        assertEquals(409, error.getStatusCode().value());
    }

    private Product longDistanceProduct(int distanceKm) {
        Product product = new Product();
        product.setId("PROD-03");
        product.setSlug("la-serena");
        product.setTitle("Santiago - La Serena");
        product.setDestination("La Serena");
        product.setType("Premium");
        product.setStatus("Activo");
        product.setPrice(20_000);
        product.setDistanceKm(distanceKm);
        product.setSchedules(List.of((Object) Map.of(
                "id", "schedule-1",
                "date", "2099-04-20",
                "time", "09:30",
                "platform", "2",
                "capacity", 64
        )));
        return product;
    }
}
