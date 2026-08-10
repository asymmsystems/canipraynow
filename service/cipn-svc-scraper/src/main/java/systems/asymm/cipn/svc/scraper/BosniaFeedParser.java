/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Pure mapping of the vaktija.ba JSON month response into {@link TimetableRow}s - no I/O, so it is
 * unit-tested against a captured response. The envelope is
 * {@code {"lokacija":..,"godina":Y,"mjesec":M,"dan":[{"vakat":["3:34","5:27","12:53","16:51","20:16","21:55"]},..]}};
 * each day's {@code vakat} array is zora, izlazak, podne, ikindija, akšam, jacija, mapping to fajr,
 * sunrise, dhuhr, asr, maghrib, isha ({@code H:MM}). The day is the array index; the month is
 * supplied by the caller (one request per month). Solar noon is an enrichment, left absent (0).
 */
final class BosniaFeedParser {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * @param json  one month's vaktija.ba response
     * @param month the month this response is for (1-12)
     * @return that month's rows
     */
    List<TimetableRow> parse(String json, int month) {
        JsonNode root;
        try {
            root = mapper.readTree(json);
        } catch (Exception e) {
            throw new ScrapeException("Could not parse vaktija.ba JSON", e);
        }
        JsonNode days = root.get("dan");
        if (days == null || !days.isArray() || days.isEmpty()) {
            throw new ScrapeException("vaktija.ba response has no 'dan' array");
        }
        List<TimetableRow> rows = new ArrayList<>(days.size());
        int day = 1;
        for (JsonNode d : days) {
            JsonNode vakat = d.get("vakat");
            if (vakat == null || vakat.size() < 6) {
                throw new ScrapeException("vaktija.ba day " + day + " has no six-time vakat");
            }
            rows.add(new TimetableRow(month, day,
                    minute(vakat.get(0)), minute(vakat.get(1)), minute(vakat.get(2)),
                    minute(vakat.get(3)), minute(vakat.get(4)), minute(vakat.get(5)), 0));
            day++;
        }
        return rows;
    }

    /** {@code H:MM} to minutes from midnight. */
    private static int minute(JsonNode node) {
        String raw = node.asText().trim();
        String[] parts = raw.split(":");
        if (parts.length < 2) {
            throw new ScrapeException("Malformed vaktija.ba time '" + raw + "'");
        }
        try {
            int total = Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
            if (total < 0 || total > 1439) {
                throw new ScrapeException("vaktija.ba time '" + raw + "' out of range");
            }
            return total;
        } catch (NumberFormatException e) {
            throw new ScrapeException("Malformed vaktija.ba time '" + raw + "'", e);
        }
    }
}
