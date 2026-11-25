package cl.viajes.service;

import cl.viajes.model.Sale;
import cl.viajes.repository.ProductRepository;
import cl.viajes.repository.SaleRepository;
import cl.viajes.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceAccessTest {

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
    void deniesPurchaseDetailsToAnotherCustomer() {
        Sale sale = saleFor(12L);
        when(saleRepository.findBySaleCode("sale-code")).thenReturn(Optional.of(sale));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> purchaseService.getPurchase("sale-code", 34L, false)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void allowsTheOwnerAndAdministratorsToReadPurchaseDetails() {
        Sale sale = saleFor(12L);
        when(saleRepository.findBySaleCode("sale-code")).thenReturn(Optional.of(sale));

        assertEquals("sale-code", purchaseService.getPurchase("sale-code", 12L, false).saleCode());
        assertEquals("sale-code", purchaseService.getPurchase("sale-code", 34L, true).saleCode());
    }

    private Sale saleFor(Long userId) {
        Sale sale = new Sale();
        sale.setSaleCode("sale-code");
        sale.setUserId(userId);
        sale.setTotalAmount(8500);
        return sale;
    }
}
