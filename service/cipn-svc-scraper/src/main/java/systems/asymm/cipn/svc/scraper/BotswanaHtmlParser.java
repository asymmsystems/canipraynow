/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import systems.asymm.cipn.fw.contract.data.TimetableRow;

/**
 * Pure mapping of the BMA (Botswana) prayer page into {@link TimetableRow}s for one city. The single
 * page holds every city's twelve monthly tables in {@code <div id="{city}-{month}">} blocks; the
 * rows are {@code <D-Mon> <Fajr> <Sunrise> <Zawwal> <Shafi Asr> <Hanafi Asr> <Sunset> <Maghrib>
 * <Isha>} in 12-hour time with no AM/PM marker, so the hour is resolved per column.
 *
 * <p>The table has no explicit dhuhr column, so dhuhr is taken from Zawwal (solar noon, when dhuhr
 * enters); asr is the Shafi column (the earlier, always-valid time) and the Hanafi column is dropped;
 * maghrib is the Maghrib column (not the earlier Sunset column). Both choices are recorded in
 * task/scholar-verification.md. Solar noon is an enrichment, left absent (0).
 */
final class BotswanaHtmlParser {

    private static final Pattern TABLE = Pattern.compile("<table[^>]*>(.*?)</table>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern ROW = Pattern.compile("<tr[^>]*>(.*?)</tr>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern CELL = Pattern.compile("<t[dh][^>]*>(.*?)</t[dh]>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern DATE = Pattern.compile("^(\\d{1,2})-([A-Za-z]{3})");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{2})");
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("jan", 1), Map.entry("feb", 2), Map.entry("mar", 3), Map.entry("apr", 4),
            Map.entry("may", 5), Map.entry("jun", 6), Map.entry("jul", 7), Map.entry("aug", 8),
            Map.entry("sep", 9), Map.entry("oct", 10), Map.entry("nov", 11), Map.entry("dec", 12));

    /**
     * @param html    the whole BMA page
     * @param cityId  the div-id spelling for the city (e.g. {@code gabarone}, {@code Francistown})
     */
    List<TimetableRow> parse(String html, String cityId, int year) {
        List<TimetableRow> rows = new ArrayList<>(366);
        Set<Integer> seen = new HashSet<>();
        Pattern block = Pattern.compile(
                "id=\"" + Pattern.quote(cityId) + "-[a-zA-Z]+\"(.*?)</div>",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher b = block.matcher(html);
        while (b.find()) {
            Matcher t = TABLE.matcher(b.group(1));
            if (t.find()) {
                addTable(rows, seen, t.group(1), year);
            }
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return rows;
    }

    private void addTable(List<TimetableRow> rows, Set<Integer> seen, String table, int year) {
        Matcher r = ROW.matcher(table);
        while (r.find()) {
            List<String> c = new ArrayList<>();
            Matcher cm = CELL.matcher(r.group(1));
            while (cm.find()) {
                c.add(TAG.matcher(cm.group(1)).replaceAll("").replace("&nbsp;", " ").strip());
            }
            if (c.size() < 9) {
                continue;
            }
            Matcher dm = DATE.matcher(c.get(0));
            if (!dm.find()) {
                continue; // header row
            }
            Integer month = MONTHS.get(dm.group(2).toLowerCase(Locale.ROOT));
            if (month == null) {
                continue;
            }
            int day = Integer.parseInt(dm.group(1));
            if (day < 1 || day > Month.of(month).length(Year.isLeap(year)) || !seen.add(month * 100 + day)) {
                continue; // drop a stray day the calendar does not have this year (e.g. 29 Feb)
            }
            int[] fajr = hm(c.get(1));
            int[] sunrise = hm(c.get(2));
            int[] zawwal = hm(c.get(3));
            int[] shafiAsr = hm(c.get(4));
            int[] maghrib = hm(c.get(7));
            int[] isha = hm(c.get(8));
            if (fajr == null || sunrise == null || zawwal == null || shafiAsr == null
                    || maghrib == null || isha == null) {
                continue;
            }
            rows.add(new TimetableRow(month, day,
                    am(fajr), am(sunrise), noon(zawwal), pm(shafiAsr), pm(maghrib), pm(isha), 0));
        }
    }

    private static int[] hm(String cell) {
        Matcher m = TIME.matcher(cell);
        return m.find() ? new int[] {Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))} : null;
    }

    private static int am(int[] hm) {
        return (hm[0] == 12 ? 0 : hm[0]) * 60 + hm[1];
    }

    private static int noon(int[] hm) {
        return (hm[0] < 7 ? hm[0] + 12 : hm[0]) * 60 + hm[1];
    }

    private static int pm(int[] hm) {
        return (hm[0] == 12 ? 12 : hm[0] + 12) * 60 + hm[1];
    }
}
