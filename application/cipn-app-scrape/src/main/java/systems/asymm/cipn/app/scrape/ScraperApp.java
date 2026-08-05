/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.app.scrape;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;

import systems.asymm.cipn.app.scrape.GoldenDiff.ZoneDiff;
import systems.asymm.cipn.fw.common.util.StructuredXmlParser;
import systems.asymm.cipn.fw.common.util.TimetableXml;
import systems.asymm.cipn.fw.contract.access.ContentAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableZone;
import systems.asymm.cipn.fw.contract.scrape.AuthorityScraper;
import systems.asymm.cipn.fw.contract.scrape.ScrapeException;
import systems.asymm.cipn.wiring.accessor.classpath.ClasspathAccessorWiring;
import systems.asymm.cipn.wiring.accessor.file.FileAccessorWiring;
import systems.asymm.cipn.wiring.engine.EngineWiring;
import systems.asymm.cipn.wiring.scraper.ScraperWiring;

/**
 * Off-build entry point for S1 (ADR-0017): scrape one country's authority timetables for a year
 * and write the classpath timetable XML (ADR-0012). Thin - it asks the wiring layer for the
 * authority scraper and the read/write accessors and orchestrates the pass; it declares no impl
 * (svc-*) dependency and instantiates nothing directly.
 *
 * <p>Usage: {@code ScraperApp <cc> <year> <outputDir>}, where {@code outputDir} is the
 * {@code timetable/<year>} directory under the classpath resources. For each zone in the country's
 * stable zone register (read from the latest committed year), it scrapes the year's daily rows,
 * serialises and XSD-validates them, and writes {@code <cc>/zone-<id>.xml}. It carries the stable
 * {@code zone.xml} (geography) over from the base year, merges {@code <cc>} into the year's
 * coverage manifest, and prints a golden-diff of every zone against the committed base table. It
 * hits third-party sites, so it is never part of the Maven build: run it by hand, review the diff,
 * commit the XML.
 */
public final class ScraperApp {

    /** How many years back to look for the stable zone register / golden-diff base. */
    private static final int BASE_LOOKBACK = 3;

    private ScraperApp() {
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: ScraperApp <cc> <year> <outputDir>");
        }
        String cc = args[0].trim().toUpperCase(Locale.ROOT);
        int year = Integer.parseInt(args[1].trim());
        Path outputRoot = Path.of(args[2]);

        AuthorityScraper scraper = ScraperWiring.authorityScraper(cc);
        ScrapeReport report = run(scraper, cc, year, outputRoot);

