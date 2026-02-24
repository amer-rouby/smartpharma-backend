package com.smartpharma.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;  // ✅ أضف الـ import ده
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
public class ReportExportService {

    // ================================
    // ✅ Excel Export (شغال 100%)
    // ================================

    public byte[] exportExpensesToExcel(List<Map<String, Object>> expenses) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("المصروفات");

            // ✅ Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ✅ Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"التصنيف", "العنوان", "المبلغ", "التاريخ", "طريقة الدفع", "الرقم المرجعي"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ✅ Add data rows
            int rowNum = 1;
            for (Map<String, Object> expense : expenses) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue((String) expense.get("category"));
                row.createCell(1).setCellValue((String) expense.get("title"));
                row.createCell(2).setCellValue(Double.parseDouble(expense.get("amount").toString()));
                row.createCell(3).setCellValue((String) expense.get("expenseDate"));
                row.createCell(4).setCellValue((String) expense.get("paymentMethod"));
                row.createCell(5).setCellValue((String) expense.get("referenceNumber"));
            }

            // ✅ Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    // ================================
    // ✅ PDF Export - FIXED Color issue
    // ================================

    // ✅ FIXED: PDF Export with fallback font
    public byte[] exportExpensesToPdf(List<Map<String, Object>> expenses) throws Exception {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        // ✅ FIXED: Try Arabic font, fallback to HELVETICA if not found
        com.lowagie.text.Font titleFont;
        com.lowagie.text.Font headerFont;
        com.lowagie.text.Font dataFont;

        try {
            BaseFont arabicFont = BaseFont.createFont(
                    "fonts/arial.ttf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );
            titleFont = new com.lowagie.text.Font(arabicFont, 18, com.lowagie.text.Font.BOLD);
            headerFont = new com.lowagie.text.Font(arabicFont, 12, com.lowagie.text.Font.BOLD);
            dataFont = new com.lowagie.text.Font(arabicFont, 10, com.lowagie.text.Font.NORMAL);
        } catch (Exception e) {
            // ✅ Fallback to standard font (English only)
            BaseFont baseFont = BaseFont.createFont(
                    BaseFont.HELVETICA,
                    BaseFont.WINANSI,
                    BaseFont.EMBEDDED
            );
            titleFont = new com.lowagie.text.Font(baseFont, 18, com.lowagie.text.Font.BOLD);
            headerFont = new com.lowagie.text.Font(baseFont, 12, com.lowagie.text.Font.BOLD);
            dataFont = new com.lowagie.text.Font(baseFont, 10, com.lowagie.text.Font.NORMAL);
        }

        document.open();

        // ✅ Add title (English fallback if Arabic font not available)
        Paragraph title = new Paragraph("Expenses Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // ✅ Add table
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new int[]{2, 3, 2, 2, 2, 2});

        // ✅ Add headers - FIXED: استخدم java.awt.Color
        String[] headers = {"Category", "Title", "Amount", "Date", "Payment", "Reference"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(new java.awt.Color(220, 220, 220));  // ✅ FIXED
            table.addCell(cell);
        }

        // ✅ Add data
        for (Map<String, Object> expense : expenses) {
            table.addCell(new PdfPCell(new Phrase((String) expense.get("category"), dataFont)));
            table.addCell(new PdfPCell(new Phrase((String) expense.get("title"), dataFont)));
            table.addCell(new PdfPCell(new Phrase(expense.get("amount").toString(), dataFont)));
            table.addCell(new PdfPCell(new Phrase((String) expense.get("expenseDate"), dataFont)));
            table.addCell(new PdfPCell(new Phrase((String) expense.get("paymentMethod"), dataFont)));
            table.addCell(new PdfPCell(new Phrase((String) expense.get("referenceNumber"), dataFont)));
        }

        document.add(table);
        document.close();

        return outputStream.toByteArray();
    }
    // ================================
    // ✅ Financial Report Excel
    // ================================

    public byte[] exportFinancialReportToExcel(
            double totalRevenue,
            double totalExpenses,
            double netProfit,
            double profitMargin,
            List<Map<String, Object>> monthlyData,
            List<Map<String, Object>> expensesByCategory
    ) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // ✅ Summary Sheet
            Sheet summarySheet = workbook.createSheet("الملخص");
            Row row1 = summarySheet.createRow(0);
            row1.createCell(0).setCellValue("إجمالي الإيرادات");
            row1.createCell(1).setCellValue(totalRevenue);

            Row row2 = summarySheet.createRow(1);
            row2.createCell(0).setCellValue("إجمالي المصروفات");
            row2.createCell(1).setCellValue(totalExpenses);

            Row row3 = summarySheet.createRow(2);
            row3.createCell(0).setCellValue("صافي الربح");
            row3.createCell(1).setCellValue(netProfit);

            Row row4 = summarySheet.createRow(3);
            row4.createCell(0).setCellValue("هامش الربح %");
            row4.createCell(1).setCellValue(profitMargin);

            // ✅ Monthly Data Sheet
            Sheet monthlySheet = workbook.createSheet("البيانات الشهرية");
            Row headerRow = monthlySheet.createRow(0);
            headerRow.createCell(0).setCellValue("الشهر");
            headerRow.createCell(1).setCellValue("الإيرادات");
            headerRow.createCell(2).setCellValue("المصروفات");
            headerRow.createCell(3).setCellValue("الربح");

            int rowNum = 1;
            for (Map<String, Object> data : monthlyData) {
                Row row = monthlySheet.createRow(rowNum++);
                row.createCell(0).setCellValue((String) data.get("month"));
                row.createCell(1).setCellValue(Double.parseDouble(data.get("revenue").toString()));
                row.createCell(2).setCellValue(Double.parseDouble(data.get("expenses").toString()));
                row.createCell(3).setCellValue(Double.parseDouble(data.get("profit").toString()));
            }

            // ✅ Category Sheet
            Sheet categorySheet = workbook.createSheet("التصنيفات");
            Row catHeaderRow = categorySheet.createRow(0);
            catHeaderRow.createCell(0).setCellValue("التصنيف");
            catHeaderRow.createCell(1).setCellValue("المبلغ");

            int catRowNum = 1;
            for (Map<String, Object> cat : expensesByCategory) {
                Row row = categorySheet.createRow(catRowNum++);
                row.createCell(0).setCellValue((String) cat.get("category"));
                row.createCell(1).setCellValue(Double.parseDouble(cat.get("amount").toString()));
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}