package pl.polsl.tab.kurier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import pl.polsl.tab.kurier.model.Region;

@Data
public class RegionDTO {
    private Integer id;

    @NotBlank(message = "Region name is required")
    private String name;

    public static RegionDTO fromEntity(Region region) {
        RegionDTO dto = new RegionDTO();
        dto.setId(region.getRegionId());
        dto.setName(region.getName());
        return dto;
    }
}
