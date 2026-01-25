package com.qdc.lims.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.qdc.lims.entity.LabOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Service
public class ReportExportService {

    @Autowired
    private ConfigService configService;
    @Autowired
    private BrandingService brandingService;
    @Autowired
    private LocaleFormatService localeFormatService;

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.ITALIC);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font DATA_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    /**
     * Export a list of LabOrders to a formatted PDF report.
     */
    public void exportDailyRevenuePdf(List<LabOrder> orders, String dateStr, File destination) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(destination));
        document.open();

        // 1. Header (Clinic Info from ConfigService)
        addHeader(document, "Daily Revenue Report");

        document.add(new Paragraph("Date: " + dateStr, SUBTITLE_FONT));
        document.add(new Paragraph(" ")); // Spacer

        // 2. Table
        PdfPTable table = new PdfPTable(6); // Columns: ID, Patient, Doctor, Total, Paid, Due
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 1, 3, 3, 2, 2, 2 });

        // Headers
        addCell(table, "Order ID", HEADER_FONT, true);
        addCell(table, "Patient", HEADER_FONT, true);
        addCell(table, "Doctor", HEADER_FONT, true);
        addCell(table, "Total", HEADER_FONT, true);
        addCell(table, "Paid", HEADER_FONT, true);
        addCell(table, "Balance", HEADER_FONT, true);

        // Data
        double grandTotal = 0;
        double totalPaid = 0;

        for (LabOrder order : orders) {
            addCell(table, order.getId().toString(), DATA_FONT, false);
            addCell(table, order.getPatient().getFullName(), DATA_FONT, false);
            addCell(table, order.getReferringDoctor() != null ? order.getReferringDoctor().getName() : "-", DATA_FONT,
                    false);
            addCell(table,
                    localeFormatService.formatCurrency(order.getTotalAmount() != null ? order.getTotalAmount() : 0.0),
                    DATA_FONT, false);
            addCell(table,
                    localeFormatService.formatCurrency(order.getPaidAmount() != null ? order.getPaidAmount() : 0.0),
                    DATA_FONT, false);
            addCell(table,
                    localeFormatService.formatCurrency(order.getBalanceDue() != null ? order.getBalanceDue() : 0.0),
                    DATA_FONT, false);

            grandTotal += (order.getTotalAmount() != null ? order.getTotalAmount() : 0);
            totalPaid += (order.getPaidAmount() != null ? order.getPaidAmount() : 0);
        }

        document.add(table);

        // 3. Summary
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Total Revenue Generated: " + localeFormatService.formatCurrency(grandTotal),
                HEADER_FONT));
        document.add(new Paragraph(
                "Total Cash Collected: " + localeFormatService.formatCurrency(totalPaid),
                HEADER_FONT));

        // 4. Footer
        addFooter(document);

        document.close();
    }

    /**
     * Export data to CSV format.
     */
    public void exportDailyRevenueCsv(List<LabOrder> orders, File destination) throws IOException {
        try (FileWriter writer = new FileWriter(destination)) {
            // Header
            writer.write("Order ID,Date/Time,Patient Name,Doctor,Total Amount,Paid Amount,Balance Due\n");

            for (LabOrder order : orders) {
                writer.write(String.format("%d,%s,\"%s\",\"%s\",%s,%s,%s\n",
                        order.getId(),
                        localeFormatService.formatDateTime(order.getOrderDate()),
                        order.getPatient().getFullName(),
                        order.getReferringDoctor() != null ? order.getReferringDoctor().getName() : "",
                        localeFormatService.formatNumber(order.getTotalAmount() != null ? order.getTotalAmount() : 0.0),
                        localeFormatService.formatNumber(order.getPaidAmount() != null ? order.getPaidAmount() : 0.0),
                        localeFormatService.formatNumber(order.getBalanceDue() != null ? order.getBalanceDue() : 0.0)));
            }
        }
    }

    private void addHeader(Document doc, String reportTitle) throws DocumentException {
        // Clinic Name
        Paragraph title = new Paragraph(brandingService.getLabNameOrAppName(), TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        String address = brandingService.getClinicAddress();
        if (!address.isEmpty()) {
            Paragraph addr = new Paragraph(address, DATA_FONT);
            addr.setAlignment(Element.ALIGN_CENTER);
            doc.add(addr);
        }

        String phone = brandingService.getClinicPhone();
        String email = brandingService.getClinicEmail();
        if (!phone.isBlank() || !email.isBlank()) {
            String contactLine = phone.isBlank() || email.isBlank() ? phone + email : phone + " | " + email;
            Paragraph contact = new Paragraph(contactLine.trim(), DATA_FONT);
            contact.setAlignment(Element.ALIGN_CENTER);
            doc.add(contact);
        }

        doc.add(new Paragraph(" "));
        Paragraph reportName = new Paragraph(reportTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14));
        reportName.setAlignment(Element.ALIGN_CENTER);
        doc.add(reportName);
        doc.add(new Paragraph(" "));
    }

    private void addFooter(Document doc) throws DocumentException {
        String footerText = brandingService.getReportFooterText();
        Paragraph footer = new Paragraph(footerText, FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20);
        doc.add(footer);
    }

    private void addCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
        }
        table.addCell(cell);
    }
}
