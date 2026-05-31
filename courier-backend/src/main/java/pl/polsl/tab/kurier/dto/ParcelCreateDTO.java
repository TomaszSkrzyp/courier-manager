package pl.polsl.tab.kurier.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ParcelCreateDTO {
    @NotNull
    @Pattern(regexp = "^(\\+?\\d[\\d\\s]{7,14})$", message = "Invalid phone number format")
    private String phoneNumber;

    @NotBlank(message = "Sender street is required")
    private String senderStreet;
    
    @NotBlank(message = "Sender building number is required")
    private String senderBuildingNumber;
    
    @NotBlank(message = "Sender postal code is required")
    @Pattern(regexp = "^\\d{2}-\\d{3}$", message = "Invalid postal code format (00-000)")
    private String senderPostalCode;
    
    @NotNull(message = "Sender region is required")
    private Integer senderRegionId;

    @NotBlank(message = "Recipient street is required")
    private String recipientStreet;
    
    @NotBlank(message = "Recipient building number is required")
    private String recipientBuildingNumber;
    
    @NotBlank(message = "Recipient postal code is required")
    @Pattern(regexp = "^\\d{2}-\\d{3}$", message = "Invalid postal code format (00-000)")
    private String recipientPostalCode;
    
    @NotNull(message = "Recipient region is required")
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
