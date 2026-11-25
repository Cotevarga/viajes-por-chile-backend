package cl.viajes.repository;

import cl.viajes.model.Ticket;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    interface TicketLocation {
        String getProductId();
        String getSaleCode();
    }

    @Query("select t.productId as productId, t.sale.saleCode as saleCode from Ticket t where t.ticketCode = :code")
    Optional<TicketLocation> findLocationByCode(@Param("code") String code);
    Optional<Ticket> findByTicketCode(String ticketCode);
    List<Ticket> findBySale_IdOrderByIdAsc(Long saleId);
    List<Ticket> findByProductIdAndScheduleDateAndDepartureTimeAndStatus(
            String productId, LocalDate scheduleDate, LocalTime departureTime, String status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.productId = :productId and t.scheduleDate = :date and t.departureTime = :time and t.status = 'activo'")
    List<Ticket> findOccupiedForUpdate(@Param("productId") String productId, @Param("date") LocalDate date,
                                       @Param("time") LocalTime time);
    List<Ticket> findByStatusAndCancelledAtBetweenOrderByCancelledAtDesc(
            String status, LocalDateTime from, LocalDateTime to
    );
    List<Ticket> findByStatusAndScheduleDateGreaterThanEqual(String status, LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.sale.id = :saleId and t.status = 'activo'")
    List<Ticket> findActiveTicketsBySaleIdForUpdate(@Param("saleId") Long saleId);
}
