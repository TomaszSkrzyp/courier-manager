package pl.polsl.tab.kurier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ParcelPriceRequestDTO {
    @DecimalMin(value = "0.1", message = "Weight must be at least 0.1")
    @Max(value = 100, message = "Weight must be less than or equal to 100")
    private Double weight;

    @Min(value = 1, message = "Height must be at least 1")
    @Max(value = 200, message = "Height must be less than or equal to 200")
    private Double height;

    @Min(value = 1, message = "Width must be at least 1")
    @Max(value = 200, message = "Width must be less than or equal to 200")
    private Double width;

    @Min(value = 1, message = "Length must be at least 1")
    @Max(value = 200, message = "Length must be less than or equal to 200")
    private Double length;

    @NotNull(message = "Delivery mode is required")
    private Integer deliveryModeId;
}
