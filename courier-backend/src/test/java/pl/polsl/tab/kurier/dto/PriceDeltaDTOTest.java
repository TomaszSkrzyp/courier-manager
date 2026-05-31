package pl.polsl.tab.kurier.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PriceDeltaDTOTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void whenValidDTO_thenNoViolations() {
        PriceDeltaDTO dto = createValidDTO();
        Set<ConstraintViolation<PriceDeltaDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid PriceDeltaDTO should have no violations");
    }

    @Test
    public void whenNegativeDeltas_thenViolation() {
        String[] fields = {"weightDelta", "lengthDelta", "widthDelta", "heightDelta", "normalModeDelta", "expressModeDelta"};
        for (String field : fields) {
            PriceDeltaDTO dto = createValidDTO();
            try {
                java.lang.reflect.Field f = dto.getClass().getDeclaredField(field);
                f.setAccessible(true);
                f.set(dto, new BigDecimal("-0.01"));
                assertFalse(validator.validate(dto).isEmpty(), field + " being negative should be invalid");
                
                f.set(dto, new BigDecimal("0.0"));
                assertTrue(validator.validate(dto).isEmpty(), field + " being zero should be valid");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void whenMandatoryFieldsNull_thenViolation() {
        String[] fields = {"weightDelta", "lengthDelta", "widthDelta", "heightDelta", "normalModeDelta", "expressModeDelta"};
        for (String field : fields) {
            PriceDeltaDTO dto = createValidDTO();
            try {
                java.lang.reflect.Field f = dto.getClass().getDeclaredField(field);
                f.setAccessible(true);
                f.set(dto, null);
                assertFalse(validator.validate(dto).isEmpty(), field + " being null should be invalid");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private PriceDeltaDTO createValidDTO() {
        PriceDeltaDTO dto = new PriceDeltaDTO();
        dto.setWeightDelta(new BigDecimal("1.0"));
        dto.setLengthDelta(new BigDecimal("0.1"));
        dto.setWidthDelta(new BigDecimal("0.1"));
        dto.setHeightDelta(new BigDecimal("0.1"));
        dto.setNormalModeDelta(new BigDecimal("15.0"));
        dto.setExpressModeDelta(new BigDecimal("25.0"));
        return dto;
    }
}
