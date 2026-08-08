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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Pure mapping of the CIL (Comunidade Islamica de Lisboa) annual PDF into {@link TimetableRow}s.
 * PDFBox extracts the row-major table deterministically; each data line is
 * {@code <day> <weekday PT|EN> <Hijri> <Fajr> <Nascer> <Zawal> <Zuhr> <Assr> <Magrib> <Isha 12deg+3>
 * <Isha 01:20>} in 24-hour time. The month is taken from each page's {@code jan/26 LISBOA} header.
 *
 * <p>Only the first eight times on a line are the prayer columns; some lines carry a trailing
 * moon-phase note with its own time, which is ignored. Zawal (the forbidden solar-noon instant) is
 * dropped, dhuhr is Zuhr, and isha is the primary "12deg + 3 min" beginning column (the later
 * "01:20" fixed-interval column is dropped; see task/scholar-verification.md). Solar noon is an
 * enrichment, left absent (0).
 */
final class PortugalPdfParser {

    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("fev", 2), Map.entry("mar", 3), Map.entry("abr", 4),
            Map.entry("mai", 5), Map.entry("jun", 6), Map.entry("jul", 7), Map.entry("ago", 8),
            Map.entry("set", 9), Map.entry("out", 10), Map.entry("nov", 11), Map.entry("dez", 12));

    private static final Pattern MONTH_HEADER = Pattern.compile("^([a-z]{3})/\\d{2}\\s+LISBOA");
    private static final Pattern DAY = Pattern.compile("^(\\d{1,2})\\b");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})");

    List<TimetableRow> parse(byte[] pdf, int year) {
        String text;
        try (PDDocument doc = PDDocument.load(pdf)) {
            text = new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new ScrapeException("Could not read CIL PDF", e);
        }

        List<TimetableRow> rows = new ArrayList<>(366);
        Set<Integer> seen = new HashSet<>();
        int month = 0;
        for (String raw : text.split("\\R")) {
            String line = raw.strip();
            Matcher mh = MONTH_HEADER.matcher(line);
            if (mh.find()) {
                month = MONTHS.getOrDefault(mh.group(1), 0);
                continue;
            }
            if (month == 0) {
                continue;
            }
            Matcher dm = DAY.matcher(line);
            if (!dm.find()) {
                continue;
            }
            int[] t = firstTimes(line, 8);
            if (t == null) {
                continue;
            }
            int day = Integer.parseInt(dm.group(1));
            if (day < 1 || day > 31 || !seen.add(month * 100 + day)) {
                continue;
            }
            rows.add(new TimetableRow(month, day,
                    t[0],   // Fajr
                    t[1],   // Nascer do Sol -> sunrise
                    t[3],   // Zuhr -> dhuhr (t[2] = Zawal, skipped)
                    t[4],   // Assr -> asr
                    t[5],   // Magrib -> maghrib
                    t[6],   // Isha (12deg + 3 min); t[7] = Isha (01:20), skipped
                    0));
        }
        if (rows.size() < 365) {
            throw new ScrapeException("CIL PDF yielded only " + rows.size() + " rows for " + year);
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return rows;
    }

    /** The first {@code n} HH:MM tokens on the line as minutes, or null if there are fewer than n. */
    private static int[] firstTimes(String line, int n) {
        int[] out = new int[n];
        Matcher m = TIME.matcher(line);
        int i = 0;
        while (m.find() && i < n) {
            out[i++] = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
        }
        return i == n ? out : null;
    }
}
