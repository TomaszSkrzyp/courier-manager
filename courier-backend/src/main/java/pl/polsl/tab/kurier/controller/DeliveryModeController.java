package pl.polsl.tab.kurier.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.polsl.tab.kurier.dto.DeliveryModeDTO;
import pl.polsl.tab.kurier.service.DeliveryModeService;

import java.util.List;

@RestController
@RequestMapping("/api/delivery-modes")
public class DeliveryModeController {

    @Autowired
    private DeliveryModeService deliveryModeService;

    @GetMapping
    public ResponseEntity<List<DeliveryModeDTO>> getAllDeliveryModes() {
        return ResponseEntity.ok(deliveryModeService.getAllDeliveryModes());
    }
}
