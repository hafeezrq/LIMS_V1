package com.qdc.lims.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.qdc.lims.entity.*;
import com.qdc.lims.repository.LabOrderRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Color;

/**
 * Service for generating PDF lab reports for orders.
 */
@Service
public class ReportService {

    private final LabOrderRepository orderRepo;

    /**
     * Constructs a ReportService with the specified LabOrderRepository.
     *
     * @param orderRepo repository for lab orders
     */
    public ReportService(LabOrderRepository orderRepo) {
        this.orderRepo = orderRepo;
    }

    /**
     * Generates a PDF report for the specified lab order ID.
     *
     * @param orderId the ID of the lab order
     * @return a byte array containing the PDF report
     */
    public byte[] generatePdfReport(Long orderId) {
        LabOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        Patient patient = order.getPatient();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // 1. Header (Lab Name)
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLUE);
            Paragraph title = new Paragraph("QDC-LIMS PATHOLOGY LAB", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n")); // Space

            // 2. Patient Details
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Patient Name: " + patient.getFullName(), normalFont));
            document.add(new Paragraph("MRN: " + patient.getMrn(), normalFont));
            document.add(new Paragraph("Date: " + order.getOrderDate().toLocalDate(), normalFont));
            document.add(new Paragraph("\n"));

            // 3. Results Table
            PdfPTable table = new PdfPTable(4); // 4 Columns
            table.setWidthPercentage(100);
            table.setWidths(new float[] { 3, 2, 2, 2 }); // Column widths

            // Table Headers
            addCell(table, "Test Name", true);
            addCell(table, "Result", true);
            addCell(table, "Unit", true);
            addCell(table, "Ref. Range", true);

            // Table Data
            for (LabResult result : order.getResults()) {
                addCell(table, result.getTestDefinition().getTestName(), false);

                // Logic: Make Abnormal Results RED
                Font resultFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
                if (result.isAbnormal()) {
                    resultFont.setColor(Color.RED);
                    resultFont.setStyle(Font.BOLD);
                }
                PdfPCell valueCell = new PdfPCell(new Phrase(result.getResultValue(), resultFont));
                valueCell.setPadding(5);
                table.addCell(valueCell);

                addCell(table, result.getTestDefinition().getUnit(), false);

                String range = result.getTestDefinition().getMinRange() + " - "
                        + result.getTestDefinition().getMaxRange();
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
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    /**
     * Adds a cell to the PDF table with specified text and style.
     *
     * @param table the PDF table to add the cell to
     * @param text the text content of the cell
     * @param isHeader true if the cell is a header cell, false otherwise
     */
    private void addCell(PdfPTable table, String text, boolean isHeader) {
        Font font = isHeader ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)
                : FontFactory.getFont(FontFactory.HELVETICA, 12);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(Color.DARK_GRAY);
        }
        table.addCell(cell);
    }
}