package pl.polsl.tab.kurier.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.polsl.tab.kurier.dto.ParcelCreateDTO;
import pl.polsl.tab.kurier.dto.ParcelDTO;
import pl.polsl.tab.kurier.service.ParcelService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parcels")
public class ParcelController {

    @Autowired
    private ParcelService parcelService;

    @GetMapping
    public List<ParcelDTO> getAllParcels() {
        return parcelService.getAllParcels();
    }

    @PostMapping
    public ResponseEntity<?> createParcel(@Valid @RequestBody ParcelCreateDTO dto) {
        try {
            ParcelDTO created = parcelService.createParcel(dto);
            return ResponseEntity.status(201).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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

    /**
     * Courier calls this endpoint to confirm delivery of the parcel to the
     * current nextRegion. The system automatically:
     *   - Marks as DELIVERED if nextRegion == destinationRegion
     *   - Computes the next hop (AT_HUB or OUT_FOR_DELIVERY) otherwise
     *
     * Body: { "employeeId": <courierId> }
     */
    @PatchMapping("/{id}/advance")
    public ResponseEntity<ParcelDTO> advanceParcel(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Integer employeeId = body.containsKey("employeeId") ? Integer.parseInt(body.get("employeeId")) : null;
        if (employeeId == null) {
            return ResponseEntity.badRequest().build();
        }
        return parcelService.advanceParcel(id, employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}