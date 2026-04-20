package pl.polsl.tab.kurier.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.polsl.tab.kurier.model.Parcel;
import pl.polsl.tab.kurier.repository.ParcelRepository;

import java.util.List;

@RestController
@RequestMapping("/api/parcels")
@CrossOrigin(origins = "http://localhost:5173") 
public class ParcelController {

    @Autowired
    private ParcelRepository parcelRepository;

    @GetMapping
    public List<Parcel> getAllParcels() {
        return parcelRepository.findAll();
    }
    @GetMapping("/track/{number}")
    public ResponseEntity<Parcel> getParcelByNumber(@PathVariable String number) {
        return parcelRepository.findByTrackingNumber(number)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}