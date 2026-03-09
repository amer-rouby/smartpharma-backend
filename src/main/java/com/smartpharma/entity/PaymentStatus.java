package com.smartpharma.entity;

public enum PaymentStatus {
    PENDING("قيد الانتظار"),
    PROCESSING("قيد المعالجة"),
    COMPLETED("مكتمل"),
    FAILED("فشل"),
    CANCELLED("ملغي"),
    REFUNDED("تم الاسترداد");

    private final String arabicName;

    PaymentStatus(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}