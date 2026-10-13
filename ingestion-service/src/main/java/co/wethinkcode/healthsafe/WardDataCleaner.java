package co.wethinkcode.healthsafe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WardDataCleaner {

    private static final int MAX_REALISTIC_BEDS = 100;
    private static final Set<String> PLACEHOLDERS = Set.of("", "n/a", "na", "tbd", "unknown");

    /** The result of cleaning one beds value: the number (or null) and the reason it was changed, if any. */
    private record Beds(Integer value, String note) {
    }

    // Reads the CSV resource with the given name and returns a cleaned list of ward records.
    public static List<WardRecord> loadAndClean(String csvResourceName) throws IOException {
        InputStream stream = WardDataCleaner.class.getClassLoader().getResourceAsStream(csvResourceName);
        if (stream == null) {
            throw new IOException("CSV resource not found: " + csvResourceName);
        }

        List<WardRecord> rawRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            reader.readLine(); // skip header row
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1); // -1 keeps empty trailing fields
                if (parts.length < 4) continue;

                List<String> notes = new ArrayList<>();

                String wing = normaliseWing(clean(parts[1]));
                if (wing == null) {
                    notes.add("wing was missing - flagged for follow-up");
                }

                Beds beds = parseBeds(parts[3]);
                if (beds.note() != null) {
                    notes.add(beds.note());
                }

                rawRows.add(new WardRecord(
                        normaliseId(clean(parts[0])),
                        wing,
                        normaliseDept(clean(parts[2])),
                        beds.value(),
                        joinNotes(notes)));
            }
        }
        return mergeDuplicates(rawRows);
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normaliseId(String id) {
        return id.toUpperCase();
    }

    private static String normaliseWing(String wing) {
        if (wing.isEmpty()) {
            return null; // missing wing
        }
        return capitalizeWords(wing);
    }

    private static String normaliseDept(String dept) {
        String lower = dept.toLowerCase();
        if (lower.contains("paediatric") || lower.contains("pediatric")) {
            return "Pediatrics";
        }
        if (lower.equals("icu")) {
            return "ICU";
        }
        return capitalizeWords(dept);
    }

    private static String capitalizeWords(String text) {
        String[] words = text.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    // Turns the raw beds text into a number, or null plus a note explaining why it could not be used.
    private static Beds parseBeds(String raw) {
        String shown = clean(raw);
        String value = shown.toLowerCase();

        if (PLACEHOLDERS.contains(value)) {
            return new Beds(null, "bedsAvailable was missing or a placeholder ('" + shown + "') - flagged for follow-up");
        }

        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return new Beds(null, "bedsAvailable was non-numeric ('" + shown + "') - flagged for follow-up");
        }

        if (number < 0) {
            return new Beds(null, "bedsAvailable was negative (" + number + ") - flagged for follow-up");
        }
        if (number > MAX_REALISTIC_BEDS) {
            return new Beds(null, "bedsAvailable was unrealistic (" + number + ") - flagged for follow-up");
        }
        return new Beds(number, null);
    }

    private static String joinNotes(List<String> notes) {
        return notes.isEmpty() ? null : String.join("; ", notes);
    }

    // Rows with the same ward id are one real ward: keep the first non-empty value of each field
    // and keep the notes from every row, plus a note that a merge happened.
    private static List<WardRecord> mergeDuplicates(List<WardRecord> rows) {
        Map<String, WardRecord> merged = new LinkedHashMap<>();

        for (WardRecord row : rows) {
            WardRecord existing = merged.get(row.wardId);
            if (existing == null) {
                merged.put(row.wardId, row);
                continue;
            }

            String mergeNote = "Merged duplicate rows for " + row.wardId;
            List<String> notes = new ArrayList<>();
            if (existing.notes != null) notes.add(existing.notes);
            if (row.notes != null) notes.add(row.notes);
            if (existing.notes == null || !existing.notes.contains(mergeNote)) notes.add(mergeNote);

            merged.put(row.wardId, new WardRecord(
                    row.wardId,
                    existing.wing != null ? existing.wing : row.wing,
                    existing.department != null ? existing.department : row.department,
                    existing.bedsAvailable != null ? existing.bedsAvailable : row.bedsAvailable,
                    joinNotes(notes)));
        }
        return new ArrayList<>(merged.values());
    }
}
