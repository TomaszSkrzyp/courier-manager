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

public class ParcelCreateDTOTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void whenValidDTO_thenNoViolations() {
        ParcelCreateDTO dto = createValidDTO();
        Set<ConstraintViolation<ParcelCreateDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void whenWeightTooHigh_thenViolation() {
        ParcelCreateDTO dto = createValidDTO();
        dto.setWeight(150.0);
        Set<ConstraintViolation<ParcelCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void whenHeightTooLow_thenViolation() {
        ParcelCreateDTO dto = createValidDTO();
        dto.setHeight(0.5);
        Set<ConstraintViolation<ParcelCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void whenDimensionsTooHigh_thenViolation() {
        ParcelCreateDTO dto = createValidDTO();
        dto.setLength(201.0);
        Set<ConstraintViolation<ParcelCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    private ParcelCreateDTO createValidDTO() {
        ParcelCreateDTO dto = new ParcelCreateDTO();
        dto.setPhoneNumber("123456789");
        dto.setSenderStreet("Street");
        dto.setSenderBuildingNumber("1");
        dto.setSenderPostalCode("00-000");
        dto.setSenderRegionId(1);
        dto.setRecipientStreet("Street");
        dto.setRecipientBuildingNumber("2");
        dto.setRecipientPostalCode("00-000");
        dto.setRecipientRegionId(2);
        dto.setWeight(10.0);
        dto.setHeight(10.0);
        dto.setWidth(10.0);
        dto.setLength(10.0);
        dto.setDeliveryModeId(1);
        return dto;
    }
}
