/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.scrape.AuthorityScraper;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * {@link AuthorityScraper} for Sri Lanka's ACJU, over the twelve monthly PDFs it publishes per zone
 * (ADR-0017). The prayer-times page links 13 zones x 12 months; the file names are inconsistent
 * (month number does not always match the month, and one file is {@code May_010.pdf}), so the index
 * is built by reading the real hrefs and mapping each to (zone, month) by the leading zone number
 * and the month name. The page is fetched once and cached across the run's zones.
 */
public final class AcjuScraper implements AuthorityScraper {

    private static final String COUNTRY = "LK";
    private static final String AUTHORITY = "ACJU";
    private static final String AUTHORITY_NAME = "All Ceylon Jamiyyathul Ulama (ACJU)";
    private static final String AUTHORITY_URL = "https://www.acju.lk/";
    private static final String INDEX_PAGE = "https://www.acju.lk/prayer-times/";

    private static final Pattern HREF = Pattern.compile(
            "href=\"(https://www\\.acju\\.lk/wp-content/uploads/[^\"]*\\.pdf)\"", Pattern.CASE_INSENSITIVE);
    // Standard file: "01-COLOMBO-...-1-Jan.pdf" -> zone 01, month Jan.
    private static final Pattern STD = Pattern.compile("/(\\d{2})-.*-([A-Za-z]{3,4})\\.pdf$");
    // Alternate file: "May_010.pdf" -> month May, zone 10 (trailing digits).
    private static final Pattern ALT = Pattern.compile("/([A-Za-z]{3,4})_0*(\\d{1,2})\\.pdf$");
    private static final Map<String, Integer> MONTHS = new HashMap<>();
    static {
        String[] m = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};
        for (int i = 0; i < 12; i++) {
            MONTHS.put(m[i], i + 1);
        }
    }

    private final FeedReader feed;
    private final AcjuPdfParser parser = new AcjuPdfParser();
    private Map<String, Map<Integer, String>> index; // zone -> month -> url (lazy, cached per run)

    public AcjuScraper() {
        this(new HttpFeedReader());
    }

    AcjuScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        if (index == null) {
            index = buildIndex(feed.get(INDEX_PAGE));
        }
        Map<Integer, String> monthly = index.get(zone.id());
        if (monthly == null || monthly.size() < 12) {
            throw new ScrapeException("ACJU page has "
                    + (monthly == null ? "no" : monthly.size()) + " months for zone " + zone.id());
        }
        List<TimetableRow> rows = new ArrayList<>(366);
        for (int month = 1; month <= 12; month++) {
            List<TimetableRow> monthRows = parser.parse(feed.getBytes(monthly.get(month)), year);
            for (TimetableRow r : monthRows) {
                if (r.month() != month) {
                    throw new ScrapeException("ACJU zone " + zone.id() + " file for month " + month
                            + " contained month " + r.month());
                }
            }
            rows.addAll(monthRows);
        }
        int expected = Year.isLeap(year) ? 366 : 365;
        if (rows.size() != expected) {
            throw new ScrapeException("ACJU zone " + zone.id() + " gave " + rows.size()
                    + " days, expected " + expected);
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }

    /** Read every PDF href on the page and index it by zone and month. */
    static Map<String, Map<Integer, String>> buildIndex(String html) {
        Map<String, Map<Integer, String>> idx = new TreeMap<>();
        Matcher h = HREF.matcher(html);
        while (h.find()) {
            String url = h.group(1);
            String zone = null;
            Integer month = null;
            Matcher s = STD.matcher(url);
            Matcher a = ALT.matcher(url);
            if (s.find()) {
                zone = s.group(1);
                month = MONTHS.get(s.group(2).substring(0, 3).toLowerCase(Locale.ROOT));
            } else if (a.find()) {
                month = MONTHS.get(a.group(1).substring(0, 3).toLowerCase(Locale.ROOT));
                zone = String.format("%02d", Integer.parseInt(a.group(2)));
            }
            if (zone != null && month != null) {
                idx.computeIfAbsent(zone, k -> new TreeMap<>()).putIfAbsent(month, url);
            }
        }
        return idx;
    }
}
