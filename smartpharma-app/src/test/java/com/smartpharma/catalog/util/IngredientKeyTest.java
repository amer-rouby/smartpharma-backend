package com.smartpharma.catalog.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IngredientKeyTest {

    @Test
    void strengthAndDosageFormDontMatter() {
        assertThat(IngredientKey.of("Paracetamol 500mg")).isEqualTo("paracetamol");
        assertThat(IngredientKey.of("Paracetamol 250mg/5ml")).isEqualTo("paracetamol");
        assertThat(IngredientKey.of("Paracetamol Infant Drops")).isEqualTo("paracetamol");
        assertThat(IngredientKey.of("paracetamol suppository")).isEqualTo("paracetamol");
        assertThat(IngredientKey.of("Hydrocortisone 1% Cream")).isEqualTo("hydrocortisone");
        assertThat(IngredientKey.of("Alprazolam 0.5mg")).isEqualTo("alprazolam");
        assertThat(IngredientKey.of("Metformin HCl 500mg")).isEqualTo("metformin");
    }

    @Test
    void combinationsMatchInAnyOrder() {
        String key = IngredientKey.of("Amoxicillin + Clavulanic Acid 625mg");
        assertThat(key).isEqualTo("amoxicillin + clavulanic");
        assertThat(IngredientKey.of("Clavulanic acid/Amoxicillin Suspension")).isEqualTo(key);
        assertThat(IngredientKey.of("Amoxicillin + Clavulanic Injection")).isEqualTo(key);
    }

    @Test
    void differentSubstancesStayApart() {
        assertThat(IngredientKey.of("Diclofenac Potassium 50mg"))
                .isNotEqualTo(IngredientKey.of("Diclofenac"));
        assertThat(IngredientKey.of("Paracetamol + Caffeine"))
                .isNotEqualTo(IngredientKey.of("Paracetamol"));
    }

    @Test
    void nothingToMatchOn() {
        assertThat(IngredientKey.of(null)).isNull();
        assertThat(IngredientKey.of("  ")).isNull();
        assertThat(IngredientKey.of("500mg Tablets")).isNull();
    }

    @Test
    void arabicNamesAreKeptAsTyped() {
        assertThat(IngredientKey.of(" باراسيتامول  500mg")).isEqualTo("باراسيتامول");
    }
}
