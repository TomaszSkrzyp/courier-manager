package pl.polsl.tab.kurier.dto;

import lombok.Data;
import pl.polsl.tab.kurier.model.Region;

@Data
public class RegionDTO {
    private Integer id;
    private String name;

    public static RegionDTO fromEntity(Region region) {
        RegionDTO dto = new RegionDTO();
        dto.setId(region.getRegionId());
        dto.setName(region.getName());
        return dto;
    }
}