        System.out.println("Review the golden-diff above, then commit the XML. (make scrape is off-build.)");
        if (report.failures() > 0) {
            System.exit(1);
        }
    }

    /**
     * The scrape orchestration, decoupled from the wiring lookup and {@code System.exit} so it can
     * be driven end-to-end in a test with a stub scraper.
     *
     * @param scraper    the authority scraper to run
     * @param cc         ISO-3166 alpha-2 country code (upper-case)
     * @param year       the Gregorian year to scrape
     * @param outputRoot the {@code timetable/<year>} directory to write into
     * @return a summary: zones written, zones that failed, zones diffed, and the worst drift seen
     */
    public static ScrapeReport run(AuthorityScraper scraper, String cc, int year, Path outputRoot) {
        String ccLower = cc.toLowerCase(Locale.ROOT);
        int baseYear = findBaseYear(cc, year);
        TimetableAccessor base = ClasspathAccessorWiring.timetableAccessor(baseYear);
        List<TimetableZone> zones = base.zones(cc);
        if (zones.isEmpty()) {
            throw new ScrapeException("No committed zone register for " + cc + " (looked back to "
                    + (year - BASE_LOOKBACK) + "). Cannot scrape a country with no zones.");
        }

        ContentAccessor writer = FileAccessorWiring.contentAccessor(outputRoot);
        int expectedDays = Year.of(year).length();

        // Solar noon is a computed (E3) enrichment, not published by the authority; the enricher
        // fills it where the authority did not announce it (ADR-0017).
        SolarNoonEnricher enricher = new SolarNoonEnricher(EngineWiring.astronomyEngine());

        System.out.printf("S1 scrape: %s %d - %d zones, base register from %d%n",
                cc, year, zones.size(), baseYear);

        List<ZoneDiff> diffs = new ArrayList<>();
        int failures = 0;
        int written = 0;
        for (TimetableZone zone : zones) {
            AuthorityTimetable scraped;
            try {
                scraped = enricher.enrich(scraper.scrape(zone, year), zone);
            } catch (ScrapeException e) {
                System.err.printf("  FAIL zone %s: %s%n", zone.id(), e.getMessage());
                failures++;
                continue;
            }
            if (scraped.rows().size() != expectedDays) {
                System.err.printf("  WARN zone %s: %d days scraped, expected %d%n",
                        zone.id(), scraped.rows().size(), expectedDays);
            }
            writer.write(ccLower + "/zone-" + zone.id() + ".xml", TimetableXml.serialize(scraped));
            written++;

            Optional<AuthorityTimetable> committed = base.timetable(cc, zone.id());
            committed.ifPresent(b -> diffs.add(GoldenDiff.compare(zone.id(), b, scraped)));
        }

        // Only claim coverage if at least one zone was actually written; a fully-failed scrape must
        // not leave an orphan zone.xml/index.xml behind (it would falsely advertise the year).
        if (written > 0) {
            carryZoneRegister(baseYear, ccLower, writer);
            mergeCoverageManifest(outputRoot, year, cc, writer);
        }

        int worstDrift = report(diffs);
        System.out.printf("S1 scrape: wrote %d zone tables into %s/%s (%d failed)%n",
                written, outputRoot, ccLower, failures);
        return new ScrapeReport(written, failures, diffs.size(), worstDrift);
    }

    /** The most recent committed year (down to {@code year - BASE_LOOKBACK}) that has zones for cc. */
    private static int findBaseYear(String cc, int year) {
        for (int y = year; y >= year - BASE_LOOKBACK; y--) {
            if (!ClasspathAccessorWiring.timetableAccessor(y).zones(cc).isEmpty()) {
                return y;
            }
        }
        throw new ScrapeException("No committed zone register for " + cc + " within "
                + BASE_LOOKBACK + " years of " + year);
    }

    /** Carry the stable {@code zone.xml} (geography) from the base year into the target year verbatim. */
    private static void carryZoneRegister(int baseYear, String ccLower, ContentAccessor writer) {
        String resource = "timetable/" + baseYear + "/" + ccLower + "/zone.xml";
        try (InputStream in = ScraperApp.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                System.err.printf("  WARN no base %s to carry - write %s/zone.xml by hand%n", resource, ccLower);
                return;
            }
            writer.write(ccLower + "/zone.xml", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ScrapeException("Failed carrying " + resource, e);
        }
    }

    /** Ensure the year's coverage manifest lists cc, preserving any countries already scraped for it. */
    private static void mergeCoverageManifest(Path outputRoot, int year, String cc, ContentAccessor writer) {
        TreeSet<String> countries = new TreeSet<>();
        Path indexPath = outputRoot.resolve("index.xml");
        if (Files.exists(indexPath)) {
            try (InputStream in = Files.newInputStream(indexPath)) {
                Manifest existing = new StructuredXmlParser<>(Manifest.class).parse(in);
                if (existing.country() != null) {
                    for (CountryRef c : existing.country()) {
                        countries.add(c.cc().toUpperCase(Locale.ROOT));
                    }
                }
            } catch (IOException e) {
                throw new ScrapeException("Failed reading " + indexPath, e);
            }
        }
        countries.add(cc);

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!--\n");
        sb.append("  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.\n");
        sb.append("  Coverage manifest for ").append(year).append(": countries with an extracted authority table.\n");
        sb.append("  @author Fahim Farook\n");
        sb.append("-->\n");
        sb.append("<timetable-index year=\"").append(year).append("\">\n");
        for (String c : countries) {
            sb.append("    <country cc=\"").append(c).append("\"/>\n");
        }
        sb.append("</timetable-index>\n");
        writer.write("index.xml", sb.toString());
    }

    private static int report(List<ZoneDiff> diffs) {
        if (diffs.isEmpty()) {
            System.out.println("Golden-diff: no committed base tables to compare against (new country).");
            return 0;
        }
        int clean = 0;
        int worst = 0;
        System.out.println("Golden-diff vs committed base:");
        for (ZoneDiff d : diffs) {
            if (d.clean()) {
                clean++;
            } else {
                System.out.println("  " + d.describe());
            }
            worst = Math.max(worst, d.maxDriftMinutes());
        }
        System.out.printf("  %d/%d zones clean; worst drift across all zones: %d min%n",
                clean, diffs.size(), worst);
        return worst;
    }

    /** Summary of one scrape run: zones written, zones that failed, zones diffed, worst drift (min). */
    public record ScrapeReport(int zonesWritten, int failures, int zonesCompared, int worstDriftMinutes) {
    }

    /** Binding for the year coverage manifest {@code index.xml}. */
    record Manifest(int year, List<CountryRef> country) {
    }

    /** Binding for one {@code <country cc="MY"/>} in the manifest. */
    record CountryRef(String cc) {
    }
}
