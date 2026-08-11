/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Pure mapping of muftiyat.kg's calendar page into {@link TimetableRow}s. The page embeds the whole
 * requested range as a JavaScript array of day objects, each a flat record:
 * {@code {'date':'2026-01-01','fajr':'06:50','sunrise':'08:32','dhuhr':'13:05','asr':'15:57',
 * 'sunset':'17:37','maghrib':'17:44','isha':'19:08','tahajjud':'02:28'}}. Times are local Bishkek
 * 24-hour clock (no DST). Sunset and tahajjud are dropped. Solar noon is enrichment, left absent (0).
 */
final class KyrgyzstanFeedParser {

    private static final Pattern DAY_OBJECT = Pattern.compile("\\{[^{}]*?'date'[^{}]*\\}");
    private static final Pattern ISO_DATE = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");

    List<TimetableRow> parse(String html, int year) {
        List<TimetableRow> rows = new ArrayList<>(366);
        Set<Integer> seen = new HashSet<>();
        Matcher o = DAY_OBJECT.matcher(html);
        while (o.find()) {
            String obj = o.group();
            String date = field(obj, "date");
            if (date == null) {
                continue;
            }
            Matcher d = ISO_DATE.matcher(date);
            if (!d.find() || Integer.parseInt(d.group(1)) != year) {
                continue;
            }
            int month = Integer.parseInt(d.group(2));
            int day = Integer.parseInt(d.group(3));
            if (!seen.add(month * 100 + day)) {
                continue;
            }
            Integer fajr = hm(field(obj, "fajr"));
            Integer sunrise = hm(field(obj, "sunrise"));
            Integer dhuhr = hm(field(obj, "dhuhr"));
            Integer asr = hm(field(obj, "asr"));
            Integer maghrib = hm(field(obj, "maghrib"));
            Integer isha = hm(field(obj, "isha"));
            if (fajr == null || sunrise == null || dhuhr == null || asr == null
                    || maghrib == null || isha == null) {
                continue;
            }
            rows.add(new TimetableRow(month, day, fajr, sunrise, dhuhr, asr, maghrib, isha, 0));
        }
        if (rows.size() < 365) {
            throw new ScrapeException("muftiyat.kg gave only " + rows.size() + " days for " + year);
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return rows;
    }

    private static String field(String obj, String name) {
        Matcher m = Pattern.compile("'" + name + "'\\s*:\\s*'([^']*)'").matcher(obj);
        return m.find() ? m.group(1) : null;
    }

    private static Integer hm(String value) {
        if (value == null) {
            return null;
        }
        Matcher m = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(value);
        return m.find() ? Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2)) : null;
    }
}
