package com.smartpharma.catalog.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Turns a free-text active ingredient / scientific name into a key that is the
 * same for every product with the same substance(s), so the POS can offer
 * alternatives: "Amoxicillin + Clavulanic Acid 625mg" and "clavulanic acid +
 * amoxicillin suspension" both become "amoxicillin + clavulanic".
 *
 * Strength and dosage form are dropped on purpose - the pharmacist sees them on
 * each alternative and decides whether the swap fits.
 */
public final class IngredientKey {

    // 500mg, 0.5 mg, 1g, 250mg/5ml, 5%, 100 IU, SPF50
    private static final Pattern STRENGTH = Pattern.compile(
            "\\d+(?:[.,]\\d+)?\\s*(?:mg|mcg|µg|g|ml|iu|%)(?:\\s*/\\s*\\d*(?:[.,]\\d+)?\\s*(?:ml|g))?\\b|\\bspf\\s*\\d+|\\d+(?:[.,]\\d+)?%",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern SEPARATOR = Pattern.compile("\\s*(?:\\+|/|&|\\bwith\\b|\\band\\b)\\s*");

    private static final Set<String> FORM_WORDS = Set.of(
            "tablet", "tablets", "tab", "tabs", "capsule", "capsules", "cap", "caps",
            "suspension", "syrup", "drops", "drop", "injection", "ampoule", "vial",
            "cream", "gel", "ointment", "lotion", "spray", "nasal", "eye", "ear",
            "inhaler", "suppository", "suppositories", "effervescent", "sachets", "sachet",
            "solution", "lozenges", "shampoo", "vaginal", "powder", "linctus", "retard",
            "infant", "infants", "baby", "pediatric", "children", "for", "extract", "acid",
            // The hydrochloride salt is the usual form, so "Metformin HCl" is Metformin.
            "hcl", "hydrochloride");

    private IngredientKey() {
    }

    /** Null when there is nothing to match on. */
    public static String of(String text) {
        if (text == null) return null;
        String withoutStrength = STRENGTH.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ");
        String key = Arrays.stream(SEPARATOR.split(withoutStrength))
                .map(IngredientKey::substance)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.joining(" + "));
        return key.isEmpty() ? null : key;
    }

    private static String substance(String part) {
        return Arrays.stream(part.trim().split("[\\s,()\\-]+"))
                .filter(w -> !w.isEmpty() && !FORM_WORDS.contains(w))
                .collect(Collectors.joining(" "));
    }
}
