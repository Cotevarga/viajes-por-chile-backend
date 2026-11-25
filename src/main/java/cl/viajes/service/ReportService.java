package cl.viajes.service;

import cl.viajes.dto.ActivityLogResponse;
import cl.viajes.dto.ContactMessageResponse;
import cl.viajes.dto.FleetEventResponse;
import cl.viajes.dto.ReportSaleResponse;
import cl.viajes.dto.ReportTicketResponse;
import cl.viajes.model.FleetEvent;
import cl.viajes.model.Sale;
import cl.viajes.model.Ticket;
import cl.viajes.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final TicketRepository ticketRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final FleetEventRepository fleetEventRepository;
    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    public ReportService(
            SaleRepository saleRepository,
            TicketRepository ticketRepository,
            ContactMessageRepository contactMessageRepository,
            FleetEventRepository fleetEventRepository,
            ActivityLogRepository activityLogRepository,
            UserRepository userRepository
    ) {
        this.saleRepository = saleRepository;
        this.ticketRepository = ticketRepository;
        this.contactMessageRepository = contactMessageRepository;
        this.fleetEventRepository = fleetEventRepository;
        this.activityLogRepository = activityLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReport(String period) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = switch (period) {
            case "day" -> today;
            case "week" -> today.with(DayOfWeek.MONDAY);
            case "month" -> today.withDayOfMonth(1);
            case "year" -> today.withDayOfYear(1);
            default -> throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Período no válido");
        };
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<Sale> sales = saleRepository.findByCreatedAtBetweenOrderByCreatedAtDescIdDesc(from, now);
        List<Ticket> activeTickets = sales.stream()
                .flatMap(sale -> sale.getTickets().stream())
                .filter(ticket -> "activo".equals(ticket.getStatus()))
                .toList();
        List<Ticket> cancelledTickets = ticketRepository
                .findByStatusAndCancelledAtBetweenOrderByCancelledAtDesc("cancelado", from, now);
        List<Ticket> scheduledTickets = ticketRepository.findByStatusAndScheduleDateGreaterThanEqual("activo", startDate);
        Set<String> completedTrips = new HashSet<>();
        for (Ticket ticket : scheduledTickets) {
            if (!LocalDateTime.of(ticket.getScheduleDate(), ticket.getDepartureTime()).isAfter(now)) {
                completedTrips.add(ticket.getProductId() + "|" + ticket.getScheduleDate() + "|" + ticket.getDepartureTime());
            }
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalSales", activeTickets.stream().mapToLong(Ticket::getPricePaid).sum());
        metrics.put("ticketsSold", activeTickets.size());
        metrics.put("completedTrips", completedTrips.size());
        metrics.put("cancellations", cancelledTickets.size());
        metrics.put("cancelledAmount", cancelledTickets.stream().mapToLong(Ticket::getPricePaid).sum());
        metrics.put("complaints", contactMessageRepository.countByTypeAndStatusNotAndCreatedAtBetween("reclamo", "eliminado", from, now));
        metrics.put("messages", contactMessageRepository.countByTypeAndStatusNotAndCreatedAtBetween("mensaje", "eliminado", from, now));
        metrics.put("unreadMessages", contactMessageRepository.countByStatus("no_leido"));
        metrics.put("newUsers", userRepository.countByCreatedAtBetween(from, now));
        metrics.put("openFleetEvents", fleetEventRepository.countByStatusNot("reparado"));

        List<FleetEventResponse> fleetEvents = fleetEventRepository.findAllByOrderByReportedAtDesc().stream()
                .filter(event -> !event.getReportedAt().isBefore(from) && !event.getReportedAt().isAfter(now))
                .sorted(Comparator.comparing((FleetEvent event) -> "reparado".equals(event.getStatus()))
                        .thenComparing(FleetEvent::getReportedAt, Comparator.reverseOrder()))
                .map(FleetEventResponse::from)
                .toList();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", period);
        report.put("metrics", metrics);
        report.put("purchases", sales.stream().map(ReportSaleResponse::from).toList());
        report.put("cancellations", cancelledTickets.stream().map(ReportTicketResponse::from).toList());
        report.put("fleetEvents", fleetEvents);
        report.put("messages", contactMessageRepository.findAllByOrderByCreatedAtDescIdDesc().stream()
                .filter(message -> !"eliminado".equals(message.getStatus()))
                .filter(message -> !message.getCreatedAt().isBefore(from) && !message.getCreatedAt().isAfter(now))
                .map(ContactMessageResponse::from).toList());
        report.put("activity", activityLogRepository.findByCreatedAtBetweenOrderByCreatedAtDescIdDesc(from, now)
                .stream().map(ActivityLogResponse::from).toList());
        return report;
    }
}
