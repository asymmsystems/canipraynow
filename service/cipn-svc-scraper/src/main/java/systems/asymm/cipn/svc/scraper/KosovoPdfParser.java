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
 * Pure mapping of BIK's annual "Takvim" PDF (Kosovo) into {@link TimetableRow}s. PDFBox extracts the
 * text deterministically (a real table extractor, never a summarising model). The table is
 * row-major, one day per line in 24-hour time:
 * {@code <day> e <weekday> <Imsaku> <Sabahu> <Lindja> <Dreka> <Ikindia> <Akshami> <Jacia> <length>}.
 * Each month is introduced by a {@code JANAR I / 2026} style header, so the current month is tracked
 * as the lines stream past.
 *
 * <p>The takvim prints two dawn columns: {@code Imsaku} (~18 deg, ~102 min before sunrise in winter)
 * and {@code Sabahu} (~14.5 deg). The fajr prayer enters at the 18-degree dawn, so {@code Imsaku} is
 * mapped to fajr and {@code Sabahu} is dropped; {@code Lindja e diellit} is sunrise, {@code Dreka}
 * dhuhr, {@code Ikindia} asr, {@code Akshami} maghrib, {@code Jacia} isha. The trailing day-length
 * column is ignored. See task/scholar-verification.md for the Imsaku/Sabahu note. Solar noon is an
 * enrichment, left absent (0).
 */
final class KosovoPdfParser {

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("JANAR", 1), Map.entry("SHKURT", 2), Map.entry("MARS", 3),
            Map.entry("PRILL", 4), Map.entry("MAJ", 5), Map.entry("QERSHOR", 6),
            Map.entry("KORRIK", 7), Map.entry("GUSHT", 8), Map.entry("SHTATOR", 9),
            Map.entry("TETOR", 10), Map.entry("NENTOR", 11), Map.entry("DHJETOR", 12));

    // A month header line: the Albanian month name starts the line (diacritics folded before match).
    private static final Pattern MONTH_HEADER = Pattern.compile(
            "^(JANAR|SHKURT|MARS|PRILL|MAJ|QERSHOR|KORRIK|GUSHT|SHTATOR|TETOR|NENTOR|DHJETOR)\\b");

    // A daily row: day, "e", weekday word, then eight HH:MM tokens. The minute separator is ':' in
    // most cells but PDFBox extracts the Dreka (dhuhr) glyph as '.' on many February rows, so both
    // are accepted - '11.54' is unambiguously 11:54.
    private static final Pattern ROW = Pattern.compile(
            "^(\\d{1,2})\\s+e\\s+\\S+" + "\\s+(\\d{1,2}[:.]\\d{2})".repeat(8));

    List<TimetableRow> parse(byte[] pdf, int year) {
        String text;
        try (PDDocument doc = PDDocument.load(pdf)) {
            text = new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new ScrapeException("Could not read BIK Takvim PDF", e);
        }

        List<TimetableRow> rows = new ArrayList<>(366);
        Set<Integer> seen = new HashSet<>();
        int month = 0;
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            Matcher mh = MONTH_HEADER.matcher(fold(line));
            if (mh.find()) {
                month = MONTHS.get(mh.group(1));
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
                    hm(m.group(2)),   // Imsaku -> fajr (18 deg dawn)
                    hm(m.group(4)),   // Lindja e diellit -> sunrise
                    hm(m.group(5)),   // Dreka -> dhuhr
                    hm(m.group(6)),   // Ikindia -> asr
                    hm(m.group(7)),   // Akshami -> maghrib
                    hm(m.group(8)),   // Jacia -> isha
                    0));
        }
        if (rows.size() < 365) {
            throw new ScrapeException("BIK Takvim PDF yielded only " + rows.size() + " rows for " + year);
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return rows;
    }

    private static int hm(String hhmm) {
        int c = hhmm.indexOf(':');
        if (c < 0) {
            c = hhmm.indexOf('.');
        }
        return Integer.parseInt(hhmm.substring(0, c)) * 60 + Integer.parseInt(hhmm.substring(c + 1));
    }

    private static String fold(String s) {
        return s.toUpperCase(Locale.ROOT).replace('Ë', 'E').replace('Ç', 'C');
    }
}
