/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.app.scrape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.List;

import systems.asymm.cipn.app.scrape.ScraperApp.ScrapeReport;
import systems.asymm.cipn.fw.common.util.TimetableXml;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.scrape.AuthorityScraper;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;
import systems.asymm.cipn.wiring.accessor.classpath.ClasspathAccessorWiring;

/**
 * Exercises the whole S1 orchestration offline, with no network: find the base register, scrape
 * every zone, serialise + XSD-validate, write the tree, carry the zone register, merge the
 * coverage manifest, and golden-diff. The stub scraper returns the <em>committed</em> MY/2026
 * table for each zone, so a faithful pipeline must write valid XML for every zone and report a
 * perfectly clean diff (zero drift, nothing missing). The live e-Solat fetch is covered by
 * {@code JakimScraperTest}; here we prove the machinery around it.
 */
class ScraperAppIntegrationTest {

    /** A scraper that echoes the committed base table - the identity case, so drift must be zero. */
    private static final class BaseEchoScraper implements AuthorityScraper {
        private final TimetableAccessor base = ClasspathAccessorWiring.timetableAccessor(2026);

        @Override
        public String countryCode() {
            return "MY";
        }

        @Override
        public AuthorityTimetable scrape(TimetableZone zone, int year) {
            return base.timetable("MY", zone.id())
                    .orElseThrow(() -> new ScrapeException("no committed base for zone " + zone.id()));
        }
    }

    /**
     * Like {@link BaseEchoScraper} but with solar noon stripped (set to 0), mimicking a real feed
     * that publishes only the six prayer times. The enricher must then recompute solar noon and
     * reproduce the committed values - so the golden-diff proves the enrichment, not just the copy.
     */
    private static final class SolarNoonStrippedScraper implements AuthorityScraper {
        private final TimetableAccessor base = ClasspathAccessorWiring.timetableAccessor(2026);

        @Override
        public String countryCode() {
            return "MY";
        }

        @Override
        public AuthorityTimetable scrape(TimetableZone zone, int year) {
            AuthorityTimetable t = base.timetable("MY", zone.id())
                    .orElseThrow(() -> new ScrapeException("no committed base for zone " + zone.id()));
            List<TimetableRow> stripped = t.rows().stream()
                    .map(r -> new TimetableRow(r.month(), r.day(),
                            r.fajr(), r.sunrise(), r.dhuhr(), r.asr(), r.maghrib(), r.isha(), 0))
                    .toList();
            return new AuthorityTimetable(t.authority(), t.authorityName(), t.authorityUrl(),
                    t.zoneId(), t.zoneName(), t.source(), t.year(), stripped);
        }
    }

    @Test
    void scrapesEveryZoneWritesValidXmlAndDiffsClean(@TempDir Path outputRoot) throws IOException {
        int myZoneCount = ClasspathAccessorWiring.timetableAccessor(2026).zones("MY").size();
        assertTrue(myZoneCount > 0, "precondition: committed MY zone register exists");

        ScrapeReport report = ScraperApp.run(new BaseEchoScraper(), "MY", 2026, outputRoot);

        // Every zone written, none failed, and - echoing the base - a perfectly clean diff.
        assertEquals(myZoneCount, report.zonesWritten());
        assertEquals(0, report.failures());
        assertEquals(myZoneCount, report.zonesCompared());
        assertEquals(0, report.worstDriftMinutes());

        // A representative zone table exists and is schema-valid on its own.
        Path wly01 = outputRoot.resolve("my/zone-WLY01.xml");
        assertTrue(Files.exists(wly01), "zone-WLY01.xml written");
        TimetableXml.validate(Files.readString(wly01, StandardCharsets.UTF_8));

        // The stable zone register was carried over, and the coverage manifest lists MY.
        assertTrue(Files.exists(outputRoot.resolve("my/zone.xml")), "zone.xml carried over");
        String manifest = Files.readString(outputRoot.resolve("index.xml"), StandardCharsets.UTF_8);
        assertTrue(manifest.contains("year=\"2026\""), "manifest is for the scraped year");
        assertTrue(manifest.contains("cc=\"MY\""), "manifest lists the scraped country");
    }

    @Test
    void aFullyFailedScrapeLeavesNoOrphanCoverage(@TempDir Path outputRoot) {
        // Every zone throws (e.g. the year isn't published yet). The run must fail loudly and write
        // nothing - no zone.xml or index.xml that would falsely advertise the year as covered.
        AuthorityScraper allFail = new AuthorityScraper() {
            @Override
            public String countryCode() {
                return "MY";
            }

            @Override
            public AuthorityTimetable scrape(TimetableZone zone, int year) {
                throw new ScrapeException("no data for " + zone.id());
            }
        };

        ScrapeReport report = ScraperApp.run(allFail, "MY", 2026, outputRoot);

        assertEquals(0, report.zonesWritten());
        assertTrue(report.failures() > 0);
        assertFalse(Files.exists(outputRoot.resolve("index.xml")), "no manifest on a failed scrape");
        assertFalse(Files.exists(outputRoot.resolve("my/zone.xml")), "no zone register on a failed scrape");
    }

    @Test
    void enrichesStrippedSolarNoonBackToCommittedValues(@TempDir Path outputRoot) {
        // Feed publishes six times only; the enricher recomputes solar noon. Because the golden-diff
        // now includes solar-noon, a zero worst-drift proves the enrichment reproduces the committed
        // (astronomically computed) values to the minute across all 26k+ rows.
        ScrapeReport report = ScraperApp.run(new SolarNoonStrippedScraper(), "MY", 2026, outputRoot);

        assertEquals(0, report.failures());
        assertEquals(0, report.worstDriftMinutes(),
                "recomputed solar noon must match the committed values to the minute");
    }
}
