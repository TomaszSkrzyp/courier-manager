package pl.polsl.tab.kurier.dto;

import lombok.Data;
import pl.polsl.tab.kurier.model.Parcel;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Data
public class ParcelDTO {
    private Integer parcelId;
    private String trackingNumber;

    // Destination info
    private String city;             // destination region name

    // Sender info
    private String senderCity;       // sender region name

    // Routing
    private String nextRegion;       // current next hop region name

    // Status & verification
    private String status;
    private Boolean verified;

    // Dates
    private String expectedDelivery;
    private String date;

    // Physical attributes (useful for couriers)
    private BigDecimal weight;
    private BigDecimal height;
    private BigDecimal width;
    private BigDecimal length;
    private String fragility;

    public static ParcelDTO fromEntity(Parcel parcel) {
        ParcelDTO dto = new ParcelDTO();
        dto.setParcelId(parcel.getParcelId());
        dto.setTrackingNumber(parcel.getTrackingNumber());

        if (parcel.getDestinationAddress() != null && parcel.getDestinationAddress().getRegion() != null) {
            dto.setCity(parcel.getDestinationAddress().getRegion().getName());
        }

        if (parcel.getSenderAddress() != null && parcel.getSenderAddress().getRegion() != null) {
            dto.setSenderCity(parcel.getSenderAddress().getRegion().getName());
        }

        if (parcel.getNextRegion() != null) {
            dto.setNextRegion(parcel.getNextRegion().getName());
        }

        if (parcel.getStatus() != null) {
            dto.setStatus(parcel.getStatus().getName());
        }

        dto.setVerified(parcel.getVerified());
        dto.setWeight(parcel.getWeight());
        dto.setHeight(parcel.getHeight());
        dto.setWidth(parcel.getWidth());
        dto.setLength(parcel.getLength());
        dto.setFragility(parcel.getFragility());

        if (parcel.getExpectedTime() != null) {
            dto.setExpectedDelivery(parcel.getExpectedTime().toLocalDate().toString());
        }

        if (parcel.getCreatedAt() != null) {
            dto.setDate(parcel.getCreatedAt().toLocalDate().toString());
        }

        return dto;
    }
}
