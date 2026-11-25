package cl.viajes.dto;

import cl.viajes.model.Ticket;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record ReportTicketResponse(
        @JsonProperty("ticket_code") String ticketCode,
        @JsonProperty("sale_code") String saleCode,
        @JsonProperty("trip_title") String tripTitle,
        String origin,
        String destination,
        @JsonProperty("schedule_date") String scheduleDate,
        @JsonProperty("departure_time") String departureTime,
        @JsonProperty("seat_number") int seatNumber,
        @JsonProperty("passenger_name") String passengerName,
        @JsonProperty("passenger_rut") String passengerRut,
        @JsonProperty("original_price") long originalPrice,
        @JsonProperty("discount_percent") int discountPercent,
        @JsonProperty("seat_class") String seatClass,
        @JsonProperty("price_paid") long pricePaid,
        String status,
        @JsonProperty("cancelled_at") LocalDateTime cancelledAt,
        @JsonProperty("cancel_reason") String cancelReason,
        @JsonProperty("cancelled_by") String cancelledBy
) {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static ReportTicketResponse from(Ticket ticket) {
        return new ReportTicketResponse(
                ticket.getTicketCode(), ticket.getSale().getSaleCode(), ticket.getTripTitle(), ticket.getOrigin(), ticket.getDestination(),
                ticket.getScheduleDate().toString(), TIME_FORMAT.format(ticket.getDepartureTime()),
                ticket.getSeatNumber(), ticket.getPassengerName(), ticket.getPassengerRut(),
                ticket.getOriginalPrice(), ticket.getDiscountPercent(),
                ticket.getSeatClass(),
                ticket.getPricePaid(),
                ticket.getStatus(), ticket.getCancelledAt(), ticket.getCancelReason(), ticket.getCancelledBy()
        );
    }
}
