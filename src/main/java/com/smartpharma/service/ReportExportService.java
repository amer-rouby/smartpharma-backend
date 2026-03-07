package com.smartpharma.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ReportExportService {

    // ✅ Sales Report to PDF - FIXED Product Names
    public byte[] exportSalesReportToPdf(double totalRevenue, long totalOrders, double avgOrder,
                                         List<Map<String, Object>> dailySales, List<Map<String, Object>> topProducts,
                                         Map<String, ?> revenueByPayment) {
        try {
            Document document = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFont, 18, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(baseFont, 12, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font dataFont = new com.lowagie.text.Font(baseFont, 10, com.lowagie.text.Font.NORMAL);

            // Title
            Paragraph title = new Paragraph("Sales Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Date
            Paragraph date = new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), dataFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            // Summary table
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(50);
            summaryTable.setWidths(new int[]{2, 1});

            addPdfCell(summaryTable, "Total Revenue:", headerFont, true);
            addPdfCell(summaryTable, String.format("%.2f EGP", totalRevenue), dataFont, false);
            addPdfCell(summaryTable, "Total Orders:", headerFont, true);
            addPdfCell(summaryTable, String.valueOf(totalOrders), dataFont, false);
            addPdfCell(summaryTable, "Average Order:", headerFont, true);
            addPdfCell(summaryTable, String.format("%.2f EGP", avgOrder), dataFont, false);

            document.add(summaryTable);
            document.add(Chunk.NEWLINE);

            // Top products table
            Paragraph productsTitle = new Paragraph("Top Selling Products", headerFont);
            productsTitle.setSpacingAfter(10);
            document.add(productsTitle);

            PdfPTable productsTable = new PdfPTable(3);
            productsTable.setWidthPercentage(100);
            productsTable.setWidths(new int[]{3, 1, 1});

            // Headers
            String[] headers = {"Product Name", "Quantity", "Revenue"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(220, 220, 220));
                productsTable.addCell(cell);
            }

            // Data - FIXED: Get product name correctly
            for (Map<String, Object> product : topProducts) {
                // ✅ Try multiple keys to get product name
                String productName = getProductSafeName(product);

                productsTable.addCell(new PdfPCell(new Phrase(productName, dataFont)));
                productsTable.addCell(new PdfPCell(new Phrase(
                        getSafeValue(product, "quantitySold", "0"), dataFont)));
                productsTable.addCell(new PdfPCell(new Phrase(
                        String.format("%.2f", parseDouble(product.get("totalRevenue"))), dataFont)));
            }

            document.add(productsTable);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export sales report to PDF", e);
        }
    }

    // ✅ Helper method to safely get product name
    private String getProductSafeName(Map<String, Object> product) {
        // Try different possible keys
        String name = getSafeValue(product, "productName", null);
        if (name == null || name.isEmpty() || name.matches("\\d+")) {
            name = getSafeValue(product, "name", null);
        }
        if (name == null || name.isEmpty() || name.matches("\\d+")) {
            name = "Product ID: " + getSafeValue(product, "productId", "N/A");
        }
        return name;
    }

    // ✅ Helper method to safely get value
    private String getSafeValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        String strValue = value.toString();
        return strValue.isEmpty() ? defaultValue : strValue;
    }

    private void addPdfCell(PdfPTable table, String text, com.lowagie.text.Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (isHeader) {
            cell.setBackgroundColor(new Color(220, 220, 220));
            cell.setPadding(5);
        }
        table.addCell(cell);
    }

    private double parseDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // باقي الـ methods (Excel exports) زي ما هي...
    public byte[] exportSalesReportToExcel(double totalRevenue, long totalOrders, double avgOrder,
                                           List<Map<String, Object>> dailySales, List<Map<String, Object>> topProducts,
                                           Map<String, ?> revenueByPayment) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet summary = workbook.createSheet("Summary");
            summary.setDefaultColumnWidth(30);
            summary.createRow(0).createCell(0).setCellValue("Total Revenue:");
            summary.getRow(0).createCell(1).setCellValue(totalRevenue);
            summary.createRow(1).createCell(0).setCellValue("Total Orders:");
            summary.getRow(1).createCell(1).setCellValue((double) totalOrders);
            summary.createRow(2).createCell(0).setCellValue("Average Order:");
            summary.getRow(2).createCell(1).setCellValue(avgOrder);

            Sheet dailySheet = workbook.createSheet("Daily Sales");
            dailySheet.setDefaultColumnWidth(20);
            Row dailyHeader = dailySheet.createRow(0);
            String[] dailyHeaders = {"Date", "Revenue", "Orders"};
            createStyledHeader(dailyHeader, dailyHeaders, workbook);

            int rowNum = 1;
            for (Map<String, Object> sale : dailySales) {
                Row row = dailySheet.createRow(rowNum++);
                row.createCell(0).setCellValue(sale.get("date") != null ? sale.get("date").toString() : "");
                row.createCell(1).setCellValue(parseDouble(sale.get("revenue")));
                row.createCell(2).setCellValue(parseDouble(sale.get("orders")));
            }

            Sheet productsSheet = workbook.createSheet("Top Products");
            productsSheet.setDefaultColumnWidth(30);
            Row productsHeader = productsSheet.createRow(0);
            String[] productHeaders = {"Product", "Quantity", "Revenue"};
            createStyledHeader(productsHeader, productHeaders, workbook);

            rowNum = 1;
            for (Map<String, Object> product : topProducts) {
                Row row = productsSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(getProductSafeName(product));
                row.createCell(1).setCellValue(parseDouble(product.get("quantitySold")));
                row.createCell(2).setCellValue(parseDouble(product.get("totalRevenue")));
            }

            Sheet paymentSheet = workbook.createSheet("Payment Methods");
            paymentSheet.setDefaultColumnWidth(25);
            Row paymentHeader = paymentSheet.createRow(0);
            String[] paymentHeaders = {"Method", "Revenue"};
            createStyledHeader(paymentHeader, paymentHeaders, workbook);

            rowNum = 1;
            for (Map.Entry<String, ?> entry : revenueByPayment.entrySet()) {
                Row row = paymentSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(parseDouble(entry.getValue()));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export sales report", e);
        }
    }

    private void createStyledHeader(Row row, String[] headers, Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    // باقي الـ methods (Expenses, Financial, Expiry)...
    public byte[] exportExpensesToExcel(List<Map<String, Object>> expenses) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Expenses");
            sheet.setDefaultColumnWidth(25);

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] headers = {"Category", "Title", "Amount", "Date", "Payment Method", "Reference"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Map<String, Object> exp : expenses) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(exp.get("category") != null ? exp.get("category").toString() : "");
                row.createCell(1).setCellValue(exp.get("title") != null ? exp.get("title").toString() : "");
                row.createCell(2).setCellValue(parseDouble(exp.get("amount")));
                row.createCell(3).setCellValue(exp.get("expenseDate") != null ? exp.get("expenseDate").toString() : "");
                row.createCell(4).setCellValue(exp.get("paymentMethod") != null ? exp.get("paymentMethod").toString() : "");
                row.createCell(5).setCellValue(exp.get("referenceNumber") != null ? exp.get("referenceNumber").toString() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export expenses to Excel", e);
        }
    }

    public byte[] exportExpensesToPdf(List<Map<String, Object>> expenses) {
        try {
            Document document = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFont, 18, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(baseFont, 12, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font dataFont = new com.lowagie.text.Font(baseFont, 10, com.lowagie.text.Font.NORMAL);

            Paragraph title = new Paragraph("Expenses Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{2, 3, 2, 2, 2, 2});

            String[] headers = {"Category", "Title", "Amount", "Date", "Payment Method", "Reference"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(220, 220, 220));
                table.addCell(cell);
            }

            for (Map<String, Object> exp : expenses) {
                table.addCell(new PdfPCell(new Phrase(
                        exp.get("category") != null ? exp.get("category").toString() : "N/A", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        exp.get("title") != null ? exp.get("title").toString() : "N/A", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        String.format("%.2f", parseDouble(exp.get("amount"))), dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        exp.get("expenseDate") != null ? exp.get("expenseDate").toString() : "N/A", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        exp.get("paymentMethod") != null ? exp.get("paymentMethod").toString() : "N/A", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        exp.get("referenceNumber") != null ? exp.get("referenceNumber").toString() : "N/A", dataFont)));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export expenses to PDF", e);
        }
    }

    public byte[] exportFinancialReportToExcel(double totalRevenue, double totalExpenses, double netProfit, double profitMargin,
                                               List<Map<String, Object>> monthlyData, List<Map<String, Object>> expensesByCategory) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet summary = workbook.createSheet("Summary");
            summary.setDefaultColumnWidth(30);

            int rowIdx = 0;
            summary.createRow(rowIdx).createCell(0).setCellValue("Total Revenue:");
            summary.getRow(rowIdx).createCell(1).setCellValue(totalRevenue);
            summary.createRow(++rowIdx).createCell(0).setCellValue("Total Expenses:");
            summary.getRow(rowIdx).createCell(1).setCellValue(totalExpenses);
            summary.createRow(++rowIdx).createCell(0).setCellValue("Net Profit:");
            summary.getRow(rowIdx).createCell(1).setCellValue(netProfit);
            summary.createRow(++rowIdx).createCell(0).setCellValue("Profit Margin %:");
            summary.getRow(rowIdx).createCell(1).setCellValue(profitMargin);

            Sheet monthlySheet = workbook.createSheet("Monthly Data");
            monthlySheet.setDefaultColumnWidth(20);
            Row monthlyHeader = monthlySheet.createRow(0);
            String[] monthlyHeaders = {"Month", "Revenue", "Expenses", "Profit"};
            createStyledHeader(monthlyHeader, monthlyHeaders, workbook);

            int rowNum = 1;
            for (Map<String, Object> data : monthlyData) {
                Row row = monthlySheet.createRow(rowNum++);
                row.createCell(0).setCellValue(data.get("month") != null ? data.get("month").toString() : "");
                row.createCell(1).setCellValue(parseDouble(data.get("revenue")));
                row.createCell(2).setCellValue(parseDouble(data.get("expenses")));
                row.createCell(3).setCellValue(parseDouble(data.get("profit")));
            }

            Sheet categorySheet = workbook.createSheet("Categories");
            categorySheet.setDefaultColumnWidth(25);
            Row catHeader = categorySheet.createRow(0);
            String[] catHeaders = {"Category", "Amount"};
            createStyledHeader(catHeader, catHeaders, workbook);

            rowNum = 1;
            for (Map<String, Object> cat : expensesByCategory) {
                Row row = categorySheet.createRow(rowNum++);
                row.createCell(0).setCellValue(cat.get("category") != null ? cat.get("category").toString() : "");
                row.createCell(1).setCellValue(parseDouble(cat.get("amount")));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export financial report", e);
        }
    }

    public byte[] exportExpiryReportToExcel(long total, long urgent, long warning, long ok, List<Map<String, Object>> products) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet summary = workbook.createSheet("Summary");
            summary.setDefaultColumnWidth(30);
            summary.createRow(0).createCell(0).setCellValue("Total Products:");
            summary.getRow(0).createCell(1).setCellValue((double) total);
            summary.createRow(1).createCell(0).setCellValue("Urgent (< 7 days):");
            summary.getRow(1).createCell(1).setCellValue((double) urgent);
            summary.createRow(2).createCell(0).setCellValue("Warning (< 30 days):");
            summary.getRow(2).createCell(1).setCellValue((double) warning);
            summary.createRow(3).createCell(0).setCellValue("OK (> 30 days):");
            summary.getRow(3).createCell(1).setCellValue((double) ok);

            Sheet detailsSheet = workbook.createSheet("Details");
            detailsSheet.setDefaultColumnWidth(25);
            Row header = detailsSheet.createRow(0);
            String[] headers = {"Product", "Batch", "Expiry Date", "Days Left", "Quantity", "Status"};
            createStyledHeader(header, headers, workbook);

            int rowNum = 1;
            for (Map<String, Object> product : products) {
                Row row = detailsSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(product.get("productName") != null ? product.get("productName").toString() : "");
                row.createCell(1).setCellValue(product.get("batchNumber") != null ? product.get("batchNumber").toString() : "");
                row.createCell(2).setCellValue(product.get("expiryDate") != null ? product.get("expiryDate").toString() : "");
                row.createCell(3).setCellValue(parseDouble(product.get("daysUntilExpiry")));
                row.createCell(4).setCellValue(parseDouble(product.get("currentStock")));
                row.createCell(5).setCellValue(product.get("status") != null ? product.get("status").toString() : "");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export expiry report", e);
        }
    }

    public byte[] exportExpiryReportToPdf(long total, long urgent, long warning, long ok, List<Map<String, Object>> products) {
        try {
            Document document = new Document(PageSize.A4.rotate());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(baseFont, 18, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(baseFont, 12, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font dataFont = new com.lowagie.text.Font(baseFont, 10, com.lowagie.text.Font.NORMAL);

            Paragraph title = new Paragraph("Product Expiry Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(50);
            addPdfCell(summaryTable, "Total Products:", headerFont, true);
            addPdfCell(summaryTable, String.valueOf(total), dataFont, false);
            addPdfCell(summaryTable, "Urgent (< 7 days):", headerFont, true);
            addPdfCell(summaryTable, String.valueOf(urgent), dataFont, false);
            addPdfCell(summaryTable, "Warning (< 30 days):", headerFont, true);
            addPdfCell(summaryTable, String.valueOf(warning), dataFont, false);
            addPdfCell(summaryTable, "OK (> 30 days):", headerFont, true);
            addPdfCell(summaryTable, String.valueOf(ok), dataFont, false);

            document.add(summaryTable);
            document.add(Chunk.NEWLINE);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            String[] headers = {"Product", "Batch", "Expiry Date", "Days Left", "Quantity", "Status"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new Color(220, 220, 220));
                table.addCell(cell);
            }

            for (Map<String, Object> p : products) {
                table.addCell(new PdfPCell(new Phrase(
                        p.get("productName") != null ? p.get("productName").toString() : "N/A", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        p.get("batchNumber") != null ? p.get("batchNumber").toString() : "N/A", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        p.get("expiryDate") != null ? p.get("expiryDate").toString() : "N/A", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        p.get("daysUntilExpiry") != null ? p.get("daysUntilExpiry").toString() : "0", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        p.get("currentStock") != null ? p.get("currentStock").toString() : "0", dataFont)));
                table.addCell(new PdfPCell(new Phrase(
                        p.get("status") != null ? p.get("status").toString() : "N/A", dataFont)));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export expiry report to PDF", e);
        }
    }
}