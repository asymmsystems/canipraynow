/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.scraper;

import java.util.List;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.scrape.AuthorityScraper;

/**
 * {@link AuthorityScraper} for Montenegro's Islamic Community (vaktija.me), over its per-city annual
 * PDF (ADR-0017). The generator takes the city name as a query parameter, so the zone id carries the
 * vaktija.me city name (e.g. {@code Podgorica}) and one request returns that city's whole year.
 */
public final class MontenegroScraper implements AuthorityScraper {

    private static final String COUNTRY = "ME";
    private static final String AUTHORITY = "Vaktija.me";
    private static final String AUTHORITY_NAME = "Islamska zajednica u Crnoj Gori (vaktija.me)";
    private static final String AUTHORITY_URL = "https://vaktija.me/";
    private static final String PDF = "https://vaktija.me/vaktija/data/pdf/generate/generatePDFYearly.php?=&city=-1&cityName=";

    private final FeedReader feed;
    private final MontenegroPdfParser parser = new MontenegroPdfParser();

    public MontenegroScraper() {
        this(new HttpFeedReader());
    }

    MontenegroScraper(FeedReader feed) {
        this.feed = feed;
    }

    @Override
    public String countryCode() {
        return COUNTRY;
    }

    @Override
    public AuthorityTimetable scrape(TimetableZone zone, int year) {
        List<TimetableRow> rows = parser.parse(feed.getBytes(PDF + zone.id()), year);
        return new AuthorityTimetable(AUTHORITY, AUTHORITY_NAME, AUTHORITY_URL,
                zone.id(), zone.name(), AUTHORITY_URL, year, rows);
    }
}
