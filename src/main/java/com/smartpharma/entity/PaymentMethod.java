package com.smartpharma.entity;

public enum PaymentMethod {
    CASH("نقدي"),
    VISA("بطاقة ائتمان"),
    INSTAPAY("إنستا باي"),
    FAWRY("فوري"),
    WALLET("محفظة إلكترونية"),
    BANK_TRANSFER("تحويل بنكي");

    private final String arabicName;

    PaymentMethod(String arabicName) {
        this.arabicName = arabicName;
    }

    public String getArabicName() {
        return arabicName;
    }
}