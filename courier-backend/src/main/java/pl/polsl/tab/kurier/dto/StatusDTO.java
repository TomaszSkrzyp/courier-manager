package pl.polsl.tab.kurier.dto;

import pl.polsl.tab.kurier.model.Status;

public record StatusDTO(Integer id, String name) {
    public static StatusDTO fromEntity(Status status) {
        return new StatusDTO(
                status.getStatusId(),
                status.getName()
        );
    }
}
