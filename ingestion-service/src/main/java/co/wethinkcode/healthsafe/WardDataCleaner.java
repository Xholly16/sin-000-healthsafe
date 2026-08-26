package co.wethinkcode.healthsafe;
import kotlin.jvm.internal.PropertyReference0Impl;

import java.io.*;
import java.util.*;
import java.io.InputStreamReader;

public class WardDataCleaner {
    // Reads the CSV FILE AT THE GIVEN PATH AND RETURNS A CLEANED LIST OF WARD RECORD
    public static List<WardRecord> loadAndClean(String csvResourceName) throws IOException{
        List<WardRecord> rawRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader((new InputStreamReader(
                WardDataCleaner.class.getClassLoader().getResourceAsStream(csvResourceName))))){
            String line = reader.readLine(); // Skips header row
            while((line = reader.readLine()) != null){
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1); // -1 keeps empty trailing fields
                if (parts.length < 4) continue;

                String wardId = clean(parts[0]);
                String wing = clean(parts[1]);
                String department = clean(parts[2]);
                Integer beds = parseBeds(parts[3]);

                rawRows.add(new WardRecord(
                        normaliseId(wardId),
                        normaliseWing(wing),
                        normaliseDept(department),
                        beds,
                        null
                ));

            }

            }
        return mergeDuplicatates(rawRows);
    }

    private static String clean(String value){
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normaliseId(String id){
        return id.toUpperCase();
    }

    private static String normaliseWing(String wing){
        if (wing.isEmpty()){
            return null; // missing wing
        }
        return capitalizeWords(wing);
    }

    private static String normaliseDept(String dept){
        String lower = dept.toLowerCase();
        if (lower.contains("paediatric") || lower.contains("pediatric")){
            return "Pediatrics";
        }
        if (lower.equals("icu")) {
            return "ICU";
        }
        return capitalizeWords(dept);
    }

    private static String capitalizeWords(String text){
        String[] words = text.toLowerCase().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static Integer parseBeds(String raw){
        String value = clean(raw).toLowerCase();
        Set<String> placeholders = Set.of("n/a","na","tbd", "unknown","");
        if (placeholders.contains(value)){
            return null;
        }

        try {
            int num = Integer.parseInt(value);
            if (num < 0) {
                return null;        // negetive beds makes no sence
            }
            if (num > 100){
                return null;        //treat unrealisti  values (e.g. 2023)
            }
            return num;
        } catch (NumberFormatException e){
            return null;            // things like "five" or "full"
        }
    }

    private static List<WardRecord> mergeDuplicatates(List<WardRecord> rows){
        Map<String,WardRecord> merged = new LinkedHashMap<>();

        for (WardRecord row: rows){
            String key = row.wardId;
            if (!merged.containsKey(key)) {
                merged.put(key, row);
            } else {
                WardRecord existing = merged.get(key);
                WardRecord combined = new WardRecord(
                        key,
                        existing.wing != null ? existing.wing : row.wing,
                        existing.department != null ? existing.department : row.department,
                        existing.bedsAvailable != null ? existing.bedsAvailable : row.bedsAvailable,
                        "Merged duplicate rows for " + key
                );
                merged.put(key,combined);
            }
        }
        return new ArrayList<>(merged.values());
    }

}
