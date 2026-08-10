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
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Coordinate-driven mapping of the islam.ua "Kalendar namaziv" annual PDF (Kyiv) into
 * {@link TimetableRow}s. This PDF is column-major and its text linearises badly - the fajr time
 * merges into the day/weekday/holiday run and single rows split across text chunks - so a naive
 * text parse silently misaligns days (the exact failure that shifted a hand transcription). Instead
 * each time token's TRUE (x, y) is read from the matched character's {@link TextPosition}: tokens are
 * clustered into rows by y (top-to-bottom = day 1..N) and ordered within a row by x into the six
 * columns Rassvet/fajr, Voskhod/sunrise, Zuhr/dhuhr, Asr, Magrib/maghrib, Isha. Each data page is one
 * month, identified by its Russian month name. The parse fails loudly unless every row holds exactly
 * six times and each month holds exactly its calendar length. Solar noon is enrichment, left 0.
 */
final class UkrainePdfParser {

    private static final Pattern TIME = Pattern.compile("\\d{1,2}:\\d{2}");
    private static final double COL_GAP = 20.0;   // the six columns are ~45px apart; within one, x is stable
    private static final String[] MONTHS = {
            "ЯНВАРЬ", "ФЕВРАЛЬ", "МАРТ", "АПРЕЛЬ", "МАЙ", "ИЮНЬ",
            "ИЮЛЬ", "АВГУСТ", "СЕНТЯБРЬ", "ОКТЯБРЬ", "НОЯБРЬ", "ДЕКАБРЬ"};

    List<TimetableRow> parse(byte[] pdf, int year) {
        try (PDDocument doc = PDDocument.load(pdf)) {
            List<TimetableRow> all = new ArrayList<>(366);
            for (int page = 1; page <= doc.getNumberOfPages(); page++) {
                int month = monthOfPage(doc, page);
                if (month == 0) {
                    continue;
                }
                List<double[]> cells = capture(doc, page);
                int expected = Month.of(month).length(Year.isLeap(year));
                all.addAll(toRows(cells, month, expected));
            }
            if (all.size() < 365) {
                throw new ScrapeException("islam.ua PDF yielded only " + all.size() + " rows for " + year);
            }
            all.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
            return all;
        } catch (IOException e) {
            throw new ScrapeException("Could not read islam.ua PDF", e);
        }
    }

    private int monthOfPage(PDDocument doc, int page) throws IOException {
        PDFTextStripper ts = new PDFTextStripper();
        ts.setStartPage(page);
        ts.setEndPage(page);
        String text = ts.getText(doc);
        for (int i = 0; i < 12; i++) {
            if (text.contains(MONTHS[i])) {
                return i + 1;
            }
        }
        return 0;
    }

    /** Capture every time token on the page as {x, y, minutes}, using the matched glyph's real position. */
    private List<double[]> capture(PDDocument doc, int page) throws IOException {
        List<double[]> cells = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> positions) {
                Matcher m = TIME.matcher(text);
                while (m.find()) {
                    if (positions.isEmpty()) {
                        continue;
                    }
                    // Ligature-collapsing runs can make the string longer than the position list;
                    // clamp to the run's last glyph, which still sits on this row's fajr cell.
                    TextPosition p = positions.get(Math.min(m.start(), positions.size() - 1));
                    String t = m.group();
                    int c = t.indexOf(':');
                    int mins = Integer.parseInt(t.substring(0, c)) * 60 + Integer.parseInt(t.substring(c + 1));
                    cells.add(new double[] {p.getXDirAdj(), p.getYDirAdj(), mins});
                }
            }
        };
        stripper.setSortByPosition(true);
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        stripper.getText(doc);
        return cells;
    }

    /**
     * Reconstruct the six columns and read across. The five right-hand columns
     * (sunrise..isha) always land cleanly at fixed x, so they are the five rightmost x-clusters.
     * Fajr merges into the day/weekday/holiday run, so a couple of its times report a corrupted x;
     * every cell left of those five clean columns is therefore taken as fajr and ordered by y. Each
     * of the six columns must hold exactly the month's day count, or the parse fails loudly.
     */
    private List<TimetableRow> toRows(List<double[]> cells, int month, int expectedDays) {
        List<List<double[]>> clusters = clusterByX(cells);
        if (clusters.size() < 6) {
            throw new ScrapeException("islam.ua month " + month + " found " + clusters.size()
                    + " time columns, expected at least 6");
        }
        // The five rightmost clusters are sunrise, dhuhr, asr, maghrib, isha (ascending x).
        List<List<double[]>> cols = new ArrayList<>();
        List<double[]> fajr = new ArrayList<>();
        for (int i = 0; i < clusters.size() - 5; i++) {
            fajr.addAll(clusters.get(i));
        }
        cols.add(fajr);
        cols.addAll(clusters.subList(clusters.size() - 5, clusters.size()));

        for (List<double[]> cc : cols) {
            if (cc.size() != expectedDays) {
                throw new ScrapeException("islam.ua month " + month + " column has " + cc.size()
                        + " values, expected " + expectedDays);
            }
            cc.sort(Comparator.comparingDouble(c -> c[1]));
        }
        List<TimetableRow> rows = new ArrayList<>(expectedDays);
        for (int i = 0; i < expectedDays; i++) {
            rows.add(new TimetableRow(month, i + 1,
                    (int) cols.get(0).get(i)[2],   // Rassvet -> fajr
                    (int) cols.get(1).get(i)[2],   // Voskhod -> sunrise
                    (int) cols.get(2).get(i)[2],   // Zuhr -> dhuhr
                    (int) cols.get(3).get(i)[2],   // Asr
                    (int) cols.get(4).get(i)[2],   // Magrib -> maghrib
                    (int) cols.get(5).get(i)[2],   // Isha
                    0));
        }
        return rows;
    }

    private static List<List<double[]>> clusterByX(List<double[]> cells) {
        cells.sort(Comparator.comparingDouble(c -> c[0]));
        List<List<double[]>> clusters = new ArrayList<>();
        List<double[]> col = new ArrayList<>();
        double lastX = Double.NaN;
        for (double[] c : cells) {
            if (!col.isEmpty() && c[0] - lastX > COL_GAP) {
                clusters.add(col);
                col = new ArrayList<>();
            }
            col.add(c);
            lastX = c[0];
        }
        if (!col.isEmpty()) {
            clusters.add(col);
        }
        return clusters;
    }
}
