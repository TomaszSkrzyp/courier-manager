package pl.polsl.tab.kurier.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.polsl.tab.kurier.dto.PriceDeltaDTO;
import pl.polsl.tab.kurier.service.PriceDeltaService;

import java.util.List;

@RestController
@RequestMapping("/api/price-deltas")
public class PriceDeltaController {

    @Autowired
    private PriceDeltaService priceDeltaService;

    @GetMapping
    public ResponseEntity<List<PriceDeltaDTO>> getAllPriceDeltas() {
        return ResponseEntity.ok(priceDeltaService.getAllPriceDeltas());
    }

    @GetMapping("/latest")
    public ResponseEntity<PriceDeltaDTO> getLatestPriceDelta() {
        return priceDeltaService.getLatestPriceDelta()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PriceDeltaDTO> createPriceDelta(@RequestBody PriceDeltaDTO dto) {
        return ResponseEntity.status(201).body(priceDeltaService.createPriceDelta(dto));
    }
}
