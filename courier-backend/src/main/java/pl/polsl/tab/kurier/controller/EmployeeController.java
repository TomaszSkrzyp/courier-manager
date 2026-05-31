package pl.polsl.tab.kurier.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.polsl.tab.kurier.dto.EmployeeDTO;
import pl.polsl.tab.kurier.dto.LoginRequestDTO;
import pl.polsl.tab.kurier.service.EmployeeService;


@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/login")
    public ResponseEntity<EmployeeDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        return employeeService.login(loginRequest.getLogin(), loginRequest.getPassword())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }

    @GetMapping
    public ResponseEntity<Page<EmployeeDTO>> getAllEmployees(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(employeeService.getEmployeesPage(pageable));
    }

    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(@RequestBody EmployeeDTO dto) {
        return ResponseEntity.ok(employeeService.createEmployee(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(@PathVariable Integer id, @RequestBody EmployeeDTO dto) {
        return employeeService.updateEmployee(id, dto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Integer id) {
        if (employeeService.deleteEmployee(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
