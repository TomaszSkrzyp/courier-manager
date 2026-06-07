package pl.polsl.tab.kurier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pl.polsl.tab.kurier.dto.EmployeeDTO;
import pl.polsl.tab.kurier.model.Employee;
import pl.polsl.tab.kurier.model.Region;
import pl.polsl.tab.kurier.model.Role;
import pl.polsl.tab.kurier.repository.EmployeeRepository;
import pl.polsl.tab.kurier.repository.RoleRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RegionService regionService;

    public Optional<EmployeeDTO> login(String login, String password) {
        return employeeRepository.findByLogin(login)
                .filter(employee -> employee.getPassword().equals(password))
                .map(EmployeeDTO::fromEntity);
    }

    public List<EmployeeDTO> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(EmployeeDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<EmployeeDTO> getEmployeesPage(Pageable pageable) {
        return employeeRepository.findAll(pageable).map(EmployeeDTO::fromEntity);
    }

    public EmployeeDTO createEmployee(EmployeeDTO dto) {
        Employee employee = new Employee();
        updateEmployeeFromDto(employee, dto);
        
        // No longer forcing a default address/Warsaw region.
        // If the UI doesn't provide an address, it remains null.
        
        Employee saved = employeeRepository.save(employee);
        return EmployeeDTO.fromEntity(saved);
    }

    public Optional<EmployeeDTO> updateEmployee(Integer id, EmployeeDTO dto) {
        return employeeRepository.findById(id).map(employee -> {
            updateEmployeeFromDto(employee, dto);
            return EmployeeDTO.fromEntity(employeeRepository.save(employee));
        });
    }

    public boolean deleteEmployee(Integer id) {
        return employeeRepository.findById(id).map(employee -> {
            if ("ADMIN".equals(employee.getRole().getName())) {
                throw new pl.polsl.tab.kurier.exception.ResourceBusyException("Cannot delete administrator account.");
            }
            employeeRepository.delete(employee);
            return true;
        }).orElse(false);
    }

    private void updateEmployeeFromDto(Employee employee, EmployeeDTO dto) {
        employee.setFirstName(dto.getFirstName());
        employee.setLastName(dto.getLastName());
        employee.setLogin(dto.getLogin());
        
        if (dto.getPesel() != null) {
            employee.setPesel(dto.getPesel());
        }
        
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            employee.setPassword(dto.getPassword());
        } else if (employee.getPassword() == null) {
            employee.setPassword("default123");
        }

        if (dto.getRole() != null) {
            Role role = roleRepository.findByName(dto.getRole())
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setName(dto.getRole());
                        return roleRepository.save(newRole);
                    });
            employee.setRole(role);
        }

        if (dto.getRegions() != null) {
            Set<Region> employeeRegions = new HashSet<>();
            for (String regionName : dto.getRegions()) {
                if (regionName != null && !regionName.trim().isEmpty()) {
                    Region region = regionService.getRegionByName(regionName.trim())
                            .orElseThrow(() -> new RuntimeException("Region not found: " + regionName));
                    employeeRegions.add(region);
                }
            }
            employee.setRegions(employeeRegions);
        }
    }
}
