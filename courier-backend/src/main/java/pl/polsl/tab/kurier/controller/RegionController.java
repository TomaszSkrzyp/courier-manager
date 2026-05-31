package pl.polsl.tab.kurier.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.polsl.tab.kurier.dto.RegionDTO;
import pl.polsl.tab.kurier.service.RegionService;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    @Autowired
    private RegionService regionService;

    @GetMapping
    public List<RegionDTO> getAllRegions() {
        return regionService.getAllRegions();
    }

    @GetMapping("/active")
    public List<RegionDTO> getActiveRegions() {
        return regionService.getRegionsWithCouriers();
    }

    @PostMapping
    public RegionDTO createRegion(@jakarta.validation.Valid @RequestBody RegionDTO dto) {
        return regionService.createRegion(dto);
    }

    @PutMapping("/{id}")
    public RegionDTO updateRegion(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody RegionDTO dto) {
        return regionService.updateRegion(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteRegion(@PathVariable Integer id) {
        regionService.deleteRegion(id);
    }
}
