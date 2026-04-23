package pl.polsl.tab.kurier.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegionStatsDTO {
    private String regionName;
    private Long parcelCount;
}
