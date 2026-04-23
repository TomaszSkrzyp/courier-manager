package pl.polsl.tab.kurier.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.polsl.tab.kurier.dto.ParcelDTO;
import pl.polsl.tab.kurier.service.ParcelService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parcels")
@CrossOrigin(origins = "http://localhost:5173") 
public class ParcelController {

    @Autowired
    private ParcelService parcelService;

    @GetMapping
    public List<ParcelDTO> getAllParcels() {
        return parcelService.getAllParcels();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParcelDTO> getParcelById(@PathVariable Integer id) {
        return parcelService.getParcelById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<ParcelDTO> getParcelByTrackingNumber(@PathVariable String trackingNumber) {
        return parcelService.getParcelByTrackingNumber(trackingNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/courier/{courierId}")
    public ResponseEntity<List<ParcelDTO>> getParcelsForCourier(@PathVariable Integer courierId) {
        return ResponseEntity.ok(parcelService.getParcelsForCourier(courierId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ParcelDTO> updateParcelStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        String comment = body.get("comment");
        Integer employeeId = body.containsKey("employeeId") ? Integer.parseInt(body.get("employeeId")) : 1;
        
        return parcelService.updateParcelStatus(id, newStatus, comment, employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<ParcelDTO> verifyParcel(@PathVariable Integer id, @RequestBody Map<String, Boolean> body) {
        Boolean verified = body.get("verified");
        if (verified == null) {
            return ResponseEntity.badRequest().build();
        }
        
        return parcelService.verifyParcel(id, verified)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}