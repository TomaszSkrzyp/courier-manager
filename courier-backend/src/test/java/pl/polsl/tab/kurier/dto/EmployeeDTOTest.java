package pl.polsl.tab.kurier.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmployeeDTOTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void whenValidDTO_thenNoViolations() {
        EmployeeDTO dto = createValidDTO();
        Set<ConstraintViolation<EmployeeDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid EmployeeDTO should have no violations");
    }

    @Test
    public void whenPeselInvalid_thenViolation() {
        EmployeeDTO dto = createValidDTO();
        String[] invalidPesels = {"1234567890", "123456789012", "1234567890a", "abcdefghijk", ""};
        for (String pesel : invalidPesels) {
            dto.setPesel(pesel);
            assertFalse(validator.validate(dto).isEmpty(), "PESEL '" + pesel + "' should be invalid");
        }
    }

    @Test
    public void whenLoginInvalid_thenViolation() {
        EmployeeDTO dto = createValidDTO();
        dto.setLogin("ab"); // Too short
        assertFalse(validator.validate(dto).isEmpty(), "Login < 3 chars should be invalid");
        
        dto.setLogin("");
        assertFalse(validator.validate(dto).isEmpty(), "Blank login should be invalid");
        
        dto.setLogin("   ");
        assertFalse(validator.validate(dto).isEmpty(), "Whitespace-only login should be invalid");
    }

    @Test
    public void whenMandatoryFieldsBlank_thenViolation() {
        String[] fields = {"firstName", "lastName", "role", "login", "pesel"};
        for (String field : fields) {
            EmployeeDTO dto = createValidDTO();
            try {
                java.lang.reflect.Field f = dto.getClass().getDeclaredField(field);
                f.setAccessible(true);
                f.set(dto, "");
                assertFalse(validator.validate(dto).isEmpty(), field + " being blank should be invalid");
                
                f.set(dto, null);
                assertFalse(validator.validate(dto).isEmpty(), field + " being null should be invalid");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private EmployeeDTO createValidDTO() {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setRole("COURIER");
        dto.setLogin("johndoe");
        dto.setPesel("12345678901");
        return dto;
    }
}
