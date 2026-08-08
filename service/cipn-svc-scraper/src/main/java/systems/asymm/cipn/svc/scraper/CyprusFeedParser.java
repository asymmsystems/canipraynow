/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * Pure mapping of the Turkish-Cypriot Presidency of Religious Affairs (KKTC Din İşleri Başkanlığı)
 * yearly HTML takvim into {@link TimetableRow}s - no I/O, so it is unit-tested against a captured
 * page. The table rows are {@code <td class='tabletd'>} cells in groups of seven: a Turkish date
 * ({@code "01 Ocak 2026 Perşembe"}) followed by İmsak, Güneş, Öğle, İkindi, Akşam, Yatsı as
 * {@code HH:MM}. Those map to fajr (İmsak is the Diyanet dawn/fajr time), sunrise, dhuhr, asr,
 * maghrib, isha. Solar noon is an E3 enrichment, left absent (0). Rows outside the requested year
 * are dropped; the result is sorted in calendar order.
 */
final class CyprusFeedParser {

    /** Data cells carry class="tabletd"; header cells do not, so this matches only data. */
    private static final Pattern CELL = Pattern.compile("class=['\"]tabletd['\"][^>]*>([^<]*)</td>",
            Pattern.CASE_INSENSITIVE);
    /** A date cell starts "DD <TurkishMonth> YYYY ..."; a time cell does not match this. */
    private static final Pattern DATE = Pattern.compile("^\\s*(\\d{1,2})\\s+(\\S+)\\s+(\\d{4})");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2})[:.](\\d{2})");

    /** Turkish month names, ASCII-folded and lower-cased (see {@link #fold}). */
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("ocak", 1), Map.entry("subat", 2), Map.entry("mart", 3), Map.entry("nisan", 4),
            Map.entry("mayis", 5), Map.entry("haziran", 6), Map.entry("temmuz", 7), Map.entry("agustos", 8),
            Map.entry("eylul", 9), Map.entry("ekim", 10), Map.entry("kasim", 11), Map.entry("aralik", 12));

    List<TimetableRow> parse(String html, int year) {
        if (html == null || html.isBlank()) {
            throw new ScrapeException("Empty KKTC takvim response");
        }
        List<String> cells = new ArrayList<>();
        Matcher cm = CELL.matcher(html);
        while (cm.find()) {
            cells.add(cm.group(1).trim());
        }

        List<TimetableRow> rows = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) {
            Matcher dm = DATE.matcher(cells.get(i));
            if (!dm.find()) {
                continue;
            }
            if (i + 6 >= cells.size()) {
                break;
            }
            Integer month = MONTHS.get(fold(dm.group(2)));
            if (month == null || Integer.parseInt(dm.group(3)) != year) {
                i += 6;
                continue;
            }
            int day = Integer.parseInt(dm.group(1));
            rows.add(new TimetableRow(month, day,
                    minute(cells.get(i + 1)),   // İmsak -> fajr
                    minute(cells.get(i + 2)),   // Güneş -> sunrise
                    minute(cells.get(i + 3)),   // Öğle  -> dhuhr
                    minute(cells.get(i + 4)),   // İkindi -> asr
                    minute(cells.get(i + 5)),   // Akşam -> maghrib
                    minute(cells.get(i + 6)),   // Yatsı -> isha
                    0));
            i += 6;
        }
        if (rows.isEmpty()) {
            throw new ScrapeException("No KKTC takvim rows for " + year);
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return rows;
    }

    private static int minute(String cell) {
        Matcher m = TIME.matcher(cell);
        if (!m.find()) {
            throw new ScrapeException("Malformed KKTC time '" + cell + "'");
        }
        int total = Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
        if (total < 0 || total > 1439) {
            throw new ScrapeException("KKTC time '" + cell + "' out of range");
        }
        return total;
    }

    /** Fold Turkish letters to ASCII and lower-case, so month matching is encoding-robust. */
    private static String fold(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(switch (c) {
                case 'ş', 'Ş' -> 's';
                case 'ğ', 'Ğ' -> 'g';
                case 'ı', 'İ', 'I' -> 'i';
                case 'ü', 'Ü' -> 'u';
                case 'ö', 'Ö' -> 'o';
                case 'ç', 'Ç' -> 'c';
                default -> Character.toLowerCase(c);
            });
        }
        return out.toString();
    }
}
