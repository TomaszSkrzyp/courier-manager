package pl.polsl.tab.kurier.dto;

import lombok.Data;
import pl.polsl.tab.kurier.model.PriceDelta;

import java.math.BigDecimal;

@Data
public class PriceDeltaDTO {
    private Integer deltaId;
    private BigDecimal weightDelta;
    private BigDecimal lengthDelta;
    private BigDecimal widthDelta;
    private BigDecimal heightDelta;
    private BigDecimal normalModeDelta;
    private BigDecimal expressModeDelta;
    private String createdAt;

    public static PriceDeltaDTO fromEntity(PriceDelta entity) {
        PriceDeltaDTO dto = new PriceDeltaDTO();
        dto.setDeltaId(entity.getDeltaId());
        dto.setWeightDelta(entity.getWeightDelta());
        dto.setLengthDelta(entity.getLengthDelta());
        dto.setWidthDelta(entity.getWidthDelta());
        dto.setHeightDelta(entity.getHeightDelta());
        dto.setNormalModeDelta(entity.getNormalModeDelta());
        dto.setExpressModeDelta(entity.getExpressModeDelta());
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().toString());
        }
        return dto;
    }
}
