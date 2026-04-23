package pl.polsl.tab.kurier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.polsl.tab.kurier.dto.RegionDTO;
import pl.polsl.tab.kurier.model.Region;
import pl.polsl.tab.kurier.repository.RegionRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegionService {

    @Autowired
    private RegionRepository regionRepository;

    public List<RegionDTO> getAllRegions() {
        return regionRepository.findAll().stream()
                .map(RegionDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public Region getOrCreateRegion(String name) {
        return regionRepository.findByName(name)
                .orElseGet(() -> {
                    Region newRegion = new Region();
                    newRegion.setName(name);
                    return regionRepository.save(newRegion);
                });
    }
}
