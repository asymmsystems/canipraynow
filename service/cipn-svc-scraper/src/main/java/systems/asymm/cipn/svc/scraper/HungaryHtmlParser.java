/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Pure mapping of the iszlam.com (MME, Hungary) prayer page into {@link TimetableRow}s. The single
 * page carries twelve monthly tables ({@code <table width="475">}), each seven columns wide:
 * {@code Nap | Hajnali (fadzsr) | Napkelte | Deli (dzuhr) | Delutani (aszr) | Esti (maghrib) |
 * Ejszakai (isha)}. Times are 12-hour with no AM/PM marker, so the hour is resolved per column:
 * fajr and sunrise are morning, dhuhr is around noon, and asr/maghrib/isha are afternoon/evening.
 *
 * <p>The tables are NOT in chronological order on the page (October is printed before September), so
 * the month is taken from the inflected Hungarian heading that precedes each table
 * ({@code Januari...}, {@code Oktoberi...}, {@code Szeptemberi...}) rather than from table order.
 * Solar noon is an enrichment, left absent (0).
 */
final class HungaryHtmlParser {

    private static final Pattern TABLE = Pattern.compile(
            "<table[^>]*width=\"475\"[^>]*>(.*?)</table>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern ROW = Pattern.compile(
            "<tr[^>]*>(.*?)</tr>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern CELL = Pattern.compile(
            "<td[^>]*>(.*?)</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})");

    // Folded (lowercase, diacritic-stripped) month stems -> month number, matched whole-word. Full
    // stems (not "jan"/"maj") so a stem cannot hide inside a Hungarian word - e.g. the October DST
    // note "ejszakajan" (ejszakajan) must not read as January, and "majd" must not read as May.
    private static final Map<String, Integer> STEMS = new LinkedHashMap<>();
    static {
        STEMS.put("januar", 1);
        STEMS.put("februar", 2);
        STEMS.put("marcius", 3);
        STEMS.put("aprilis", 4);
        STEMS.put("majus", 5);
        STEMS.put("junius", 6);
        STEMS.put("julius", 7);
        STEMS.put("augusztus", 8);
        STEMS.put("szeptember", 9);
        STEMS.put("oktober", 10);
        STEMS.put("november", 11);
        STEMS.put("december", 12);
    }
    private static final Pattern MONTH_WORD = Pattern.compile(
            "\\b(januar|februar|marcius|aprilis|majus|junius|julius|augusztus|szeptember|oktober|november|december)");

    List<TimetableRow> parse(String html, int year) {
        List<TimetableRow> rows = new ArrayList<>(366);
        Set<Integer> seen = new HashSet<>();
        Matcher tm = TABLE.matcher(html);
        int prevEnd = 0;
        while (tm.find()) {
            int month = monthOf(html.substring(prevEnd, tm.start()));
            prevEnd = tm.end();
            if (month == 0) {
                continue;
            }
            Matcher rm = ROW.matcher(tm.group(1));
            while (rm.find()) {
                List<String> cells = cells(rm.group(1));
                if (cells.size() < 7 || !cells.get(0).matches("\\d{1,2}")) {
                    continue;
                }
                int day = Integer.parseInt(cells.get(0));
                int[][] hm = new int[6][];
                boolean ok = true;
                for (int i = 0; i < 6; i++) {
                    hm[i] = hm(cells.get(i + 1));
                    if (hm[i] == null) {
                        ok = false;
                        break;
                    }
                }
                if (!ok || day < 1 || day > 31 || !seen.add(month * 100 + day)) {
                    continue;
                }
                rows.add(new TimetableRow(month, day,
                        am(hm[0]),     // Hajnali -> fajr
                        am(hm[1]),     // Napkelte -> sunrise
                        noon(hm[2]),   // Deli -> dhuhr
                        pm(hm[3]),     // Delutani -> asr
                        pm(hm[4]),     // Esti -> maghrib
                        pm(hm[5]),     // Ejszakai -> isha
                        0));
            }
        }
        if (rows.size() < 365) {
            throw new ScrapeException("iszlam.com page yielded only " + rows.size() + " rows for " + year);
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return rows;
    }

    private static List<String> cells(String tr) {
        List<String> out = new ArrayList<>(7);
        Matcher cm = CELL.matcher(tr);
        while (cm.find()) {
            out.add(TAG.matcher(cm.group(1)).replaceAll("").strip());
        }
        return out;
    }

    private static int[] hm(String cell) {
        Matcher t = TIME.matcher(cell);
        if (!t.find()) {
            return null;
        }
        return new int[] {Integer.parseInt(t.group(1)), Integer.parseInt(t.group(2))};
    }

    private static int monthOf(String snippet) {
        Matcher m = MONTH_WORD.matcher(fold(snippet));
        int month = 0;
        while (m.find()) {
            month = STEMS.get(m.group(1)); // last month word before the table wins (the heading)
        }
        return month;
    }

    private static String fold(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ö', 'o').replace('ő', 'o')
                .replace('ú', 'u').replace('ü', 'u').replace('ű', 'u');
    }

    private static int am(int[] hm) {
        return (hm[0] == 12 ? 0 : hm[0]) * 60 + hm[1];
    }

    private static int noon(int[] hm) {
        // dhuhr is 11am, 12 noon, or (summer DST) just past 1pm - hours below 7 are the afternoon.
        return (hm[0] < 7 ? hm[0] + 12 : hm[0]) * 60 + hm[1];
    }

    private static int pm(int[] hm) {
        return (hm[0] == 12 ? 12 : hm[0] + 12) * 60 + hm[1];
    }
}
