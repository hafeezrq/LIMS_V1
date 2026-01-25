package com.qdc.lims.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.qdc.lims.entity.*;
import com.qdc.lims.repository.LabOrderRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

/**
 * Generates printable PDF lab reports for completed orders.
 */
@Service
public class ReportService {

    private final LabOrderRepository orderRepo;

    /**
     * Creates the report service.
     *
     * @param orderRepo lab order repository
     */
    public ReportService(LabOrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    /**
     * Builds a PDF report for the given order id.
     *
     * @param orderId lab order id
     * @return PDF document bytes
     */
    public byte[] generatePdfReport(Long orderId) {
        LabOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Patient patient = order.getPatient();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // 1. Header
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLUE);
            Paragraph title = new Paragraph("QDC-LIMS PATHOLOGY LAB", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // 2. Patient Details
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Patient Name: " + patient.getFullName(), normalFont));
            document.add(new Paragraph("MRN: " + patient.getMrn(), normalFont));
            document.add(new Paragraph("Date: " + order.getOrderDate().toLocalDate(), normalFont));
            document.add(new Paragraph("\n"));

            // 3. Results Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 2, 2, 2});

            addCell(table, "Test Name", true);
            addCell(table, "Result", true);
            addCell(table, "Unit", true);
            addCell(table, "Ref. Range", true);

            // Table Data
            for (LabResult result : order.getResults()) {
                // Skip results that are not yet entered.
                if (result.getResultValue() == null || result.getResultValue().trim().isEmpty()) {
                    continue;
                }

                addCell(table, result.getTestDefinition().getTestName(), false);

                Font resultFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
                if (result.isAbnormal()) {
                    resultFont.setColor(Color.RED);
                    resultFont.setStyle(Font.BOLD);
                }

                PdfPCell valueCell = new PdfPCell(new Phrase(result.getResultValue(), resultFont));
                valueCell.setPadding(5);
                table.addCell(valueCell);

                String unit = result.getTestDefinition().getUnit();
                addCell(table, unit != null ? unit : "", false);

                BigDecimal min = result.getTestDefinition().getMinRange();
                BigDecimal max = result.getTestDefinition().getMaxRange();

                String range;
                if (min != null && max != null) {
                    range = min + " - " + max;
                } else {
                    range = "";
                }
                addCell(table, range, false);
            }

            document.add(table);

            // 4. Footer
            document.add(new Paragraph("\n\n"));
            Paragraph footer = new Paragraph("*** End of Report ***",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    /**
     * Adds a formatted cell to the report table.
     *
     * @param table    target table
     * @param text     cell text (null-safe)
     * @param isHeader whether the cell is a header cell
     */
    private void addCell(PdfPTable table, String text, boolean isHeader) {
        String safeText = (text != null) ? text : "";

        Font font = isHeader ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)
                : FontFactory.getFont(FontFactory.HELVETICA, 12);

        PdfPCell cell = new PdfPCell(new Phrase(safeText, font));
        cell.setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(Color.DARK_GRAY);
        }
        table.addCell(cell);
    }
}
