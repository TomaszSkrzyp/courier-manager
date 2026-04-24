package pl.polsl.tab.kurier.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.polsl.tab.kurier.dto.StatusDTO;
import pl.polsl.tab.kurier.repository.StatusRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatusService {

    @Autowired
    private StatusRepository statusRepository;

    public List<StatusDTO> getAllStatuses() {
        return statusRepository.findAll().stream()
                .map(StatusDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
