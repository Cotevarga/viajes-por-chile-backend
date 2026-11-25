package cl.viajes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record PassengerRequest(String name, String rut, Integer seatNumber,
                               @Email @Size(max = 190) String email,
                               @Size(max = 40) String telephone) {
    public PassengerRequest(String name, String rut, Integer seatNumber) {
        this(name, rut, seatNumber, null, null);
    }
}
