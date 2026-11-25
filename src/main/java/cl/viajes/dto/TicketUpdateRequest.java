package cl.viajes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketUpdateRequest(
        @NotBlank String scheduleId,
        @NotNull Integer seatNumber,
        @NotBlank @Size(max = 150) String passengerName,
        @NotBlank @Size(max = 20) String passengerRut,
        @Email @Size(max = 190) String passengerEmail,
        @Size(max = 40) String passengerPhone
) {}
