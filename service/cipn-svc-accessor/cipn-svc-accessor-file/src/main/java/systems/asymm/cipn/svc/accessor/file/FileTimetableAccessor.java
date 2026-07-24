/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import systems.asymm.cipn.fw.common.util.ResourceParseException;
import systems.asymm.cipn.fw.common.util.ResourceParser;
import systems.asymm.cipn.fw.common.util.StructuredXmlParser;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;
import systems.asymm.cipn.fw.contract.data.TimetableZone;

/**
 * File-backed {@link TimetableAccessor} reading the baked authority timetables from a
 * directory tree ({@code <root>/<cc>/zones.xml} and {@code <root>/<cc>/zone-<id>.xml}).
 *
 * <p>4-Concerns: Location = filesystem (this class, rooted at the timetables directory);
 * Type/Schema = XML (internal, via {@link StructuredXmlParser}); Consumed Type = authority
 * timetables (the contract). Reads only - snapping a location to a zone is the engine's job.
 *
 * @see TimetableAccessor
 */
public class FileTimetableAccessor implements TimetableAccessor {

    private final Path root;
    private final ResourceParser<Zones> zonesParser;
    private final ResourceParser<Timetable> timetableParser;

    /**
     * @param timetablesRoot directory holding one subdirectory per country code
     */
    public FileTimetableAccessor(Path timetablesRoot) {
        this(timetablesRoot, new StructuredXmlParser<>(Zones.class), new StructuredXmlParser<>(Timetable.class));
    }

    public FileTimetableAccessor(Path timetablesRoot, ResourceParser<Zones> zonesParser,
                                 ResourceParser<Timetable> timetableParser) {
        this.root = timetablesRoot;
        this.zonesParser = zonesParser;
        this.timetableParser = timetableParser;
    }

    @Override
    public Set<String> countries() {
        Set<String> codes = new TreeSet<>();
        if (!Files.isDirectory(root)) {
            return codes;
        }
        try (Stream<Path> entries = Files.list(root)) {
            entries.filter(Files::isDirectory)
                    .filter(dir -> Files.isRegularFile(dir.resolve("zones.xml")))
                    .forEach(dir -> codes.add(dir.getFileName().toString().toUpperCase()));
        } catch (IOException e) {
            throw new ResourceParseException("Failed listing timetables under " + root, e);
        }
        return Set.copyOf(codes);
    }

    @Override
    public List<TimetableZone> zones(String countryCode) {
        Path zonesFile = countryDir(countryCode).resolve("zones.xml");
        if (!Files.isRegularFile(zonesFile)) {
            return List.of();
        }
        List<TimetableZone> result = new ArrayList<>();
        for (ZoneElement z : read(zonesFile, zonesParser).zone()) {
            result.add(new TimetableZone(z.id(), z.name(), z.lat(), z.lng()));
        }
        return List.copyOf(result);
    }

    @Override
    public java.util.Optional<AuthorityTimetable> timetable(String countryCode, String zoneId) {
        Path zonesFile = countryDir(countryCode).resolve("zones.xml");
        Path zoneFile = countryDir(countryCode).resolve("zone-" + zoneId + ".xml");
        if (!Files.isRegularFile(zoneFile) || !Files.isRegularFile(zonesFile)) {
            return java.util.Optional.empty();
        }
        Zones meta = read(zonesFile, zonesParser);
        Timetable zone = read(zoneFile, timetableParser);
        List<TimetableRow> rows = new ArrayList<>();
        for (DayElement d : zone.day()) {
            rows.add(new TimetableRow(d.month(), d.day(),
                    d.fajr(), d.sunrise(), d.dhuhr(), d.asr(), d.maghrib(), d.isha()));
        }
        rows.sort(Comparator.comparingInt(TimetableRow::month).thenComparingInt(TimetableRow::day));
        return java.util.Optional.of(new AuthorityTimetable(
                zone.authority(), meta.authorityName(), meta.authorityUrl(),
                zoneId, zone.name(), zone.source(), List.copyOf(rows)));
    }

    private Path countryDir(String countryCode) {
        return root.resolve(countryCode.toLowerCase());
    }

    private static <T> T read(Path file, ResourceParser<T> parser) {
        try (InputStream in = Files.newInputStream(file)) {
            return parser.parse(in);
        } catch (IOException e) {
            throw new ResourceParseException("Failed reading " + file, e);
        }
    }

    /** XML binding for {@code zones.xml}. */
    record Zones(String authority, String authorityName, String authorityUrl, String country, List<ZoneElement> zone) {
    }

    /** XML binding for one {@code <zone .../>}. */
    record ZoneElement(String id, String name, double lat, double lng) {
    }

    /** XML binding for {@code zone-<id>.xml}. */
    record Timetable(String authority, String zone, String name, String source, String unit, List<DayElement> day) {
    }

    /** XML binding for one {@code <day .../>} row. */
    record DayElement(int month, int day, int fajr, int sunrise, int dhuhr, int asr, int maghrib, int isha) {
    }
}
