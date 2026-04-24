package pl.polsl.tab.kurier.dto;

import lombok.Data;

@Data
public class ParcelCreateDTO {
    private String phoneNumber;

    private String senderStreet;
    private String senderBuildingNumber;
    private String senderPostalCode;
    private Integer senderRegionId;

    private String recipientStreet;
    private String recipientBuildingNumber;
    private String recipientPostalCode;
    private Integer recipientRegionId;

    private Double weight;
    private Double height;
    private Double width;
    private Double length;
    private String fragility;
    private Integer deliveryModeId;
    private String comment;
}
