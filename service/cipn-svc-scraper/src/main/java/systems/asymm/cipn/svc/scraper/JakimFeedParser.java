/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Pure mapping of JAKIM's e-Solat {@code takwimsolat} JSON into {@link TimetableRow}s - no I/O,
 * so it is unit-tested against a captured response. The e-Solat envelope is
 * {@code { "prayerTime": [ {date, fajr, syuruk, dhuhr, asr, maghrib, isha, ...}, ... ], "status": "OK!" }};
 * {@code date} is {@code dd-MMM-yyyy} with a <em>Malay</em> month name (e.g. {@code 01-Mac-2026}),
 * and each prayer is {@code HH:MM:SS}. Rows are converted to minutes-from-midnight; e-Solat's
 * {@code syuruk} is our sunrise. Solar noon is not published by the authority (an E3 enrichment),
 * so it is left absent (0). Days outside the requested year are dropped defensively; the result is
 * sorted in calendar order.
 */
final class JakimFeedParser {

    /**
     * Month lookup keyed on the first three letters of the month token, which is unique across
     * every Malay and English month name whether e-Solat sends an abbreviation or the full name
     * ({@code Mac}, {@code Ogos}, {@code Julai}, {@code Disember}, {@code March}, ...).
     */
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("feb", 2), Map.entry("mac", 3), Map.entry("mar", 3),
            Map.entry("apr", 4), Map.entry("mei", 5), Map.entry("may", 5), Map.entry("jun", 6),
            Map.entry("jul", 7), Map.entry("ogo", 8), Map.entry("aug", 8), Map.entry("sep", 9),
            Map.entry("okt", 10), Map.entry("oct", 10), Map.entry("nov", 11),
            Map.entry("dis", 12), Map.entry("dec", 12));
    private static final String ARRAY_FIELD = "prayerTime";

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param json the raw e-Solat response body
     * @param year the Gregorian year requested (rows for other years are ignored)
     * @return the year's rows in calendar order (possibly partial - completeness is the caller's check)
     * @throws ScrapeException if the body is not the expected e-Solat shape or a day is malformed
     */
    List<TimetableRow> parse(String json, int year) {
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            throw new ScrapeException("Could not parse e-Solat JSON", e);
        }
        JsonNode days = root.get(ARRAY_FIELD);
        if (days == null || !days.isArray()) {
            JsonNode status = root.get("status");
            throw new ScrapeException("e-Solat response has no '" + ARRAY_FIELD + "' array (status="
                    + (status == null ? "?" : status.asText()) + ")");
        }
        List<TimetableRow> rows = new ArrayList<>();
        for (JsonNode day : days) {
            LocalDate date = date(day);
            if (date.getYear() != year) {
                continue;
            }
            rows.add(new TimetableRow(date.getMonthValue(), date.getDayOfMonth(),
                    minute(day, "fajr"), minute(day, "syuruk"), minute(day, "dhuhr"),
                    minute(day, "asr"), minute(day, "maghrib"), minute(day, "isha"), 0));
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return rows;
    }

    private static LocalDate date(JsonNode day) {
        String raw = text(day, "date");
        String[] parts = raw.split("-");
        if (parts.length != 3) {
            throw new ScrapeException("Unparseable e-Solat date '" + raw + "'");
        }
        String token = parts[1].trim().toLowerCase(Locale.ROOT);
        Integer month = token.length() >= 3 ? MONTHS.get(token.substring(0, 3)) : null;
        if (month == null) {
            throw new ScrapeException("Unknown e-Solat month in date '" + raw + "'");
        }
        try {
            return LocalDate.of(Integer.parseInt(parts[2].trim()), month, Integer.parseInt(parts[0].trim()));
        } catch (NumberFormatException | DateTimeException e) {
            throw new ScrapeException("Unparseable e-Solat date '" + raw + "'", e);
        }
    }

    /** Convert an {@code HH:MM:SS} prayer time to minutes from local midnight. */
    private static int minute(JsonNode day, String field) {
        String raw = text(day, field);
        String[] parts = raw.split(":");
        if (parts.length < 2) {
            throw new ScrapeException("Malformed e-Solat time '" + raw + "' for '" + field + "'");
        }
        try {
            int hours = Integer.parseInt(parts[0].trim());
            int minutes = Integer.parseInt(parts[1].trim());
            int total = hours * 60 + minutes;
            if (total < 0 || total > 1439) {
                throw new ScrapeException("e-Solat time '" + raw + "' for '" + field + "' out of range");
            }
            return total;
        } catch (NumberFormatException e) {
            throw new ScrapeException("Malformed e-Solat time '" + raw + "' for '" + field + "'", e);
        }
    }

    private static String text(JsonNode day, String field) {
        JsonNode node = day.get(field);
        if (node == null || node.isNull()) {
            throw new ScrapeException("e-Solat day is missing '" + field + "'");
        }
        return node.asText().trim();
    }
}
