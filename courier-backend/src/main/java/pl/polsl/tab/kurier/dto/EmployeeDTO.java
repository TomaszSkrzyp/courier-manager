package pl.polsl.tab.kurier.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import pl.polsl.tab.kurier.model.Employee;
import pl.polsl.tab.kurier.model.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class EmployeeDTO {
    private Integer id;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Role is required")
    private String role;

    @NotBlank(message = "Login is required")
    @Size(min = 3, message = "Login must be at least 3 characters")
    private String login;

    private String password;

    @NotBlank(message = "PESEL is required")
    @Pattern(regexp = "^\\d{11}$", message = "PESEL must be exactly 11 digits")
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
