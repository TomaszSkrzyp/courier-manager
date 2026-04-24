package pl.polsl.tab.kurier.dto;

import pl.polsl.tab.kurier.model.DeliveryMode;

public record DeliveryModeDTO(Integer id, String name) {
    public static DeliveryModeDTO fromEntity(DeliveryMode deliveryMode) {
        return new DeliveryModeDTO(
                deliveryMode.getDeliveryModeId(),
                deliveryMode.getName()
        );
    }
}
