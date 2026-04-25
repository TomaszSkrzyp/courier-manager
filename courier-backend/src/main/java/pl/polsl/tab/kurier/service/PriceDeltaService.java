package pl.polsl.tab.kurier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.polsl.tab.kurier.dto.PriceDeltaDTO;
import pl.polsl.tab.kurier.model.PriceDelta;
import pl.polsl.tab.kurier.repository.PriceDeltaRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PriceDeltaService {

    @Autowired
    private PriceDeltaRepository priceDeltaRepository;

    public List<PriceDeltaDTO> getAllPriceDeltas() {
        return priceDeltaRepository.findAll().stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .map(PriceDeltaDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<PriceDeltaDTO> getLatestPriceDelta() {
        return priceDeltaRepository.findFirstByOrderByCreatedAtDesc()
                .map(PriceDeltaDTO::fromEntity);
    }

    public PriceDeltaDTO createPriceDelta(PriceDeltaDTO dto) {
        PriceDelta pd = new PriceDelta();
        pd.setWeightDelta(dto.getWeightDelta());
        pd.setLengthDelta(dto.getLengthDelta());
        pd.setWidthDelta(dto.getWidthDelta());
        pd.setHeightDelta(dto.getHeightDelta());
        pd.setModeDelta(dto.getModeDelta());
        
        return PriceDeltaDTO.fromEntity(priceDeltaRepository.save(pd));
    }
}
