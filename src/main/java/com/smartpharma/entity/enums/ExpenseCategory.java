package com.smartpharma.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExpenseCategory {
    PURCHASES("Purchases", "Purchases of medicines and supplies"),
    SALARIES("Salaries", "Employee salaries and wages"),
    RENT("Rent", "Pharmacy rent payments"),
    UTILITIES("Utilities", "Electricity, water, internet bills"),
    MAINTENANCE("Maintenance", "Equipment and facility maintenance"),
    MARKETING("Marketing", "Advertising and promotional activities"),
    INSURANCE("Insurance", "Insurance premiums"),
    LICENSES("Licenses", "License and permit fees"),
    TRANSPORT("Transport", "Delivery and transportation costs"),
    OTHER("Other", "Other miscellaneous expenses");

    private final String arabicName;
    private final String description;
}