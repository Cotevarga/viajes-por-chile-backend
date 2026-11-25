package cl.viajes.service;

import cl.viajes.dto.SaleCancellationRequest;
import cl.viajes.model.Sale;
import cl.viajes.model.Ticket;
import cl.viajes.repository.SaleRepository;
import cl.viajes.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SaleServiceCancellationTest {
    @Test
    void cancellingOneTicketPreservesOtherSeatsAndRecordsActor() {
        SaleRepository sales = mock(SaleRepository.class);
        TicketRepository tickets = mock(TicketRepository.class);
        ActivityLogService activity = mock(ActivityLogService.class);
        SaleService service = new SaleService(sales, tickets, activity);
        Sale sale = new Sale();
        sale.setSaleCode("sale-code");
        ReflectionTestUtils.setField(sale, "id", 1L);
        Ticket cancelled = new Ticket();
        cancelled.setTicketCode("ticket-one");
        cancelled.setPassengerName("Ana");
        cancelled.setSeatNumber(1);
        cancelled.setTripTitle("Santiago - Talca");
        Ticket active = new Ticket();
        active.setTicketCode("ticket-two");
        TicketRepository.TicketLocation location = mock(TicketRepository.TicketLocation.class);
        when(location.getSaleCode()).thenReturn("sale-code");
        when(tickets.findLocationByCode("ticket-one")).thenReturn(Optional.of(location));
        when(sales.findBySaleCodeForUpdate("sale-code")).thenReturn(Optional.of(sale));
        when(tickets.findActiveTicketsBySaleIdForUpdate(1L)).thenReturn(List.of(cancelled, active));
        when(tickets.findBySale_IdOrderByIdAsc(1L)).thenReturn(List.of(cancelled, active));

        assertEquals("parcial", service.cancel(new SaleCancellationRequest("TICKET-ONE", null, "Cambio"), "Vendedor"));
        assertEquals("cancelado", cancelled.getStatus());
        assertEquals("activo", active.getStatus());
        assertEquals("Vendedor", cancelled.getCancelledBy());
        assertEquals("Cambio", cancelled.getCancelReason());
        verify(activity).record(eq("cancelacion"), anyString(), eq("Vendedor"));
    }
}
