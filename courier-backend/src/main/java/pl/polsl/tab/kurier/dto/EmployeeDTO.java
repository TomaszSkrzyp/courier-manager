package pl.polsl.tab.kurier.dto;

import lombok.Data;
import pl.polsl.tab.kurier.model.Employee;
import pl.polsl.tab.kurier.model.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class EmployeeDTO {
    private Integer id;
    private String firstName;
    private String lastName;
    private String role;
    private String login;
    private String password;
    private String pesel;
    private List<String> regions = new ArrayList<>();
    private String dateAdded;

    public static EmployeeDTO fromEntity(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(employee.getEmployeeId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        if (employee.getRole() != null) {
            dto.setRole(employee.getRole().getName());
        }
        dto.setLogin(employee.getLogin());
        dto.setPesel(employee.getPesel());
        
        if (employee.getRegions() != null) {
            dto.setRegions(employee.getRegions().stream()
                    .map(Region::getName)
                    .collect(Collectors.toList()));
        }
        
        if (employee.getCreatedAt() != null) {
            dto.setDateAdded(employee.getCreatedAt().toLocalDate().toString());
        }
        
        return dto;
    }
}
