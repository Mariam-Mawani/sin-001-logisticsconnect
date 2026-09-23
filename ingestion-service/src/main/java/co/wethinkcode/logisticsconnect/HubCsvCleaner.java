package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HubCsvCleaner {

    // Values that mean "we don't actually have this piece of data", however the
    // export happened to spell that. Compared in lower case after trimming.
    private static final Set<String> PLACEHOLDER_VALUES = Set.of(
            "", "n/a", "na", "tbd", "unknown", "-", "nan"
    );

    // Some hub-global exports spell the same province differently even though
    // they mean the same real-world place (hyphens, spacing, casing). Every key
    // here is lower-case with internal spacing already collapsed to single
    // spaces, so it lines up with what cleanText(...) produces.
    private static final Map<String, String> PROVINCE_ALIASES = Map.ofEntries(
            Map.entry("gauteng", "Gauteng"),
            Map.entry("western cape", "Western Cape"),
            Map.entry("eastern cape", "Eastern Cape"),
            Map.entry("kwazulu-natal", "KwaZulu-Natal"),
            Map.entry("kwazulu natal", "KwaZulu-Natal"),
            Map.entry("kwa-zulu natal", "KwaZulu-Natal"),
            Map.entry("free state", "Free State"),
            Map.entry("limpopo", "Limpopo"),
            Map.entry("north west", "North West"),
            Map.entry("mpumalanga", "Mpumalanga"),
            Map.entry("northern cape", "Northern Cape")
    );


    /**
     * Reads every row of the CSV, cleans each field, then deduplicates records
     * that describe the same real-world hub. Returns the final list ready to be
     * served over REST.
     */
    public static List<Hub> loadAndClean(InputStream csvInput) throws IOException, CsvException {
        List<Hub> rawHubs = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(csvInput, StandardCharsets.UTF_8))) {
            List<String[]> allRows = reader.readAll();

            // Row 0 is the header (hub_id, Province, sorting_center, active) - skip it.
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);

                // Defensive: skip any row that doesn't have all four columns rather
                // than crashing the whole ingestion on one malformed line.
                if (row.length < 4) {
                    continue;
                }
                String hubId = cleanId(row[0]);
                String province = cleanProvince(row[1]);
                String sortingCenter = cleanText(row[2]);
                Boolean active = cleanBoolean(row[3]);

                rawHubs.add(new Hub(hubId, province, sortingCenter, active));
            }
        }
        return deduplicate(rawHubs);
    }

    // Field-level cleaning
    /**
     * General-purpose text cleanup: trim outer padding, collapse any run of
     * inner whitespace (including double spaces) down to one space, then
     * Title Case every word so "johannesburg central" and "Johannesburg
     * Central" end up identical.
     */
    private static String cleanText(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        String[] words = trimmed.toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
