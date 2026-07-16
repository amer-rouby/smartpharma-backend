package com.smartpharma.config.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().trim();
        if (value.isEmpty()) {
            return null;
        }

        // Try standard date format first (yyyy-MM-dd)
        try {
            return LocalDate.parse(value, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // Ignore, try next format
        }

        // Try ISO datetime format (e.g., 2025-06-11T21:00:00.000Z)
        try {
            return LocalDate.parse(value, ISO_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            // Ignore, try next format
        }

        // Try parsing as LocalDateTime then extracting date
        try {
            return java.time.LocalDateTime.parse(value, ISO_DATE_TIME_FORMATTER).toLocalDate();
        } catch (DateTimeParseException e) {
            throw new IOException("Cannot deserialize value of type `java.time.LocalDate` from String \"" + value
                    + "\": expected format yyyy-MM-dd or ISO datetime", e);
        }
    }
}