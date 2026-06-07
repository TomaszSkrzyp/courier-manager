package pl.polsl.tab.kurier.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import pl.polsl.tab.kurier.repository.ParcelRepository;
import pl.polsl.tab.kurier.repository.DeliveryUpdateRepository;
import pl.polsl.tab.kurier.dto.RegionStatsDTO;
import pl.polsl.tab.kurier.dto.DeliveryModeStatsDTO;
import pl.polsl.tab.kurier.dto.CourierStatsDTO;

import pl.polsl.tab.kurier.repository.RegionRepository;
import pl.polsl.tab.kurier.model.Region;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ReportService {

    private final ParcelRepository parcelRepository;
    private final DeliveryUpdateRepository deliveryUpdateRepository;
    private final RegionRepository regionRepository;

    public ReportService(ParcelRepository parcelRepository, 
                         DeliveryUpdateRepository deliveryUpdateRepository,
                         RegionRepository regionRepository) {
        this.parcelRepository = parcelRepository;
        this.deliveryUpdateRepository = deliveryUpdateRepository;
        this.regionRepository = regionRepository;
    }

    public byte[] generateParcelReport(LocalDateTime startDate, LocalDateTime endDate, Integer regionId) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Statistics Report", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDate = LocalDateTime.now().format(formatter);
            Paragraph genDate = new Paragraph("Generated at: " + formattedDate, dateFont);
            genDate.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(genDate);

            Paragraph rangePara = new Paragraph(String.format("Report Period: %s to %s", 
                startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), 
                endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))), dateFont);
            rangePara.setAlignment(Paragraph.ALIGN_RIGHT);
            rangePara.setSpacingAfter(20);
            document.add(rangePara);

            if (regionId != null) {
                String regionName = regionRepository.findById(regionId)
                        .map(Region::getName)
                        .orElse("Unknown Region");
                
                Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
                Paragraph regionHeader = new Paragraph("SUMMARY FOR REGION: " + regionName.toUpperCase(), subTitleFont);
                regionHeader.setSpacingBefore(10);
                regionHeader.setSpacingAfter(10);
                document.add(regionHeader);
                
                // When filtered, show summary instead of single-row tables
                long sentCount = parcelRepository.countParcelsBySourceRegion(startDate, endDate, regionId).stream().mapToLong(RegionStatsDTO::getParcelCount).sum();
                long receivedCount = parcelRepository.countParcelsByDestinationRegion(startDate, endDate, regionId).stream().mapToLong(RegionStatsDTO::getParcelCount).sum();
                long deliveredCount = deliveryUpdateRepository.countDeliveredParcelsByRegion(startDate, endDate, regionId).stream().mapToLong(RegionStatsDTO::getParcelCount).sum();

                Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
                document.add(new Paragraph("• Number of parcels sent from this region: " + sentCount, normalFont));
                document.add(new Paragraph("• Number of parcels addressed to this region: " + receivedCount, normalFont));
                document.add(new Paragraph("• Number of parcels successfully delivered in this region: " + deliveredCount, normalFont));
            }

            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Distribution tables
            
            // Parcels by Delivery Mode
            addSectionTitle(document, "Delivery Mode Popularity");
            List<DeliveryModeStatsDTO> modeStats = parcelRepository.countParcelsByDeliveryMode(startDate, endDate, regionId);
            PdfPTable modeTable = createTable(new String[]{"Delivery Mode", "Parcel Count"}, new float[]{3f, 1f});
            for (DeliveryModeStatsDTO stat : modeStats) {
                modeTable.addCell(new Phrase(stat.getModeName(), cellFont));
                modeTable.addCell(new Phrase(String.valueOf(stat.getParcelCount()), cellFont));
            }
            document.add(modeTable);

            // Delivered Parcels by Courier
            addSectionTitle(document, "Courier Performance Ranking");
            List<CourierStatsDTO> courierStats = deliveryUpdateRepository.countDeliveredParcelsByCourier(startDate, endDate, regionId);
            PdfPTable courierTable = createTable(new String[]{"First Name", "Last Name", "Delivered Count"}, new float[]{2f, 2f, 1f});
            for (CourierStatsDTO stat : courierStats) {
                courierTable.addCell(new Phrase(stat.getFirstName(), cellFont));
                courierTable.addCell(new Phrase(stat.getLastName(), cellFont));
                courierTable.addCell(new Phrase(String.valueOf(stat.getParcelCount()), cellFont));
            }
            document.add(courierTable);

            if (regionId == null) {
                // Show these tables only in Global Report
                addSectionTitle(document, "Shipping Volume by Region");
                List<RegionStatsDTO> sourceRegionStats = parcelRepository.countParcelsBySourceRegion(startDate, endDate, null);
                PdfPTable sourceRegionTable = createTable(new String[]{"Region Name", "Sent Count"}, new float[]{3f, 1f});
                for (RegionStatsDTO stat : sourceRegionStats) {
                    sourceRegionTable.addCell(new Phrase(stat.getRegionName(), cellFont));
                    sourceRegionTable.addCell(new Phrase(String.valueOf(stat.getParcelCount()), cellFont));
                }
                document.add(sourceRegionTable);

                addSectionTitle(document, "Delivery Volume by Region");
                List<RegionStatsDTO> deliveredRegionStats = deliveryUpdateRepository.countDeliveredParcelsByRegion(startDate, endDate, null);
                PdfPTable deliveredRegionTable = createTable(new String[]{"Region Name", "Delivered Count"}, new float[]{3f, 1f});
                for (RegionStatsDTO stat : deliveredRegionStats) {
                    deliveredRegionTable.addCell(new Phrase(stat.getRegionName(), cellFont));
                    deliveredRegionTable.addCell(new Phrase(String.valueOf(stat.getParcelCount()), cellFont));
                }
                document.add(deliveredRegionTable);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF report", e);
        }
    }

    private void addSectionTitle(Document document, String titleStr) throws Exception {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph paragraph = new Paragraph(titleStr, sectionFont);
        paragraph.setSpacingBefore(15);
        paragraph.setSpacingAfter(10);
        document.add(paragraph);
    }

    private PdfPTable createTable(String[] headers, float[] widths) throws Exception {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell();
            cell.setPhrase(new Phrase(header, headerFont));
            cell.setPadding(5);
            cell.setBackgroundColor(new Color(240, 240, 240));
            table.addCell(cell);
        }
        return table;
    }
}
