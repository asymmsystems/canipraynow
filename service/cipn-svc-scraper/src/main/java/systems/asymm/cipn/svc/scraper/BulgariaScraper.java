/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.scrape.AuthorityScraper;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;

/**
 * {@link AuthorityScraper} for Bulgaria's Chief Muftiate, over its monthly HTML timetable (ADR-0017).
 * The timetable sits behind a {@code POST} form ({@code month} + {@code town}), so a full year is 12
 * POST requests for one town; the town code is the zone id (e.g. {@code "sofia"}). Composes a
 * {@link FeedReader} with {@link BulgariaFeedParser}.
 */
public final class BulgariaScraper implements AuthorityScraper {

    private static final String COUNTRY = "BG";
    private static final String AUTHORITY = "BG-Muftiate";
    private static final String AUTHORITY_NAME = "Chief Muftiate of the Muslim Denomination (Bulgaria)";
    private static final String AUTHORITY_URL = "https://www.grandmufti.bg/";
    private static final String ENDPOINT = "https://www.grandmufti.bg/bg/home/vremena-za-namaz.html";

    private final FeedReader feed;
    private final BulgariaFeedParser parser = new BulgariaFeedParser();

    public BulgariaScraper() {
        this(new HttpFeedReader());
    }

    BulgariaScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        if (zone == null || zone.id() == null || zone.id().isBlank()) {
            throw new ScrapeException("A zone with a town code is required");
        }
        List<TimetableRow> rows = new ArrayList<>(366);
        for (int month = 1; month <= 12; month++) {
            String body = "month=" + month + "&town=" + zone.id();
            rows.addAll(parser.parse(feed.post(ENDPOINT, body), month));
        }
        if (rows.isEmpty()) {
            throw new ScrapeException("Bulgarian Muftiate returned no " + year + " days for " + zone.id());
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
