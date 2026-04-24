package pl.polsl.tab.kurier.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.polsl.tab.kurier.dto.StatusDTO;
import pl.polsl.tab.kurier.service.StatusService;

import java.util.List;

@RestController
@RequestMapping("/api/statuses")
public class StatusController {

    @Autowired
    private StatusService statusService;

    @GetMapping
    public ResponseEntity<List<StatusDTO>> getAllStatuses() {
        return ResponseEntity.ok(statusService.getAllStatuses());
    }
}
