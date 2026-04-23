package pl.polsl.tab.kurier.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourierStatsDTO {
    private String firstName;
    private String lastName;
    private Long parcelCount;
}
