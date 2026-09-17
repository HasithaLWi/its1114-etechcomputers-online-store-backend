package lk.ijse.etechbackend.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lk.ijse.etechbackend.dto.analytics.*;
import lk.ijse.etechbackend.service.ReportExportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class ReportExportServiceImpl implements ReportExportService {

    private static final Color COLOR_PRIMARY = new Color(30, 58, 138); // #1e3a8a
    private static final Color COLOR_ACCENT = new Color(37, 99, 235);  // #2563eb
    private static final Color COLOR_BG_HEADER = new Color(241, 245, 249); // #f1f5f9
    private static final Color COLOR_BG_ALT = new Color(248, 250, 252); // #f8fafc
    private static final Color COLOR_BORDER = new Color(226, 232, 240); // #e2e8f0
    private static final Color COLOR_TEXT_DARK = new Color(15, 23, 42); // #0f172a
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139); // #64748b

    @Override
    public byte[] generatePdfReport(
            AnalyticsSummaryDTO summary,
            List<BranchPerformanceDTO> branches,
            List<TopProductDTO> topProducts,
            List<CategoryPerformanceDTO> categories,
            InventoryHealthDTO inventory,
            String dateRangeLabel,
            String branchScopeLabel,
            String generatedBy
    ) {
        log.info("Generating professional executive PDF report");
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, out);
            document.open();

            // 1. Header Banner
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{65f, 35f});

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            Paragraph brandTitle = new Paragraph("ETEC COMPUTERS ONLINE STORE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRIMARY));
            Paragraph docTitle = new Paragraph("Executive Business Intelligence & Performance Report", FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_TEXT_MUTED));
            leftCell.addElement(brandTitle);
            leftCell.addElement(docTitle);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Paragraph genDate = new Paragraph("Generated: " + nowStr, FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_MUTED));
            Paragraph genUser = new Paragraph("Auditor: " + (generatedBy != null ? generatedBy : "System Admin"), FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_MUTED));
            genDate.setAlignment(Element.ALIGN_RIGHT);
            genUser.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(genDate);
            rightCell.addElement(genUser);

            headerTable.addCell(leftCell);
            headerTable.addCell(rightCell);
            document.add(headerTable);

            // Divider line
            document.add(new Paragraph(" "));
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            PdfPCell lineCell = new PdfPCell();
            lineCell.setBackgroundColor(COLOR_ACCENT);
            lineCell.setFixedHeight(2f);
            lineCell.setBorder(Rectangle.NO_BORDER);
            line.addCell(lineCell);
            document.add(line);
            document.add(new Paragraph(" "));

            // 2. Filter Parameters Ribbon
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{50f, 50f});

            PdfPCell pCell1 = createStyledCell("Reporting Period: " + (dateRangeLabel != null ? dateRangeLabel : "All Time"), true, 9);
            PdfPCell pCell2 = createStyledCell("Branch Scope: " + (branchScopeLabel != null ? branchScopeLabel : "All Branches"), true, 9);
            pCell1.setBackgroundColor(COLOR_BG_HEADER);
            pCell2.setBackgroundColor(COLOR_BG_HEADER);
            pCell2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            metaTable.addCell(pCell1);
            metaTable.addCell(pCell2);
            document.add(metaTable);
            document.add(new Paragraph(" "));

            // 3. Executive KPI Summary Cards
            document.add(new Paragraph("1. Executive Financial & Operational KPIs", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY)));
            document.add(new Paragraph(" "));

            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setWidths(new float[]{25f, 25f, 25f, 25f});

            kpiTable.addCell(createKpiCard("Gross Revenue", formatRs(summary.getGrossRevenue()), "Total sales processed"));
            kpiTable.addCell(createKpiCard("Net Revenue", formatRs(summary.getNetRevenue()), "Excluding cancellations"));
            kpiTable.addCell(createKpiCard("Total Orders", String.valueOf(summary.getTotalOrders()), summary.getCompletedOrders() + " delivered"));
            kpiTable.addCell(createKpiCard("Avg Order Value", formatRs(summary.getAvgOrderValue()), "Ticket size"));

            document.add(kpiTable);
            document.add(new Paragraph(" "));

            PdfPTable kpiTable2 = new PdfPTable(3);
            kpiTable2.setWidthPercentage(100);
            kpiTable2.setWidths(new float[]{33f, 33f, 34f});
            kpiTable2.addCell(createKpiCard("Units Sold", String.valueOf(summary.getTotalUnitsSold()), "Hardware items dispatched"));
            kpiTable2.addCell(createKpiCard("Fulfillment Rate", summary.getFulfillmentRate() + "%", "Completed vs Active"));
            kpiTable2.addCell(createKpiCard("Inventory Units", inventory != null ? String.valueOf(inventory.getTotalUnitsInStock()) : "N/A", "Active stock on hand"));
            document.add(kpiTable2);
            document.add(new Paragraph(" "));

            // 4. Regional Branch Performance Matrix
            document.add(new Paragraph("2. Regional Branch Performance Matrix", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY)));
            document.add(new Paragraph(" "));

            PdfPTable branchTable = new PdfPTable(6);
            branchTable.setWidthPercentage(100);
            branchTable.setWidths(new float[]{25f, 15f, 12f, 12f, 22f, 14f});

            branchTable.addCell(createHeaderCell("Branch Name"));
            branchTable.addCell(createHeaderCell("City"));
            branchTable.addCell(createHeaderCell("Orders"));
            branchTable.addCell(createHeaderCell("Delivered"));
            branchTable.addCell(createHeaderCell("Net Revenue"));
            branchTable.addCell(createHeaderCell("Share %"));

            if (branches != null && !branches.isEmpty()) {
                boolean alt = false;
                for (BranchPerformanceDTO b : branches) {
                    Color rowBg = alt ? COLOR_BG_ALT : Color.WHITE;
                    branchTable.addCell(createBodyCell(b.getBranchName(), rowBg, Element.ALIGN_LEFT));
                    branchTable.addCell(createBodyCell(b.getCity(), rowBg, Element.ALIGN_LEFT));
                    branchTable.addCell(createBodyCell(String.valueOf(b.getOrderCount()), rowBg, Element.ALIGN_CENTER));
                    branchTable.addCell(createBodyCell(String.valueOf(b.getCompletedOrders()), rowBg, Element.ALIGN_CENTER));
                    branchTable.addCell(createBodyCell(formatRs(b.getRevenue()), rowBg, Element.ALIGN_RIGHT));
                    branchTable.addCell(createBodyCell((b.getPercentage() != null ? b.getPercentage() : BigDecimal.ZERO) + "%", rowBg, Element.ALIGN_RIGHT));
                    alt = !alt;
                }
            } else {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No branch transactions found for the selected period.", FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_MUTED)));
                emptyCell.setColspan(6);
                emptyCell.setPadding(8);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                branchTable.addCell(emptyCell);
            }

            document.add(branchTable);
            document.add(new Paragraph(" "));

            // 5. Top Performing Hardware Products
            document.add(new Paragraph("3. Top Performing Hardware Catalog (By Revenue & Units)", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY)));
            document.add(new Paragraph(" "));

            PdfPTable prodTable = new PdfPTable(5);
            prodTable.setWidthPercentage(100);
            prodTable.setWidths(new float[]{15f, 35f, 20f, 12f, 18f});

            prodTable.addCell(createHeaderCell("SKU"));
            prodTable.addCell(createHeaderCell("Product Name"));
            prodTable.addCell(createHeaderCell("Category"));
            prodTable.addCell(createHeaderCell("Units Sold"));
            prodTable.addCell(createHeaderCell("Total Revenue"));

            if (topProducts != null && !topProducts.isEmpty()) {
                boolean alt = false;
                for (TopProductDTO p : topProducts) {
                    Color rowBg = alt ? COLOR_BG_ALT : Color.WHITE;
                    prodTable.addCell(createBodyCell(p.getSku() != null ? p.getSku() : "N/A", rowBg, Element.ALIGN_LEFT));
                    prodTable.addCell(createBodyCell(p.getName(), rowBg, Element.ALIGN_LEFT));
                    prodTable.addCell(createBodyCell(p.getCategoryName(), rowBg, Element.ALIGN_LEFT));
                    prodTable.addCell(createBodyCell(String.valueOf(p.getUnitsSold()), rowBg, Element.ALIGN_CENTER));
                    prodTable.addCell(createBodyCell(formatRs(p.getRevenue()), rowBg, Element.ALIGN_RIGHT));
                    alt = !alt;
                }
            } else {
                PdfPCell emptyCell = new PdfPCell(new Phrase("No product sales data recorded.", FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_MUTED)));
                emptyCell.setColspan(5);
                emptyCell.setPadding(8);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                prodTable.addCell(emptyCell);
            }

            document.add(prodTable);
            document.add(new Paragraph(" "));

            // 6. Category Market Share Summary
            if (categories != null && !categories.isEmpty()) {
                document.add(new Paragraph("4. Category Revenue Contribution", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY)));
                document.add(new Paragraph(" "));

                PdfPTable catTable = new PdfPTable(4);
                catTable.setWidthPercentage(100);
                catTable.setWidths(new float[]{35f, 20f, 25f, 20f});

                catTable.addCell(createHeaderCell("Category"));
                catTable.addCell(createHeaderCell("Units Dispatched"));
                catTable.addCell(createHeaderCell("Revenue"));
                catTable.addCell(createHeaderCell("Contribution %"));

                boolean alt = false;
                for (CategoryPerformanceDTO c : categories) {
                    Color rowBg = alt ? COLOR_BG_ALT : Color.WHITE;
                    catTable.addCell(createBodyCell(c.getCategoryName(), rowBg, Element.ALIGN_LEFT));
                    catTable.addCell(createBodyCell(String.valueOf(c.getUnitsSold()), rowBg, Element.ALIGN_CENTER));
                    catTable.addCell(createBodyCell(formatRs(c.getRevenue()), rowBg, Element.ALIGN_RIGHT));
                    catTable.addCell(createBodyCell(c.getPercentage() + "%", rowBg, Element.ALIGN_RIGHT));
                    alt = !alt;
                }
                document.add(catTable);
                document.add(new Paragraph(" "));
            }

            // 7. Footer
            Paragraph footer = new Paragraph(
                    "Confidential Business Intelligence Document — Generated for ETec Computers Executive Board.\n" +
                    "This automated audit report is verified against transactional database ledgers.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7, COLOR_TEXT_MUTED));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF report", e);
            throw new RuntimeException("Failed to generate PDF analytics report: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateExcelReport(
            AnalyticsSummaryDTO summary,
            List<BranchPerformanceDTO> branches,
            List<TopProductDTO> topProducts,
            List<CategoryPerformanceDTO> categories,
            InventoryHealthDTO inventory,
            String dateRangeLabel,
            String branchScopeLabel,
            String generatedBy
    ) {
        log.info("Generating professional executive Excel workbook");
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Cell Styles
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 10);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("Rs #,##0.00"));

            CellStyle integerStyle = workbook.createCellStyle();
            integerStyle.setDataFormat(format.getFormat("#,##0"));

            CellStyle percentStyle = workbook.createCellStyle();
            percentStyle.setDataFormat(format.getFormat("0.0%"));

            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            // ==========================================
            // SHEET 1: Executive Summary
            // ==========================================
            Sheet sheet1 = workbook.createSheet("Executive Overview");
            int r = 0;

            Row titleRow = sheet1.createRow(r++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("ETec Computers — Executive Business Intelligence Report");
            titleCell.setCellStyle(boldStyle);

            Row metaRow1 = sheet1.createRow(r++);
            metaRow1.createCell(0).setCellValue("Reporting Period: " + (dateRangeLabel != null ? dateRangeLabel : "All Time"));
            metaRow1.createCell(2).setCellValue("Branch Scope: " + (branchScopeLabel != null ? branchScopeLabel : "All Branches"));

            Row metaRow2 = sheet1.createRow(r++);
            metaRow2.createCell(0).setCellValue("Generated At: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            metaRow2.createCell(2).setCellValue("Auditor: " + (generatedBy != null ? generatedBy : "System Admin"));

            r++; // blank row

            Row kpiHeaderRow = sheet1.createRow(r++);
            kpiHeaderRow.createCell(0).setCellValue("Metric");
            kpiHeaderRow.createCell(1).setCellValue("Value");
            kpiHeaderRow.createCell(2).setCellValue("Notes");
            for (int i = 0; i <= 2; i++) kpiHeaderRow.getCell(i).setCellStyle(headerStyle);

            addKpiRow(sheet1, r++, "Gross Revenue", summary.getGrossRevenue().doubleValue(), currencyStyle, "Total gross orders");
            addKpiRow(sheet1, r++, "Net Revenue", summary.getNetRevenue().doubleValue(), currencyStyle, "Excluding cancelled orders");
            addKpiRow(sheet1, r++, "Total Orders", (double) summary.getTotalOrders(), integerStyle, "All customer orders");
            addKpiRow(sheet1, r++, "Completed Orders", (double) summary.getCompletedOrders(), integerStyle, "Delivered successfully");
            addKpiRow(sheet1, r++, "Pending Orders", (double) summary.getPendingOrders(), integerStyle, "Awaiting fulfillment");
            addKpiRow(sheet1, r++, "Cancelled Orders", (double) summary.getCancelledOrders(), integerStyle, "Refunded / cancelled");
            addKpiRow(sheet1, r++, "Average Order Value", summary.getAvgOrderValue().doubleValue(), currencyStyle, "Net Revenue / Orders");
            addKpiRow(sheet1, r++, "Total Units Sold", (double) summary.getTotalUnitsSold(), integerStyle, "Hardware units sold");
            addKpiRow(sheet1, r++, "Fulfillment Rate", summary.getFulfillmentRate() / 100.0, percentStyle, "% delivered successfully");

            if (inventory != null) {
                addKpiRow(sheet1, r++, "Total Inventory Units", (double) inventory.getTotalUnitsInStock(), integerStyle, "Stock on hand");
                addKpiRow(sheet1, r++, "Low Stock Alerts", (double) inventory.getLowStockCount(), integerStyle, "At or below safety margin");
                addKpiRow(sheet1, r++, "Out of Stock Products", (double) inventory.getOutOfStockCount(), integerStyle, "Zero units in branch");
            }

            for (int i = 0; i < 4; i++) sheet1.autoSizeColumn(i);

            // ==========================================
            // SHEET 2: Branch Performance
            // ==========================================
            Sheet sheet2 = workbook.createSheet("Branch Performance");
            Row bHeader = sheet2.createRow(0);
            String[] bCols = {"Branch ID", "Branch Name", "City", "Total Orders", "Delivered Orders", "Revenue", "Share %", "Fulfillment Rate"};
            for (int i = 0; i < bCols.length; i++) {
                Cell c = bHeader.createCell(i);
                c.setCellValue(bCols[i]);
                c.setCellStyle(headerStyle);
            }

            int br = 1;
            if (branches != null) {
                for (BranchPerformanceDTO b : branches) {
                    Row row = sheet2.createRow(br++);
                    row.createCell(0).setCellValue(b.getBranchId());
                    row.createCell(1).setCellValue(b.getBranchName());
                    row.createCell(2).setCellValue(b.getCity());

                    Cell cOrders = row.createCell(3);
                    cOrders.setCellValue(b.getOrderCount());
                    cOrders.setCellStyle(integerStyle);

                    Cell cDeliv = row.createCell(4);
                    cDeliv.setCellValue(b.getCompletedOrders());
                    cDeliv.setCellStyle(integerStyle);

                    Cell cRev = row.createCell(5);
                    cRev.setCellValue(b.getRevenue().doubleValue());
                    cRev.setCellStyle(currencyStyle);

                    Cell cPct = row.createCell(6);
                    double pct = b.getPercentage() != null ? b.getPercentage().doubleValue() / 100.0 : 0.0;
                    cPct.setCellValue(pct);
                    cPct.setCellStyle(percentStyle);

                    Cell cRate = row.createCell(7);
                    cRate.setCellValue(b.getFulfillmentRate() / 100.0);
                    cRate.setCellStyle(percentStyle);
                }
            }
            for (int i = 0; i < bCols.length; i++) sheet2.autoSizeColumn(i);

            // ==========================================
            // SHEET 3: Top Products
            // ==========================================
            Sheet sheet3 = workbook.createSheet("Top Products");
            Row pHeader = sheet3.createRow(0);
            String[] pCols = {"Product ID", "SKU", "Product Name", "Category", "Units Sold", "Total Revenue"};
            for (int i = 0; i < pCols.length; i++) {
                Cell c = pHeader.createCell(i);
                c.setCellValue(pCols[i]);
                c.setCellStyle(headerStyle);
            }

            int pr = 1;
            if (topProducts != null) {
                for (TopProductDTO p : topProducts) {
                    Row row = sheet3.createRow(pr++);
                    row.createCell(0).setCellValue(p.getProductId() != null ? p.getProductId() : 0);
                    row.createCell(1).setCellValue(p.getSku() != null ? p.getSku() : "");
                    row.createCell(2).setCellValue(p.getName());
                    row.createCell(3).setCellValue(p.getCategoryName() != null ? p.getCategoryName() : "General");

                    Cell cUnits = row.createCell(4);
                    cUnits.setCellValue(p.getUnitsSold());
                    cUnits.setCellStyle(integerStyle);

                    Cell cRev = row.createCell(5);
                    cRev.setCellValue(p.getRevenue().doubleValue());
                    cRev.setCellStyle(currencyStyle);
                }
            }
            for (int i = 0; i < pCols.length; i++) sheet3.autoSizeColumn(i);

            // ==========================================
            // SHEET 4: Category Breakdown
            // ==========================================
            Sheet sheet4 = workbook.createSheet("Category Breakdown");
            Row cHeader = sheet4.createRow(0);
            String[] cCols = {"Category ID", "Category Name", "Units Dispatched", "Total Revenue", "Contribution %"};
            for (int i = 0; i < cCols.length; i++) {
                Cell c = cHeader.createCell(i);
                c.setCellValue(cCols[i]);
                c.setCellStyle(headerStyle);
            }

            int cr = 1;
            if (categories != null) {
                for (CategoryPerformanceDTO c : categories) {
                    Row row = sheet4.createRow(cr++);
                    row.createCell(0).setCellValue(c.getCategoryId());
                    row.createCell(1).setCellValue(c.getCategoryName());

                    Cell cUnits = row.createCell(2);
                    cUnits.setCellValue(c.getUnitsSold());
                    cUnits.setCellStyle(integerStyle);

                    Cell cRev = row.createCell(3);
                    cRev.setCellValue(c.getRevenue().doubleValue());
                    cRev.setCellStyle(currencyStyle);

                    Cell cPct = row.createCell(4);
                    double pct = c.getPercentage() != null ? c.getPercentage().doubleValue() / 100.0 : 0.0;
                    cPct.setCellValue(pct);
                    cPct.setCellStyle(percentStyle);
                }
            }
            for (int i = 0; i < cCols.length; i++) sheet4.autoSizeColumn(i);

            // ==========================================
            // SHEET 5: Inventory Health
            // ==========================================
            if (inventory != null && inventory.getBranchSummaries() != null) {
                Sheet sheet5 = workbook.createSheet("Inventory Breakdown");
                Row iHeader = sheet5.createRow(0);
                String[] iCols = {"Branch ID", "Branch Name", "Total Units In Stock", "Low Stock Items", "Out of Stock Items"};
                for (int i = 0; i < iCols.length; i++) {
                    Cell c = iHeader.createCell(i);
                    c.setCellValue(iCols[i]);
                    c.setCellStyle(headerStyle);
                }

                int ir = 1;
                for (InventoryHealthDTO.BranchStockSummaryDTO bs : inventory.getBranchSummaries()) {
                    Row row = sheet5.createRow(ir++);
                    row.createCell(0).setCellValue(bs.getBranchId());
                    row.createCell(1).setCellValue(bs.getBranchName());

                    Cell cUnits = row.createCell(2);
                    cUnits.setCellValue(bs.getTotalUnits());
                    cUnits.setCellStyle(integerStyle);

                    Cell cLow = row.createCell(3);
                    cLow.setCellValue(bs.getLowStockItems());
                    cLow.setCellStyle(integerStyle);

                    Cell cOut = row.createCell(4);
                    cOut.setCellValue(bs.getOutOfStockItems());
                    cOut.setCellStyle(integerStyle);
                }
                for (int i = 0; i < iCols.length; i++) sheet5.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Excel report", e);
            throw new RuntimeException("Failed to generate Excel analytics report: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateCsvReport(
            AnalyticsSummaryDTO summary,
            List<BranchPerformanceDTO> branches,
            List<TopProductDTO> topProducts,
            List<CategoryPerformanceDTO> categories,
            InventoryHealthDTO inventory,
            String dateRangeLabel,
            String branchScopeLabel,
            String generatedBy
    ) {
        log.info("Generating RFC-4180 CSV report");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // UTF-8 BOM for Microsoft Excel
        pw.print('\uFEFF');

        pw.println("ETec Computers Online Store — Executive Analytics Report");
        pw.println("Generated At," + escapeCsv(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        pw.println("Auditor," + escapeCsv(generatedBy != null ? generatedBy : "System Admin"));
        pw.println("Period," + escapeCsv(dateRangeLabel != null ? dateRangeLabel : "All Time"));
        pw.println("Branch Scope," + escapeCsv(branchScopeLabel != null ? branchScopeLabel : "All Branches"));
        pw.println();

        // 1. Executive Summary
        pw.println("--- EXECUTIVE KPIS ---");
        pw.println("Metric,Value");
        pw.println("Gross Revenue," + summary.getGrossRevenue());
        pw.println("Net Revenue," + summary.getNetRevenue());
        pw.println("Total Orders," + summary.getTotalOrders());
        pw.println("Completed Orders," + summary.getCompletedOrders());
        pw.println("Pending Orders," + summary.getPendingOrders());
        pw.println("Cancelled Orders," + summary.getCancelledOrders());
        pw.println("Average Order Value," + summary.getAvgOrderValue());
        pw.println("Total Units Sold," + summary.getTotalUnitsSold());
        pw.println("Fulfillment Rate (%)," + summary.getFulfillmentRate());
        if (inventory != null) {
            pw.println("Total Inventory Units," + inventory.getTotalUnitsInStock());
            pw.println("Low Stock Alerts," + inventory.getLowStockCount());
            pw.println("Out of Stock Products," + inventory.getOutOfStockCount());
        }
        pw.println();

        // 2. Branch Performance
        pw.println("--- BRANCH PERFORMANCE ---");
        pw.println("Branch ID,Branch Name,City,Orders,Completed,Revenue,Share %,Fulfillment Rate %");
        if (branches != null) {
            for (BranchPerformanceDTO b : branches) {
                pw.printf("%s,%s,%s,%d,%d,%.2f,%s,%.1f%n",
                        escapeCsv(b.getBranchId()),
                        escapeCsv(b.getBranchName()),
                        escapeCsv(b.getCity()),
                        b.getOrderCount(),
                        b.getCompletedOrders(),
                        b.getRevenue(),
                        b.getPercentage() != null ? b.getPercentage().toString() : "0",
                        b.getFulfillmentRate());
            }
        }
        pw.println();

        // 3. Top Products
        pw.println("--- TOP PRODUCTS ---");
        pw.println("Product ID,SKU,Name,Category,Units Sold,Revenue");
        if (topProducts != null) {
            for (TopProductDTO p : topProducts) {
                pw.printf("%d,%s,%s,%s,%d,%.2f%n",
                        p.getProductId() != null ? p.getProductId() : 0,
                        escapeCsv(p.getSku() != null ? p.getSku() : ""),
                        escapeCsv(p.getName()),
                        escapeCsv(p.getCategoryName() != null ? p.getCategoryName() : "General"),
                        p.getUnitsSold(),
                        p.getRevenue());
            }
        }
        pw.println();

        // 4. Category Performance
        if (categories != null) {
            pw.println("--- CATEGORY CONTRIBUTION ---");
            pw.println("Category ID,Category Name,Units Sold,Revenue,Share %");
            for (CategoryPerformanceDTO c : categories) {
                pw.printf("%s,%s,%d,%.2f,%s%n",
                        escapeCsv(c.getCategoryId()),
                        escapeCsv(c.getCategoryName()),
                        c.getUnitsSold(),
                        c.getRevenue(),
                        c.getPercentage() != null ? c.getPercentage().toString() : "0");
            }
        }

        pw.flush();
        return sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // --- Helper Methods ---

    private void addKpiRow(Sheet sheet, int rowNum, String metric, double value, CellStyle style, String notes) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(metric);
        Cell valCell = row.createCell(1);
        valCell.setCellValue(value);
        valCell.setCellStyle(style);
        row.createCell(2).setCellValue(notes);
    }

    private String escapeCsv(String str) {
        if (str == null) return "";
        if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            return "\"" + str.replace("\"", "\"\"") + "\"";
        }
        return str;
    }

    private String formatRs(BigDecimal val) {
        if (val == null) return "Rs. 0.00";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "Rs. " + nf.format(val);
    }

    private PdfPCell createHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE)));
        cell.setBackgroundColor(COLOR_PRIMARY);
        cell.setPadding(6);
        cell.setBorderColor(COLOR_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createBodyCell(String text, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_DARK)));
        cell.setBackgroundColor(bg);
        cell.setPadding(5);
        cell.setBorderColor(COLOR_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell createStyledCell(String text, boolean bold, float size) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, size, COLOR_TEXT_DARK)));
        cell.setPadding(6);
        cell.setBorderColor(COLOR_BORDER);
        return cell;
    }

    private PdfPCell createKpiCard(String title, String val, String subtext) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_BG_ALT);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(8);

        Paragraph pTitle = new Paragraph(title.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, COLOR_TEXT_MUTED));
        Paragraph pVal = new Paragraph(val, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_PRIMARY));
        Paragraph pSub = new Paragraph(subtext, FontFactory.getFont(FontFactory.HELVETICA, 7, COLOR_TEXT_MUTED));

        cell.addElement(pTitle);
        cell.addElement(pVal);
        cell.addElement(pSub);
        return cell;
    }
}
