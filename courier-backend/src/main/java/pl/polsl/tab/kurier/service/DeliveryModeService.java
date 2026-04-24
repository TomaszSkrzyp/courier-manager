package pl.polsl.tab.kurier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.polsl.tab.kurier.dto.DeliveryModeDTO;
import pl.polsl.tab.kurier.repository.DeliveryModeRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeliveryModeService {

    @Autowired
    private DeliveryModeRepository deliveryModeRepository;

    public List<DeliveryModeDTO> getAllDeliveryModes() {
        return deliveryModeRepository.findAll().stream()
                .map(DeliveryModeDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
