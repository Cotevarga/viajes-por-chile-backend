package cl.viajes.repository;

import cl.viajes.dto.OrderSummaryResponse;
import cl.viajes.model.Sale;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    Optional<Sale> findBySaleCode(String saleCode);
    List<Sale> findByCreatedAtBetweenOrderByCreatedAtDescIdDesc(LocalDateTime from, LocalDateTime to);

    @Query("""
            select new cl.viajes.dto.OrderSummaryResponse(
                s.saleCode, s.totalAmount, s.status, s.createdAt, count(t.id)
            )
            from Sale s left join s.tickets t
            group by s.id, s.saleCode, s.totalAmount, s.status, s.createdAt
            order by s.createdAt desc, s.id desc
            """)
    List<OrderSummaryResponse> findAllOrderSummaries();

    @Query("""
            select new cl.viajes.dto.OrderSummaryResponse(
                s.saleCode, s.totalAmount, s.status, s.createdAt, count(t.id)
            )
            from Sale s left join s.tickets t
            where (:search = '' or lower(s.saleCode) like concat('%', :search, '%')
                or exists (select t2.id from Ticket t2 where t2.sale = s
                    and (lower(t2.passengerName) like concat('%', :search, '%')
                        or lower(t2.passengerRut) like concat('%', :search, '%'))))
              and (:fromDate is null or s.createdAt >= :fromDate)
              and (:toDate is null or s.createdAt < :toDate)
            group by s.id, s.saleCode, s.totalAmount, s.status, s.createdAt
            order by s.createdAt desc, s.id desc
            """)
    List<OrderSummaryResponse> searchOrders(@Param("search") String search,
                                            @Param("fromDate") LocalDateTime fromDate,
                                            @Param("toDate") LocalDateTime toDate);

    @Query("""
            select new cl.viajes.dto.OrderSummaryResponse(
                s.saleCode, s.totalAmount, s.status, s.createdAt, count(t.id)
            )
            from Sale s left join s.tickets t
            where s.userId = :userId
            group by s.id, s.saleCode, s.totalAmount, s.status, s.createdAt
            order by s.createdAt desc, s.id desc
            """)
    List<OrderSummaryResponse> findOrderSummariesByUser(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.saleCode = :saleCode")
    Optional<Sale> findBySaleCodeForUpdate(@Param("saleCode") String saleCode);
}
