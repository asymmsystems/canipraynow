/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Pure mapping of vaktija.me's annual per-city PDF into {@link TimetableRow}s. PDFBox extracts the
 * row-major table; each month is introduced by a {@code <Month>, <City>} header and each day is
 * {@code <day> <weekday> <fajr> <sunrise> <dhuhr> <asr> <maghrib> <isha>} in 24-hour local time. The
 * current month is tracked as the lines stream past. Solar noon is an enrichment, left absent (0).
 */
final class MontenegroPdfParser {

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("januar", 1), Map.entry("februar", 2), Map.entry("mart", 3),
            Map.entry("april", 4), Map.entry("maj", 5), Map.entry("jun", 6),
            Map.entry("jul", 7), Map.entry("avgust", 8), Map.entry("septembar", 9),
            Map.entry("oktobar", 10), Map.entry("novembar", 11), Map.entry("decembar", 12));

    private static final Pattern MONTH_HEADER = Pattern.compile(
            "^(januar|februar|mart|april|maj|jun|jul|avgust|septembar|oktobar|novembar|decembar)\\s*,",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ROW = Pattern.compile(
            "^(\\d{1,2})\\s+\\S+" + "\\s+(\\d{1,2}:\\d{2})".repeat(6));

    List<TimetableRow> parse(byte[] pdf, int year) {
        String text;
        try (PDDocument doc = PDDocument.load(pdf)) {
            text = new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new ScrapeException("Could not read vaktija.me PDF", e);
        }

        List<TimetableRow> rows = new ArrayList<>(366);
        Set<Integer> seen = new HashSet<>();
        int month = 0;
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            Matcher mh = MONTH_HEADER.matcher(line);
            if (mh.find()) {
                month = MONTHS.get(mh.group(1).toLowerCase(Locale.ROOT));
                continue;
            }
            if (month == 0) {
                continue;
            }
            Matcher m = ROW.matcher(line);
            if (!m.find()) {
                continue;
            }
            int day = Integer.parseInt(m.group(1));
            if (day < 1 || day > 31 || !seen.add(month * 100 + day)) {
                continue;
            }
            rows.add(new TimetableRow(month, day,
                    hm(m.group(2)),   // fajr
                    hm(m.group(3)),   // sunrise
                    hm(m.group(4)),   // dhuhr
                    hm(m.group(5)),   // asr
                    hm(m.group(6)),   // maghrib
                    hm(m.group(7)),   // isha
                    0));
        }
        if (rows.size() < 365) {
            throw new ScrapeException("vaktija.me PDF yielded only " + rows.size() + " rows for " + year);
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return rows;
    }

    private static int hm(String hhmm) {
        int c = hhmm.indexOf(':');
        return Integer.parseInt(hhmm.substring(0, c)) * 60 + Integer.parseInt(hhmm.substring(c + 1));
    }
}
