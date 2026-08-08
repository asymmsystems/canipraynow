/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.scraper;

import java.util.Locale;

import systems.asymm.cipn.fw.contract.scrape.AuthorityScraper;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;
import systems.asymm.cipn.svc.scraper.BosniaScraper;
import systems.asymm.cipn.svc.scraper.BulgariaScraper;
import systems.asymm.cipn.svc.scraper.CyprusScraper;
import systems.asymm.cipn.svc.scraper.JakimScraper;

/**
 * Plain-Java wiring for the S1 authority scrapers: the sole place that instantiates an
 * {@link AuthorityScraper}, keeping {@code cipn-svc-scraper} out of the app (ADR-0017). One
 * authority is wired today - MY/JAKIM; each new authority in the register (T-01) adds one
 * {@code case} here alongside its new {@code *Scraper} implementation.
 */
public final class ScraperWiring {

    private ScraperWiring() {
    }

    /**
     * Resolve the scraper for a country.
     *
     * @param countryCode ISO-3166 alpha-2 code (case-insensitive)
     * @return the authority scraper for that country
     * @throws ScrapeException if no scraper is wired for the country yet
     */
    public static AuthorityScraper authorityScraper(String countryCode) {
        String cc = countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
        return switch (cc) {
            case "MY" -> new JakimScraper();
            case "CY" -> new CyprusScraper();
            case "BG" -> new BulgariaScraper();
            case "BA" -> new BosniaScraper();
            default -> throw new ScrapeException("No authority scraper wired for country '" + countryCode
                    + "'. Wired: MY (JAKIM), CY (KKTC), BG (Muftiate), BA (Rijaset). See ADR-0017 and the T-01 authority register.");
        };
    }
}
