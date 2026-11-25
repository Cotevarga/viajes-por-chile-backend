package cl.viajes.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PurchaseRequest(@NotEmpty List<@NotNull @Valid PurchaseItem> items,
                              @Size(max = 150) String buyerName,
                              @Email @Size(max = 190) String buyerEmail) {
    public PurchaseRequest(List<PurchaseItem> items) {
        this(items, null, null);
    }
}
