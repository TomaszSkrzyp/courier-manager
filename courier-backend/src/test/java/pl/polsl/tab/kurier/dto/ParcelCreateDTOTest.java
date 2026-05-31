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
        assertTrue(violations.isEmpty(), "Valid DTO should have no violations");
    }

    @Test
    public void whenWeightAtBoundaries_thenNoViolations() {
        ParcelCreateDTO dto = createValidDTO();
        dto.setWeight(0.1);
        assertTrue(validator.validate(dto).isEmpty(), "Weight 0.1 should be valid");
        
        dto.setWeight(100.0);
        assertTrue(validator.validate(dto).isEmpty(), "Weight 100.0 should be valid");
    }

    @Test
    public void whenWeightOutOfBounds_thenViolation() {
        ParcelCreateDTO dto = createValidDTO();
        dto.setWeight(0.09);
        assertFalse(validator.validate(dto).isEmpty(), "Weight < 0.1 should be invalid");
        
        dto.setWeight(100.1);
        assertFalse(validator.validate(dto).isEmpty(), "Weight > 100 should be invalid");
    }

    @Test
    public void whenDimensionsAtBoundaries_thenNoViolations() {
        ParcelCreateDTO dto = createValidDTO();
        dto.setHeight(1.0);
        dto.setWidth(1.0);
        dto.setLength(1.0);
        assertTrue(validator.validate(dto).isEmpty(), "Dimensions of 1.0 should be valid");
        
        dto.setHeight(200.0);
        dto.setWidth(200.0);
        dto.setLength(200.0);
        assertTrue(validator.validate(dto).isEmpty(), "Dimensions of 200.0 should be valid");
    }

    @Test
    public void whenDimensionsOutOfBounds_thenViolation() {
        ParcelCreateDTO dto = createValidDTO();
        dto.setHeight(0.9);
        assertFalse(validator.validate(dto).isEmpty(), "Height < 1.0 should be invalid");
        
        dto.setHeight(200.1);
        assertFalse(validator.validate(dto).isEmpty(), "Height > 200 should be invalid");
    }

    @Test
    public void whenMandatoryFieldsBlank_thenViolation() {
        ParcelCreateDTO dto = createValidDTO();
        
        String[] fields = {"senderStreet", "senderBuildingNumber", "senderPostalCode", 
                          "recipientStreet", "recipientBuildingNumber", "recipientPostalCode"};
        
        for (String field : fields) {
            ParcelCreateDTO testDto = createValidDTO();
            try {
                java.lang.reflect.Field f = testDto.getClass().getDeclaredField(field);
                f.setAccessible(true);
                f.set(testDto, "");
                assertFalse(validator.validate(testDto).isEmpty(), field + " being blank should be invalid");
                
                f.set(testDto, "   ");
                assertFalse(validator.validate(testDto).isEmpty(), field + " being only spaces should be invalid");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void whenMandatoryIdsNull_thenViolation() {
        ParcelCreateDTO dto = createValidDTO();
        dto.setSenderRegionId(null);
        assertFalse(validator.validate(dto).isEmpty(), "SenderRegionId being null should be invalid");
        
        dto = createValidDTO();
        dto.setRecipientRegionId(null);
        assertFalse(validator.validate(dto).isEmpty(), "RecipientRegionId being null should be invalid");
        
        dto = createValidDTO();
        dto.setDeliveryModeId(null);
        assertFalse(validator.validate(dto).isEmpty(), "DeliveryModeId being null should be invalid");
    }

    @Test
    public void whenInvalidPostalCode_thenViolation() {
        ParcelCreateDTO dto = createValidDTO();
        String[] invalidCodes = {"00000", "00-0000", "0-000", "AA-000", "00-00A", ""};
        for (String code : invalidCodes) {
            dto.setSenderPostalCode(code);
            assertFalse(validator.validate(dto).isEmpty(), "Postal code '" + code + "' should be invalid");
        }
    }

    @Test
    public void whenInvalidPhoneNumber_thenViolation() {
        ParcelCreateDTO dto = createValidDTO();
        String[] invalidPhones = {"123456", "123abc456", "+48-123-456", "123 456 789 012 345 6", ""};
        for (String phone : invalidPhones) {
            dto.setPhoneNumber(phone);
            assertFalse(validator.validate(dto).isEmpty(), "Phone number '" + phone + "' should be invalid");
        }
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
