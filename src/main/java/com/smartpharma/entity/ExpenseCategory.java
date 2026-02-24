package com.smartpharma.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExpenseCategory {
    PURCHASES("المشتريات", "Purchases of medicines and supplies"),
    SALARIES("الرواتب", "Employee salaries and wages"),
    RENT("الإيجار", "Pharmacy rent payments"),
    UTILITIES("المرافق", "Electricity, water, internet bills"),
    MAINTENANCE("الصيانة", "Equipment and facility maintenance"),
    MARKETING("التسويق", "Advertising and promotional activities"),
    INSURANCE("التأمين", "Insurance premiums"),
    LICENSES("التراخيص", "License and permit fees"),
    TRANSPORT("النقل", "Delivery and transportation costs"),
    OTHER("أخرى", "Other miscellaneous expenses");

    private final String arabicName;
    private final String description;
}