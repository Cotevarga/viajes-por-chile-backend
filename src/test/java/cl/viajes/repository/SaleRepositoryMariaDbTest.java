package cl.viajes.repository;

import cl.viajes.model.Sale;
import cl.viajes.model.Ticket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfEnvironmentVariable(named = "REPORTING_POS_DB_TEST", matches = "true")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=none")
class SaleRepositoryMariaDbTest {
    @Autowired private SaleRepository sales;

    @Test
    void searchesOrdersByCodePassengerRutAndDate() {
        Sale sale = new Sale();
        sale.setSaleCode("test-sale");
        sale.setTotalAmount(10000);
        Ticket ticket = new Ticket();
        ticket.setTicketCode("test-ticket");
        ticket.setProductId("test-route");
        ticket.setTripTitle("Santiago - Talca");
        ticket.setDestination("Talca");
        ticket.setScheduleDate(LocalDate.now().plusDays(1));
        ticket.setDepartureTime(LocalTime.NOON);
        ticket.setPlatform("1");
        ticket.setSeatNumber(1);
        ticket.setPassengerName("Ana Perez");
        ticket.setPassengerRut("123456789");
        ticket.setOriginalPrice(10000);
        ticket.setPricePaid(10000);
        sale.addTicket(ticket);
        sales.saveAndFlush(sale);

        assertEquals(1, sales.searchOrders("test-sale", null, null).size());
        assertEquals(1, sales.searchOrders("ana", null, null).size());
        assertEquals(1, sales.searchOrders("123456789", null, null).size());
        assertEquals(1, sales.searchOrders("", LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay()).size());
        assertEquals(0, sales.searchOrders("", LocalDate.now().minusDays(1).atStartOfDay(), LocalDate.now().atStartOfDay()).size());
    }
}
