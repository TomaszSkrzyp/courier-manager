package pl.polsl.tab.kurier.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pl.polsl.tab.kurier.model.PriceDelta;

import java.math.BigDecimal;

@Data
public class PriceDeltaDTO {
    private Integer deltaId;

    @NotNull(message = "Weight delta is required")
    @DecimalMin(value = "0.0", message = "Weight delta cannot be negative")
    private BigDecimal weightDelta;

    @NotNull(message = "Length delta is required")
    @DecimalMin(value = "0.0", message = "Length delta cannot be negative")
    private BigDecimal lengthDelta;

    @NotNull(message = "Width delta is required")
    @DecimalMin(value = "0.0", message = "Width delta cannot be negative")
    private BigDecimal widthDelta;

    @NotNull(message = "Height delta is required")
    @DecimalMin(value = "0.0", message = "Height delta cannot be negative")
    private BigDecimal heightDelta;

    @NotNull(message = "Normal mode delta is required")
    @DecimalMin(value = "0.0", message = "Normal mode delta cannot be negative")
    private BigDecimal normalModeDelta;

    @NotNull(message = "Express mode delta is required")
    @DecimalMin(value = "0.0", message = "Express mode delta cannot be negative")
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
