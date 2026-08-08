/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Pure mapping of the namaz.muftyat.kz annual JSON response into {@link TimetableRow}s - no I/O. The
 * response is {@code {"success": true, "result": [ {"date":"01-01-2026","Fajr":"05:59 ",
 * "Sunrise":"07:19 ","Dhuhr":"12:01 ","Asr":"14:50 ","Maghrib":"16:32 ","Isha":"17:53 "}, ... ]}}
 * with 24-hour times (a trailing space) and {@code dd-MM-yyyy} dates. Solar noon is enrichment,
 * left absent (0).
 */
final class KazakhstanFeedParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    List<TimetableRow> parse(String json, int year) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (Exception e) {
            throw new ScrapeException("Could not parse muftyat.kz JSON", e);
        }
        if (!root.path("success").asBoolean(false)) {
            throw new ScrapeException("muftyat.kz reported success=false (unknown settlement coordinates?)");
        }
        List<TimetableRow> rows = new ArrayList<>(366);
        for (JsonNode d : root.path("result")) {
            String date = d.path("date").asText();           // dd-MM-yyyy
            String[] p = date.split("-");
            if (p.length != 3 || Integer.parseInt(p[2]) != year) {
                continue;
            }
            rows.add(new TimetableRow(Integer.parseInt(p[1]), Integer.parseInt(p[0]),
                    hm(d, "Fajr"), hm(d, "Sunrise"), hm(d, "Dhuhr"),
                    hm(d, "Asr"), hm(d, "Maghrib"), hm(d, "Isha"), 0));
        }
        if (rows.size() < 365) {
            throw new ScrapeException("muftyat.kz returned only " + rows.size() + " days for " + year);
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return rows;
    }

    private static int hm(JsonNode day, String key) {
        String t = day.path(key).asText().trim();
        int c = t.indexOf(':');
        if (c < 0) {
            throw new ScrapeException("muftyat.kz bad time '" + t + "' for " + key);
        }
        return Integer.parseInt(t.substring(0, c)) * 60 + Integer.parseInt(t.substring(c + 1));
    }
}
