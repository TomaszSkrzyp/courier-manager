package pl.polsl.tab.kurier.dto;

import lombok.Data;
import pl.polsl.tab.kurier.model.Parcel;

import java.time.format.DateTimeFormatter;

@Data
public class ParcelDTO {
    private Integer parcelId;
    private String trackingNumber;
    private String city;
    private String status;
    private Boolean verified;
    private String expectedDelivery;
    private String date;

    public static ParcelDTO fromEntity(Parcel parcel) {
        ParcelDTO dto = new ParcelDTO();
        dto.setParcelId(parcel.getParcelId());
        dto.setTrackingNumber(parcel.getTrackingNumber());
        
        if (parcel.getDestinationAddress() != null && parcel.getDestinationAddress().getRegion() != null) {
            dto.setCity(parcel.getDestinationAddress().getRegion().getName());
        }
        
        if (parcel.getStatus() != null) {
            dto.setStatus(parcel.getStatus().getName());
        }
        
        dto.setVerified(parcel.getVerified());
        
        if (parcel.getExpectedTime() != null) {
            dto.setExpectedDelivery(parcel.getExpectedTime().toLocalDate().toString());
        }
        
        if (parcel.getCreatedAt() != null) {
            dto.setDate(parcel.getCreatedAt().toLocalDate().toString());
        }
        
        return dto;
    }
}
