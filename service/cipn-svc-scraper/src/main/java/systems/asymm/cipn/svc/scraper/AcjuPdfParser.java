/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.io.IOException;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Pure mapping of one ACJU (Sri Lanka) monthly PDF into {@link TimetableRow}s. PDFBox extracts the
 * row-major table deterministically; each data line is
 * {@code <day>-<MonthAbbr> <Fajr> <Sunrise> <Luhr> <Asr> <Magrib> <Isha>} with each time an explicit
 * 12-hour {@code H:MM AM|PM} (no inference needed). LUHR maps to dhuhr. Days beyond the month's
 * calendar length for the year are dropped (a stray Feb 29 on a non-leap year). Solar noon is an
 * enrichment, left absent (0).
 */
final class AcjuPdfParser {

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("feb", 2), Map.entry("mar", 3), Map.entry("apr", 4),
            Map.entry("may", 5), Map.entry("jun", 6), Map.entry("jul", 7), Map.entry("aug", 8),
            Map.entry("sep", 9), Map.entry("oct", 10), Map.entry("nov", 11), Map.entry("dec", 12));

    private static final Pattern DATE = Pattern.compile("^(\\d{1,2})-([A-Za-z]{3})");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*(AM|PM)", Pattern.CASE_INSENSITIVE);

    List<TimetableRow> parse(byte[] pdf, int year) {
        String text;
        try (PDDocument doc = PDDocument.load(pdf)) {
            text = new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new ScrapeException("Could not read ACJU PDF", e);
        }

        List<TimetableRow> rows = new ArrayList<>(31);
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            Matcher dm = DATE.matcher(line);
            if (!dm.find()) {
                continue;
            }
            Integer month = MONTHS.get(dm.group(2).toLowerCase(Locale.ROOT));
            if (month == null) {
                continue;
            }
            int day = Integer.parseInt(dm.group(1));
            if (day < 1 || day > Month.of(month).length(Year.isLeap(year))) {
                continue; // drop a stray day the calendar does not have this year
            }
            int[] t = sixTimes(line);
            if (t == null) {
                continue;
            }
            rows.add(new TimetableRow(month, day,
                    t[0],   // Fajr
                    t[1],   // Sunrise
                    t[2],   // Luhr -> dhuhr
                    t[3],   // Asr
                    t[4],   // Magrib -> maghrib
                    t[5],   // Isha
                    0));
        }
        if (rows.isEmpty()) {
            throw new ScrapeException("ACJU PDF yielded no rows");
        }
        return rows;
    }

    /** The first six {@code H:MM AM|PM} tokens on the line as minutes, or null if fewer than six. */
    private static int[] sixTimes(String line) {
        int[] out = new int[6];
        Matcher m = TIME.matcher(line);
        int i = 0;
        while (m.find() && i < 6) {
            int h = Integer.parseInt(m.group(1));
            int min = Integer.parseInt(m.group(2));
            boolean pm = m.group(3).equalsIgnoreCase("PM");
            out[i++] = ((pm ? (h == 12 ? 12 : h + 12) : (h == 12 ? 0 : h)) * 60) + min;
        }
        return i == 6 ? out : null;
    }
}
