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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Coordinate-driven mapping of the KMSH (Albania) annual "Kalendari" PDF into {@link TimetableRow}s.
 * The PDF is column-major and prints half a month per page (days 1-15, then 16-31), so like the
 * Ukraine parser each time token's true (x, y) is read and the six columns are recovered by x, each
 * ordered top-to-bottom by y. Despite a busy header, the daily grid is a plain six-column table -
 * one dawn column and one asr column - mapping left to right to fajr, sunrise, dhuhr, asr, maghrib,
 * isha (Tirana reference; see task/scholar-verification.md). The day number is read from the
 * left-hand day column so half-month pages place correctly. Solar noon is enrichment, left 0.
 */
final class AlbaniaPdfParser {

    private static final Pattern TIME = Pattern.compile("\\d{1,2}:\\d{2}");
    private static final Pattern INT = Pattern.compile("^\\d{1,2}$");
    private static final double COL_GAP = 12.0;
    private static final double DAY_MAX_X = 110.0;   // day + hijri columns sit left of the times
    private static final String[] MONTHS = {
            "JANAR", "SHKURT", "MARS", "PRILL", "MAJ", "QERSHOR",
            "KORRIK", "GUSHT", "SHTATOR", "TETOR", "NENTOR", "DHJETOR"};

    List<TimetableRow> parse(byte[] pdf, int year) {
        try (PDDocument doc = PDDocument.load(pdf)) {
            List<TimetableRow> all = new ArrayList<>(366);
            Set<Integer> seen = new HashSet<>();
            for (int page = 1; page <= doc.getNumberOfPages(); page++) {
                int month = monthOfPage(doc, page);
                if (month == 0) {
                    continue;
                }
                Page p = capture(doc, page);
                addPage(all, seen, p, month);
            }
            if (all.size() < 365) {
                throw new ScrapeException("KMSH PDF yielded only " + all.size() + " rows for " + year);
            }
            all.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
            return all;
        } catch (IOException e) {
            throw new ScrapeException("Could not read KMSH PDF", e);
        }
    }

    private void addPage(List<TimetableRow> all, Set<Integer> seen, Page p, int month) {
        if (p.times.size() < 6 * 12) {
            return; // not a daily grid page (a half-month is at least Feb's 13 days = 78 times)
        }
        List<List<double[]>> cols = clusterByX(p.times);
        if (cols.size() != 6) {
            return;
        }
        // Rows are anchored on the day column (its longest consecutive run drops a stray page number);
        // each prayer time is the cell in its column nearest that row's y, so a ragged extra cell or a
        // merged stray cannot shift a day.
        List<double[]> days = dayColumn(p.days);
        for (double[] d : days) {
            int day = (int) d[2];
            if (day < 1 || day > 31 || !seen.add(month * 100 + day)) {
                continue;
            }
            all.add(new TimetableRow(month, day,
                    nearestByY(cols.get(0), d[1]),   // fajr (single dawn column)
                    nearestByY(cols.get(1), d[1]),   // sunrise
                    nearestByY(cols.get(2), d[1]),   // dhuhr
                    nearestByY(cols.get(3), d[1]),   // asr (single asr column)
                    nearestByY(cols.get(4), d[1]),   // maghrib
                    nearestByY(cols.get(5), d[1]),   // isha
                    0));
        }
    }

    private static int nearestByY(List<double[]> column, double y) {
        double best = Double.MAX_VALUE;
        int value = -1;
        for (double[] c : column) {
            double dy = Math.abs(c[1] - y);
            if (dy < best) {
                best = dy;
                value = (int) c[2];
            }
        }
        return value;
    }

    /**
     * The day numbers for the page: the leftmost dense integer column (left of the Hijri column),
     * reduced to its longest run of consecutive values so a stray page number cannot join it.
     */
    private static List<double[]> dayColumn(List<double[]> ints) {
        List<List<double[]>> cols = clusterByX(ints);
        List<double[]> best = null;
        for (List<double[]> c : cols) {
            if (c.size() >= 10 && (best == null || c.get(0)[0] < best.get(0)[0])) {
                best = c;
            }
        }
        if (best == null) {
            return List.of();
        }
        best.sort(Comparator.comparingDouble(a -> a[1]));
        int bestStart = 0;
        int bestLen = 1;
        int start = 0;
        for (int i = 1; i < best.size(); i++) {
            if ((int) best.get(i)[2] == (int) best.get(i - 1)[2] + 1) {
                if (i - start + 1 > bestLen) {
                    bestLen = i - start + 1;
                    bestStart = start;
                }
            } else {
                start = i;
            }
        }
        return new ArrayList<>(best.subList(bestStart, bestStart + bestLen));
    }

    private int monthOfPage(PDDocument doc, int page) throws IOException {
        PDFTextStripper ts = new PDFTextStripper();
        ts.setStartPage(page);
        ts.setEndPage(page);
        String flat = fold(ts.getText(doc)).replaceAll("\\s+", "");
        for (int i = 0; i < 12; i++) {
            if (flat.contains(MONTHS[i])) {
                return i + 1;
            }
        }
        return 0;
    }

    private Page capture(PDDocument doc, int page) throws IOException {
        Page out = new Page();
        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> positions) {
                if (positions.isEmpty()) {
                    return;
                }
                Matcher tm = TIME.matcher(text);
                while (tm.find()) {
                    TextPosition p = positions.get(Math.min(tm.start(), positions.size() - 1));
                    String t = tm.group();
                    int c = t.indexOf(':');
                    int mins = Integer.parseInt(t.substring(0, c)) * 60 + Integer.parseInt(t.substring(c + 1));
                    out.times.add(new double[] {p.getXDirAdj(), p.getYDirAdj(), mins});
                }
                String s = text.strip();
                if (INT.matcher(s).matches()) {
                    TextPosition p = positions.get(0);
                    if (p.getXDirAdj() < DAY_MAX_X) {
                        out.days.add(new double[] {p.getXDirAdj(), p.getYDirAdj(), Integer.parseInt(s)});
                    }
                }
            }
        };
        stripper.setSortByPosition(true);
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        stripper.getText(doc);
        return out;
    }

    /** Cluster cells into columns by their x (index 0); each returned column keeps insertion cells. */
    private static List<List<double[]>> clusterByX(List<double[]> cells) {
        List<double[]> sorted = new ArrayList<>(cells);
        sorted.sort(Comparator.comparingDouble(c -> c[0]));
        List<List<double[]>> clusters = new ArrayList<>();
        List<double[]> col = new ArrayList<>();
        double lastX = Double.NaN;
        for (double[] c : sorted) {
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

    private static String fold(String s) {
        return s.toUpperCase(Locale.ROOT).replace('Ë', 'E').replace('Ç', 'C');
    }

    private static final class Page {
        final List<double[]> times = new ArrayList<>();
        final List<double[]> days = new ArrayList<>();
    }
}
