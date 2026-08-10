/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import systems.asymm.cipn.fw.common.util.ResourceParseException;
import systems.asymm.cipn.fw.common.util.ResourceParser;
import systems.asymm.cipn.fw.common.util.StructuredXmlParser;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;

/**
 * Classpath-backed {@link TimetableAccessor} for one year. Reads the bundled authority
 * timetables from {@code timetable/<year>/<cc>/zone.xml} (the zone index) and
 * {@code timetable/<year>/<cc>/zone-<id>.xml} (one zone's annual table), with the set of
 * covered countries listed in {@code timetable/<year>/index.xml}.
 *
 * <p>4-Concerns (ADR-032): Location = classpath (this class); Type/Schema = XML (internal,
 * via {@link StructuredXmlParser}); Consumed Type = authority timetables (the contract). Reads
 * only - snapping a location to a zone is the engine's job. The year is bound at construction,
 * so the contract stays year-agnostic; the generator composes one accessor per year it bakes.
 *
 * <p>Coverage is discovered via the {@code index.xml} manifest rather than by listing a
 * classpath directory, so it works identically from exploded classes and from a packaged jar.
 * S1 writes the manifest alongside the tables it extracts.
 *
 * @see TimetableAccessor
 */
public class ClasspathTimetableAccessor implements TimetableAccessor {

    private final int year;
    private final String root;
    private final ResourceParser<Index> indexParser;
    private final ResourceParser<Zones> zonesParser;
    private final ResourceParser<Timetable> timetableParser;

    /** For the given year, reading the bundled {@code timetable/<year>/...} tree. */
    public ClasspathTimetableAccessor(int year) {
        this(year, new StructuredXmlParser<>(Index.class),
                new StructuredXmlParser<>(Zones.class), new StructuredXmlParser<>(Timetable.class));
    }

    public ClasspathTimetableAccessor(int year, ResourceParser<Index> indexParser,
                                      ResourceParser<Zones> zonesParser, ResourceParser<Timetable> timetableParser) {
        this.year = year;
        this.root = "timetable/" + year;
        this.indexParser = indexParser;
        this.zonesParser = zonesParser;
        this.timetableParser = timetableParser;
    }

    @Override
    public Set<String> countries() {
        InputStream indexXml = open(root + "/index.xml");
        if (indexXml == null) {
            return Set.of();
        }
        Index index = parse(indexXml, indexParser, root + "/index.xml");
        Set<String> codes = new TreeSet<>();
        if (index.country() != null) {
            for (CountryRef c : index.country()) {
                codes.add(c.cc().toUpperCase(Locale.ROOT));
            }
        }
        return Set.copyOf(codes);
    }

    @Override
    public List<TimetableZone> zones(String countryCode) {
        InputStream zonesXml = open(zonesPath(countryCode));
        if (zonesXml == null) {
            return List.of();
        }
        Zones meta = parse(zonesXml, zonesParser, zonesPath(countryCode));
        List<TimetableZone> result = new ArrayList<>();
        for (ZoneElement z : meta.zone()) {
            result.add(new TimetableZone(z.id(), z.name(), z.lat(), z.lng()));
        }
        return List.copyOf(result);
    }

    @Override
    public Optional<AuthorityTimetable> timetable(String countryCode, String zoneId) {
        InputStream zonesXml = open(zonesPath(countryCode));
        InputStream zoneXml = open(zonePath(countryCode, zoneId));
        if (zonesXml == null || zoneXml == null) {
            close(zonesXml);
            close(zoneXml);
            return Optional.empty();
        }
        Zones meta = parse(zonesXml, zonesParser, zonesPath(countryCode));
        Timetable zone = parse(zoneXml, timetableParser, zonePath(countryCode, zoneId));
        List<TimetableRow> rows = new ArrayList<>();
        for (DayElement d : zone.day()) {
            rows.add(new TimetableRow(d.month(), d.day(),
                    d.fajr(), d.sunrise(), d.dhuhr(), d.asr(), d.maghrib(), d.isha(), d.solarNoon()));
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return Optional.of(new AuthorityTimetable(
                zone.authority(), meta.authorityName(), meta.authorityUrl(),
                zoneId, zone.name(), zone.source(), year, List.copyOf(rows), zone.note()));
    }

    private String zonesPath(String countryCode) {
        return root + "/" + countryCode.toLowerCase(Locale.ROOT) + "/zone.xml";
    }

    private String zonePath(String countryCode, String zoneId) {
        return root + "/" + countryCode.toLowerCase(Locale.ROOT) + "/zone-" + zoneId + ".xml";
    }

    private static InputStream open(String resourcePath) {
        return ClasspathTimetableAccessor.class.getClassLoader().getResourceAsStream(resourcePath);
    }

    private static <T> T parse(InputStream in, ResourceParser<T> parser, String path) {
        try (InputStream stream = in) {
            return parser.parse(stream);
        } catch (IOException e) {
            throw new ResourceParseException("Failed reading " + path, e);
        }
    }

    private static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
                // best-effort close of an unused stream
            }
        }
    }

    /** XML binding for {@code index.xml}: the countries covered for a year. */
    record Index(int year, List<CountryRef> country) {
    }

    /** XML binding for one {@code <country cc="LK"/>} in {@code index.xml}. */
    record CountryRef(String cc) {
    }

    /** XML binding for {@code zone.xml}. */
    record Zones(String authority, String authorityName, String authorityUrl, String country, List<ZoneElement> zone) {
    }

    /** XML binding for one {@code <zone .../>} in {@code zone.xml}. */
    record ZoneElement(String id, String name, double lat, double lng) {
    }

    /** XML binding for {@code zone-<id>.xml}. */
    record Timetable(String authority, String zone, String name, String source, String unit, String note,
            List<DayElement> day) {
    }

    /** XML binding for one {@code <day .../>} row. */
    record DayElement(
            int month, int day, int fajr, int sunrise, int dhuhr, int asr, int maghrib, int isha, int solarNoon) {
    }
}
