/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Pure mapping of MUIS's annual PDF timetable into {@link TimetableRow}s. PDFBox extracts the text
 * deterministically (a real table extractor, never a summarising model); each data line is
 * {@code D/M/YYYY <Day> <Subuh> <Syuruk> <Zohor> <Asar> <Maghrib> <Isyak>} with each time printed as
 * a space-separated 12-hour {@code H MM}. Subuh (fajr) and Syuruk (sunrise) are AM; Zohor (dhuhr),
 * Asar, Maghrib and Isyak (isha) are PM - so the 12-hour hours are converted per column. Solar noon
 * is an enrichment, left absent (0).
 */
final class SingaporePdfParser {

    // date  day  then six "H MM" pairs
    private static final Pattern ROW = Pattern.compile(
            "(\\d{1,2})/(\\d{1,2})/(\\d{4})\\s+\\S+" + "\\s+(\\d{1,2})\\s+(\\d{2})".repeat(6));

    List<TimetableRow> parse(byte[] pdf, int year) {
        String text;
        try (PDDocument doc = PDDocument.load(pdf)) {
            text = new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            throw new ScrapeException("Could not read MUIS PDF", e);
        }

        List<TimetableRow> rows = new ArrayList<>(366);
        Matcher m = ROW.matcher(text);
        while (m.find()) {
            if (Integer.parseInt(m.group(3)) != year) {
                continue;
            }
            int day = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int[] g = new int[12];
            for (int i = 0; i < 12; i++) {
                g[i] = Integer.parseInt(m.group(4 + i));
            }
            rows.add(new TimetableRow(month, day,
                    am(g[0], g[1]),   // Subuh -> fajr
                    am(g[2], g[3]),   // Syuruk -> sunrise
                    pm(g[4], g[5]),   // Zohor -> dhuhr
                    pm(g[6], g[7]),   // Asar -> asr
                    pm(g[8], g[9]),   // Maghrib -> maghrib
                    pm(g[10], g[11]), // Isyak -> isha
                    0));
        }
        if (rows.isEmpty()) {
            throw new ScrapeException("No MUIS PDF rows for " + year);
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return rows;
    }

    private static int am(int hour, int minute) {
        return (hour == 12 ? 0 : hour) * 60 + minute;
    }

    private static int pm(int hour, int minute) {
        return (hour == 12 ? 12 : hour + 12) * 60 + minute;
    }
}
