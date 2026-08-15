package com.smartpharma.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Margin;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.PurchaseOrder;
import com.smartpharma.entity.PurchaseOrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Renders the purchase order PDF through a real (headless) browser instead of
 * the openpdf library - openpdf's Arabic text shaping is a legacy, incomplete
 * implementation that renders disconnected, unreadable glyphs no matter how the
 * document is built (verified: correct character/word order, still broken
 * glyph joining). A real browser engine renders Arabic exactly as well as it
 * already does on every other screen of this app, since it's the same engine.
 * Same letterhead visual language (indigo accent, clean table) as the
 * frontend's own print templates (print-document.util.ts), just in HTML here
 * since this runs server-side for the email attachment.
 */
@Service
@Slf4j
public class PurchaseOrderPdfService {

    private static final String BRAND = "#4338ca";
    private static final String TEXT_PRIMARY = "#1e293b";
    private static final String TEXT_SECONDARY = "#64748b";
    private static final String BORDER = "#cbd5e1";
    private static final String TABLE_HEADER_BG = "#f1f5f9";

    public byte[] generatePdf(PurchaseOrder order) {
        String html = buildHtml(order);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setArgs(java.util.List.of("--no-sandbox")));
            try {
                Page page = browser.newPage();
                page.setContent(html);
                byte[] pdf = page.pdf(new Page.PdfOptions()
                        .setFormat("A4")
                        .setPrintBackground(true)
                        .setMargin(new Margin().setTop("12mm").setBottom("12mm").setLeft("10mm").setRight("10mm")));
                return pdf;
            } finally {
                browser.close();
            }
        } catch (Exception e) {
            log.error("Failed to generate purchase order PDF via headless browser", e);
            throw new RuntimeException("Failed to generate purchase order PDF", e);
        }
    }

    private String buildHtml(PurchaseOrder order) {
        Pharmacy pharmacy = order.getPharmacy();
        String pharmacyName = pharmacy != null && pharmacy.getName() != null ? pharmacy.getName() : "SmartPharma";

        StringBuilder metaLine = new StringBuilder();
        if (pharmacy != null) {
            if (pharmacy.getAddress() != null && !pharmacy.getAddress().isBlank()) {
                metaLine.append(escape(pharmacy.getAddress()));
            }
            if (pharmacy.getPhone() != null && !pharmacy.getPhone().isBlank()) {
                if (metaLine.length() > 0) metaLine.append("&nbsp;&middot;&nbsp;");
                metaLine.append(escape(pharmacy.getPhone()));
            }
        }

        String supplierName = order.getSupplier() != null ? order.getSupplier().getName() : "غير محدد";

        StringBuilder itemsHtml = new StringBuilder();
        int index = 1;
        for (PurchaseOrderItem item : order.getItems()) {
            String productName = item.getProduct() != null ? item.getProduct().getName() : "منتج غير متاح";
            itemsHtml.append("<tr>")
                    .append("<td>").append(index).append("</td>")
                    .append("<td>").append(escape(productName)).append("</td>")
                    .append("<td>").append(item.getQuantity()).append("</td>")
                    .append("<td>").append(String.format("%.2f ج.م", item.getUnitPrice())).append("</td>")
                    .append("<td>").append(String.format("%.2f ج.م", item.getTotalPrice())).append("</td>")
                    .append("</tr>");
            index++;
        }

        String deliveryLabel = order.getExpectedDeliveryDate() != null ? "تاريخ التسليم المتوقع" : "الأولوية";
        String deliveryValue = order.getExpectedDeliveryDate() != null
                ? String.valueOf(order.getExpectedDeliveryDate())
                : translatePriority(order.getPriority());

        String sixthField = (order.getPaymentTerms() != null && !order.getPaymentTerms().isBlank())
                ? infoField("شروط الدفع", escape(order.getPaymentTerms()))
                : infoField("الأولوية", translatePriority(order.getPriority()));

        String generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        return "<!DOCTYPE html><html dir=\"rtl\" lang=\"ar\"><head><meta charset=\"UTF-8\"><style>" +
                "* { margin: 0; padding: 0; box-sizing: border-box; }" +
                "html, body { font-family: 'Segoe UI', Tahoma, Arial, sans-serif; color: " + TEXT_PRIMARY + "; }" +
                "body { direction: rtl; padding: 4mm; font-size: 12px; line-height: 1.5; }" +
                ".letterhead { text-align: center; padding-bottom: 10px; margin-bottom: 18px; border-bottom: 2px solid " + BRAND + "; }" +
                ".ph-name { font-size: 20px; font-weight: 700; }" +
                ".ph-meta { font-size: 11px; color: " + TEXT_SECONDARY + "; margin-top: 4px; }" +
                ".doc-title { font-size: 16px; font-weight: 700; color: " + BRAND + "; margin-top: 10px; }" +
                ".info-table { width: 100%; border-collapse: collapse; margin-bottom: 18px; }" +
                ".info-table td { padding: 6px 8px; border-bottom: 1px solid " + BORDER + "; }" +
                ".info-label { font-size: 10px; font-weight: 700; color: " + TEXT_SECONDARY + "; white-space: nowrap; width: 1%; }" +
                ".info-value { font-size: 12px; font-weight: 700; }" +
                "table.items { width: 100%; border-collapse: collapse; }" +
                "table.items th { background: " + TABLE_HEADER_BG + "; font-weight: 700; font-size: 11px; text-align: right; padding: 8px; border-bottom: 2px solid " + BRAND + "; }" +
                "table.items td { padding: 7px 8px; border-bottom: 1px solid " + BORDER + "; font-size: 11.5px; }" +
                "table.items tbody tr:nth-child(even) { background: #fafafa; }" +
                ".totals { margin-top: 16px; text-align: left; }" +
                ".totals-inner { display: inline-block; min-width: 260px; border-top: 2px solid " + BRAND + "; padding-top: 8px; }" +
                ".totals-row { display: flex; justify-content: space-between; font-size: 14px; font-weight: 700; }" +
                ".totals-row .amount { color: " + BRAND + "; }" +
                ".doc-footer { text-align: center; margin-top: 24px; padding-top: 10px; border-top: 1px solid " + BORDER + "; color: " + TEXT_SECONDARY + "; font-size: 10px; }" +
                "</style></head><body>" +
                "<header class=\"letterhead\">" +
                "<div class=\"ph-name\">" + escape(pharmacyName) + "</div>" +
                (metaLine.length() > 0 ? "<div class=\"ph-meta\">" + metaLine + "</div>" : "") +
                "<div class=\"doc-title\">طلب شراء</div>" +
                "</header>" +
                "<table class=\"info-table\"><tbody>" +
                "<tr>" + infoField("رقم الطلب", escape(order.getOrderNumber())) + infoField("التاريخ", String.valueOf(order.getOrderDate())) + "</tr>" +
                "<tr>" + infoField("المورد", escape(supplierName)) + infoField("الحالة", translateStatus(order.getStatus())) + "</tr>" +
                "<tr>" + infoField(deliveryLabel, deliveryValue) + sixthField + "</tr>" +
                "</tbody></table>" +
                "<table class=\"items\"><thead><tr>" +
                "<th>#</th><th>المنتج</th><th>الكمية</th><th>سعر الوحدة</th><th>الإجمالي</th>" +
                "</tr></thead><tbody>" + itemsHtml + "</tbody></table>" +
                "<div class=\"totals\"><div class=\"totals-inner\"><div class=\"totals-row\">" +
                "<span>إجمالي المبلغ</span><span class=\"amount\">" + String.format("%.2f ج.م", order.getTotalAmount()) + "</span>" +
                "</div></div></div>" +
                "<footer class=\"doc-footer\">تم الإنشاء: " + generatedAt + "</footer>" +
                "</body></html>";
    }

    private String infoField(String label, String value) {
        return "<td class=\"info-label\">" + escape(label) + "</td><td class=\"info-value\">" + value + "</td>";
    }

    private String translateStatus(Object status) {
        if (status == null) return "-";
        return switch (status.toString()) {
            case "DRAFT" -> "مسودة";
            case "PENDING" -> "معلّق";
            case "APPROVED" -> "معتمد";
            case "RECEIVED" -> "تم الاستلام";
            case "CANCELLED" -> "ملغي";
            default -> status.toString();
        };
    }

    private String translatePriority(Object priority) {
        if (priority == null) return "-";
        return switch (priority.toString()) {
            case "LOW" -> "منخفضة";
            case "NORMAL" -> "عادية";
            case "URGENT" -> "عاجلة";
            default -> priority.toString();
        };
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
