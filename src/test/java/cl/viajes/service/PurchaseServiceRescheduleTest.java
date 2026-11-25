package cl.viajes.service;

import cl.viajes.dto.TicketUpdateRequest;
import cl.viajes.model.Product;
import cl.viajes.model.Sale;
import cl.viajes.model.Ticket;
import cl.viajes.repository.ProductRepository;
import cl.viajes.repository.SaleRepository;
import cl.viajes.repository.TicketRepository;
import cl.viajes.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PurchaseServiceRescheduleTest {
    private final ProductRepository products = mock(ProductRepository.class);
    private final SaleRepository sales = mock(SaleRepository.class);
    private final TicketRepository tickets = mock(TicketRepository.class);
    private final ActivityLogService activity = mock(ActivityLogService.class);
    private final PurchaseService service = new PurchaseService(products, sales, tickets, activity, mock(UserRepository.class));
    private final Product product = new Product();
    private final Sale sale = new Sale();
    private final Ticket ticket = new Ticket();
    private final LocalDate departure = LocalDate.now().plusDays(3);

    @BeforeEach
    void setUp() {
        product.setId("route");
        product.setSlug("route");
        product.setType("Salón Cama");
        product.setDistanceKm(500);
        product.setSchedules(List.of(Map.of("id", "next", "date", departure.toString(), "time", "12:00", "platform", "2")));
        ReflectionTestUtils.setField(sale, "id", 1L);
        sale.setSaleCode("sale");
        ReflectionTestUtils.setField(ticket, "id", 2L);
        ticket.setTicketCode("ticket");
        ticket.setSale(sale);
        ticket.setProductId("route");
        ticket.setTripTitle("Santiago - Talca");
        ticket.setDestination("Talca");
        ticket.setScheduleDate(LocalDate.now().plusDays(1));
        ticket.setDepartureTime(LocalTime.of(9, 0));
        ticket.setSeatNumber(71);
        ticket.setSeatClass("Salón Cama");
        ticket.setPassengerName("Ana");
        ticket.setPassengerRut("123456789");
        ticket.setPricePaid(20000);
        TicketRepository.TicketLocation location = mock(TicketRepository.TicketLocation.class);
        when(location.getProductId()).thenReturn("route");
        when(location.getSaleCode()).thenReturn("sale");
        when(tickets.findLocationByCode("ticket")).thenReturn(Optional.of(location));
        when(products.findByIdForUpdate("route")).thenReturn(Optional.of(product));
        when(sales.findBySaleCodeForUpdate("sale")).thenReturn(Optional.of(sale));
        when(tickets.findActiveTicketsBySaleIdForUpdate(1L)).thenReturn(List.of(ticket));
        when(tickets.findOccupiedForUpdate(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void reprogramsSameClassAndKeepsPaidPrice() {
        var result = service.updateTicket("ticket", new TicketUpdateRequest("next", 72, "Ana Pérez", "12345678-9", null, "123"), "Usuario #1");
        assertEquals(72, result.seatNumber());
        assertEquals(20000, result.pricePaid());
        assertEquals(departure, ticket.getScheduleDate());
        verify(activity).record(eq("reprogramacion"), any(), eq("Usuario #1"));
    }

    @Test
    void rejectsOccupiedSeatAndClassChange() {
        Ticket occupied = new Ticket();
        ReflectionTestUtils.setField(occupied, "id", 3L);
        occupied.setSeatNumber(72);
        when(tickets.findOccupiedForUpdate(any(), any(), any())).thenReturn(List.of(occupied));
        assertThrows(ResponseStatusException.class, () -> service.updateTicket("ticket",
                new TicketUpdateRequest("next", 72, "Ana", "123456789", null, null), "Usuario #1"));
        assertThrows(ResponseStatusException.class, () -> service.updateTicket("ticket",
                new TicketUpdateRequest("next", 1, "Ana", "123456789", null, null), "Usuario #1"));
        assertEquals(71, ticket.getSeatNumber());
    }
}
