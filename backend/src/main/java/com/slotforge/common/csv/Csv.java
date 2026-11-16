package com.slotforge.common.csv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.slotforge.common.exception.ApiException;

/**
 * Reads an uploaded CSV file with a header row. Handles quoted fields, doubled quotes, commas and line breaks inside quotes,
 * Windows line endings and the byte-order mark Excel adds. Column names are matched without regard to case or spaces.
 */
public final class Csv {

    public static final int MAX_ROWS = 5_000;

    /** line is the line of the file the row starts on, counting the header as 1, so a person can find it in a spreadsheet. */
    public record Row(int line, Map<String, String> values) {

        public String get(String column) {
            return values.getOrDefault(column, "").trim();
        }
    }

    private Csv() {
    }

    public static List<Row> parse(String text, List<String> requiredColumns) {
        List<List<String>> records = new ArrayList<>();
        List<Integer> lines = new ArrayList<>();
        split(text.startsWith("\uFEFF") ? text.substring(1) : text, records, lines);
        if (records.isEmpty()) throw ApiException.unprocessable("INVALID_FILE", "The file is empty.");

        List<String> header = records.get(0).stream().map(Csv::columnName).toList();
        List<String> missing = requiredColumns.stream().filter(column -> !header.contains(column)).toList();
        if (!missing.isEmpty()) {
            throw ApiException.unprocessable("INVALID_FILE",
                    "The first row must name these columns: " + String.join(", ", requiredColumns) + ". Missing: " + String.join(", ", missing) + ".");
        }
        if (records.size() - 1 > MAX_ROWS) {
            throw ApiException.unprocessable("INVALID_FILE", "A file can hold at most " + MAX_ROWS + " rows. Split it and import each part.");
        }
        List<Row> rows = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            Map<String, String> values = new LinkedHashMap<>();
            List<String> fields = records.get(index);
            for (int column = 0; column < header.size(); column++) {
                values.put(header.get(column), column < fields.size() ? fields.get(column) : "");
            }
            rows.add(new Row(lines.get(index), values));
        }
        if (rows.isEmpty()) throw ApiException.unprocessable("INVALID_FILE", "The file has a header row but no data rows.");
        return rows;
    }

    private static String columnName(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }

    private static void split(String text, List<List<String>> records, List<Integer> lines) {
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int line = 1;
        int startedOn = 1;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (quoted) {
                if (current == '"' && index + 1 < text.length() && text.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else if (current == '"') {
                    quoted = false;
                } else {
                    if (current == '\n') line++;
                    field.append(current);
                }
            } else if (current == '"' && field.isEmpty()) {
                quoted = true;
            } else if (current == ',') {
                record.add(field.toString());
                field.setLength(0);
            } else if (current == '\n' || current == '\r') {
                if (current == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') index++;
                record.add(field.toString());
                field.setLength(0);
                keep(record, startedOn, records, lines);
                record = new ArrayList<>();
                line++;
                startedOn = line;
            } else {
                field.append(current);
            }
        }
        if (quoted) throw ApiException.unprocessable("INVALID_FILE", "A quoted value that starts on line " + startedOn + " is never closed.");
        record.add(field.toString());
        keep(record, startedOn, records, lines);
    }

    // A blank line, such as the one a spreadsheet leaves at the end, is not a row.
    private static void keep(List<String> record, int line, List<List<String>> records, List<Integer> lines) {
        if (record.stream().allMatch(String::isBlank)) return;
        records.add(record);
        lines.add(line);
    }
}
