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

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private final ParcelRepository parcelRepository;
    private final DeliveryUpdateRepository deliveryUpdateRepository;

    public ReportService(ParcelRepository parcelRepository, DeliveryUpdateRepository deliveryUpdateRepository) {
        this.parcelRepository = parcelRepository;
        this.deliveryUpdateRepository = deliveryUpdateRepository;
    }

    public byte[] generateParcelReport() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Detailed Statistics Report", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);

            Font dateFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            String formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Paragraph datePara = new Paragraph("Generated at: " + formattedDate, dateFont);
            datePara.setAlignment(Paragraph.ALIGN_RIGHT);
            datePara.setSpacingAfter(20);
            document.add(datePara);

            // 1. Parcels by Region
            addSectionTitle(document, "Parcels by Destination Region");
            List<RegionStatsDTO> regionStats = parcelRepository.countParcelsByDestinationRegion();
            PdfPTable regionTable = createTable(new String[]{"Region Name", "Parcel Count"}, new float[]{3f, 1f});
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            for (RegionStatsDTO stat : regionStats) {
                regionTable.addCell(new Phrase(stat.getRegionName(), cellFont));
                regionTable.addCell(new Phrase(String.valueOf(stat.getParcelCount()), cellFont));
            }
            document.add(regionTable);

            // 2. Parcels by Delivery Mode
            addSectionTitle(document, "Parcels by Delivery Mode");
            List<DeliveryModeStatsDTO> modeStats = parcelRepository.countParcelsByDeliveryMode();
            PdfPTable modeTable = createTable(new String[]{"Delivery Mode", "Parcel Count"}, new float[]{3f, 1f});
            for (DeliveryModeStatsDTO stat : modeStats) {
                modeTable.addCell(new Phrase(stat.getModeName(), cellFont));
                modeTable.addCell(new Phrase(String.valueOf(stat.getParcelCount()), cellFont));
            }
            document.add(modeTable);

            // 3. Delivered Parcels by Courier
            addSectionTitle(document, "Delivered Parcels by Courier");
            List<CourierStatsDTO> courierStats = deliveryUpdateRepository.countDeliveredParcelsByCourier();
            PdfPTable courierTable = createTable(new String[]{"First Name", "Last Name", "Delivered Count"}, new float[]{2f, 2f, 1f});
            for (CourierStatsDTO stat : courierStats) {
                courierTable.addCell(new Phrase(stat.getFirstName(), cellFont));
                courierTable.addCell(new Phrase(stat.getLastName(), cellFont));
                courierTable.addCell(new Phrase(String.valueOf(stat.getParcelCount()), cellFont));
            }
            document.add(courierTable);

            // 4. Delivered Parcels by Region
            addSectionTitle(document, "Delivered Parcels by Region");
            List<RegionStatsDTO> deliveredRegionStats = deliveryUpdateRepository.countDeliveredParcelsByRegion();
            PdfPTable deliveredRegionTable = createTable(new String[]{"Region Name", "Delivered Count"}, new float[]{3f, 1f});
            for (RegionStatsDTO stat : deliveredRegionStats) {
                deliveredRegionTable.addCell(new Phrase(stat.getRegionName(), cellFont));
                deliveredRegionTable.addCell(new Phrase(String.valueOf(stat.getParcelCount()), cellFont));
            }
            document.add(deliveredRegionTable);

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
