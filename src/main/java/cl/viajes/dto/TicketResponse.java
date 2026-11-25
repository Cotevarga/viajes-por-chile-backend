package cl.viajes.dto;

import cl.viajes.model.Ticket;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.format.DateTimeFormatter;

public record TicketResponse(
        @JsonProperty("ticket_code") String ticketCode,
        @JsonProperty("product_id") String productId,
        @JsonProperty("trip_title") String tripTitle,
        String destination,
        @JsonProperty("schedule_date") String scheduleDate,
        @JsonProperty("departure_time") String departureTime,
        String platform,
        @JsonProperty("seat_number") int seatNumber,
        @JsonProperty("passenger_name") String passengerName,
        @JsonProperty("passenger_rut") String passengerRut,
        @JsonProperty("passenger_email") String passengerEmail,
        @JsonProperty("passenger_phone") String passengerPhone,
        @JsonProperty("seat_class") String seatClass,
        @JsonProperty("original_price") long originalPrice,
        @JsonProperty("discount_percent") int discountPercent,
        @JsonProperty("price_paid") long pricePaid,
        @JsonProperty("distance_km") int distanceKm,
        String status
) {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getTicketCode(), ticket.getProductId(), ticket.getTripTitle(), ticket.getDestination(),
                ticket.getScheduleDate().toString(), TIME_FORMAT.format(ticket.getDepartureTime()),
                ticket.getPlatform(), ticket.getSeatNumber(), ticket.getPassengerName(),
                ticket.getPassengerRut(), ticket.getPassengerEmail(), ticket.getPassengerPhone(), ticket.getSeatClass(),
                ticket.getOriginalPrice(), ticket.getDiscountPercent(),
                ticket.getPricePaid(), ticket.getDistanceKm(), ticket.getStatus()
        );
    }
}
