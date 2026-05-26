package pl.polsl.tab.kurier.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ParcelCreateDTO {
    @NotNull
    @Pattern(regexp = "^(\\+?\\d[\\d\\s]{7,14})$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotNull
    private String senderStreet;
    @NotNull
    private String senderBuildingNumber;
    @NotNull
    private String senderPostalCode;
    @NotNull
    private Integer senderRegionId;

    @NotNull
    private String recipientStreet;
    @NotNull
    private String recipientBuildingNumber;
    @NotNull
    private String recipientPostalCode;
    @NotNull
    private Integer recipientRegionId;

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

    private String fragility;
    @NotNull
    private Integer deliveryModeId;
    private String comment;
}
