/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Pure mapping of the Bulgarian Chief Muftiate's monthly HTML timetable into {@link TimetableRow}s -
 * no I/O, so it is unit-tested against a captured page. Each month page is a table whose data rows
 * are seven {@code <td>} cells: the day of the month, then Зора, Изгрев, Обяд, Следобяд, Залез, Нощ
 * (dawn/sunrise/noon/afternoon/sunset/night) as {@code H:MM}, which map to fajr, sunrise, dhuhr, asr,
 * maghrib, isha. The month is supplied by the caller (one request per month); solar noon is an
 * enrichment, left absent (0). A row is recognised structurally - a 1-31 day cell followed by six
 * {@code H:MM} cells - so surrounding layout/select cells are ignored.
 */
final class BulgariaFeedParser {

    private static final Pattern CELL = Pattern.compile("<td[^>]*>\\s*([^<]*?)\\s*</td>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME = Pattern.compile("^(\\d{1,2}):(\\d{2})$");
    private static final Pattern DAY = Pattern.compile("^(\\d{1,2})$");

    /**
     * @param html  one month's page
     * @param month the month this page is for (1-12), supplied by the scraper
     * @return that month's rows (day + six times); empty if the page had no table
     */
    List<TimetableRow> parse(String html, int month) {
        if (html == null || html.isBlank()) {
            throw new ScrapeException("Empty Bulgarian Muftiate response");
        }
        List<String> cells = new ArrayList<>();
        Matcher cm = CELL.matcher(html);
        while (cm.find()) {
            cells.add(cm.group(1).trim());
        }

        List<TimetableRow> rows = new ArrayList<>();
        for (int i = 0; i + 6 < cells.size(); i++) {
            Matcher dm = DAY.matcher(cells.get(i));
            if (!dm.find()) {
                continue;
            }
            int day = Integer.parseInt(dm.group(1));
            if (day < 1 || day > 31) {
                continue;
            }
            int[] times = new int[6];
            boolean allTimes = true;
            for (int k = 0; k < 6 && allTimes; k++) {
                Integer m = minute(cells.get(i + 1 + k));
                if (m == null) {
                    allTimes = false;
                } else {
                    times[k] = m;
                }
            }
            if (!allTimes) {
                continue;
            }
            rows.add(new TimetableRow(month, day,
                    times[0], times[1], times[2], times[3], times[4], times[5], 0));
            i += 6;
        }
        return rows;
    }

    /** {@code H:MM} to minutes from midnight, or null if the cell is not a time. */
    private static Integer minute(String cell) {
        Matcher m = TIME.matcher(cell);
        if (!m.find()) {
            return null;
        }
        int total = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
        return (total >= 0 && total <= 1439) ? total : null;
    }
}
