package com.smartpharma.entity.enums;

public enum RefundStatus {
    PENDING("قيد الانتظار"),
    APPROVED("موافق عليه"),
    REJECTED("مرفوض"),
    PROCESSING("قيد المعالجة"),
    COMPLETED("مكتمل"),
    FAILED("فشل");

    private final String arabicName;

    RefundStatus(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}