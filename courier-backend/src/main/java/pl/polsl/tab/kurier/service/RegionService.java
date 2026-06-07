package pl.polsl.tab.kurier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.polsl.tab.kurier.dto.RegionDTO;
import pl.polsl.tab.kurier.exception.ResourceBusyException;
import pl.polsl.tab.kurier.model.Region;
import pl.polsl.tab.kurier.repository.AddressRepository;
import pl.polsl.tab.kurier.repository.ParcelRepository;
import pl.polsl.tab.kurier.repository.RegionRepository;
import pl.polsl.tab.kurier.repository.EmployeeRepository;
import pl.polsl.tab.kurier.repository.DeliveryUpdateRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegionService {

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ParcelRepository parcelRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DeliveryUpdateRepository deliveryUpdateRepository;

    public List<RegionDTO> getAllRegions() {
        return regionRepository.findAll().stream()
                .map(RegionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<RegionDTO> getRegionsWithCouriers() {
        return regionRepository.findRegionsWithCouriers().stream()
                .map(RegionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public java.util.Optional<Region> getRegionByName(String name) {
        return regionRepository.findByName(name);
    }

    public RegionDTO createRegion(RegionDTO dto) {
        Region region = new Region();
        region.setName(dto.getName());
        Region saved = regionRepository.save(region);
        return RegionDTO.fromEntity(saved);
    }

    public RegionDTO updateRegion(Integer id, RegionDTO dto) {
        Region region = regionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Region not found"));
        region.setName(dto.getName());
        Region saved = regionRepository.save(region);
        return RegionDTO.fromEntity(saved);
    }

    public void deleteRegion(Integer id) {
        String errorMessage = "Region is in use (cannot delete due to active parcels or couriers).";
        
        if (addressRepository.countByRegionRegionId(id) > 0) {
            throw new ResourceBusyException(errorMessage);
        }
        if (parcelRepository.countByCurrentRegionRegionId(id) > 0 || parcelRepository.countByNextRegionRegionId(id) > 0) {
            throw new ResourceBusyException(errorMessage);
        }
        if (employeeRepository.countByRegionsRegionId(id) > 0) {
            throw new ResourceBusyException(errorMessage);
        }
        if (deliveryUpdateRepository.countByRegionRegionId(id) > 0) {
            throw new ResourceBusyException(errorMessage);
        }
        
        regionRepository.deleteById(id);
    }
}
