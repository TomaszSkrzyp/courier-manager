package pl.polsl.tab.kurier.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.polsl.tab.kurier.service.ReportService;
import pl.polsl.tab.kurier.repository.RegionRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final RegionRepository regionRepository;

    public ReportController(ReportService reportService, RegionRepository regionRepository) {
        this.reportService = reportService;
        this.regionRepository = regionRepository;
    }

    @GetMapping(value = "/parcels", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getParcelsReport(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(value = "regionId", required = false) Integer regionId) {
        try {
            byte[] pdfBytes = reportService.generateParcelReport(startDate, endDate, regionId);

            String regionName = "ALL";
            if (regionId != null) {
                regionName = regionRepository.findById(regionId)
                        .map(r -> r.getName().replaceAll("\\s+", "_"))
                        .orElse("Unknown");
            }

            DateTimeFormatter fileDateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String filename = String.format("raport_%s_%s_%s.pdf", 
                regionName, 
                startDate.format(fileDateFormatter), 
                endDate.format(fileDateFormatter));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
