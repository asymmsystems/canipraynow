/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.file;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import systems.asymm.cipn.fw.common.util.ResourceParseException;
import systems.asymm.cipn.fw.common.util.ResourceParser;
import systems.asymm.cipn.fw.common.util.StructuredXmlParser;
import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.data.City;

/**
 * File-backed {@link CityAccessor} reading the city dataset from {@code cities.xml}.
 *
 * <p>4-Concerns: Location = filesystem; Type/Schema = XML (via {@link StructuredXmlParser});
 * Consumed Type = cities. Reads only.
 *
 * @see CityAccessor
 */
public class FileCityAccessor implements CityAccessor {

    private final Path citiesFile;
    private final ResourceParser<Cities> parser;

    public FileCityAccessor(Path citiesFile) {
        this(citiesFile, new StructuredXmlParser<>(Cities.class));
    }

    public FileCityAccessor(Path citiesFile, ResourceParser<Cities> parser) {
        this.citiesFile = citiesFile;
        this.parser = parser;
    }

    @Override
    public List<City> all() {
        Cities doc = read();
        List<City> cities = new ArrayList<>();
        for (CityElement c : doc.city()) {
            cities.add(new City(c.name(), c.country(), c.cc(), c.slug(), c.countrySlug(),
                    c.lat(), c.lng(), c.tz(), c.page()));
        }
        return List.copyOf(cities);
    }

    private Cities read() {
        try (InputStream in = Files.newInputStream(citiesFile)) {
            return parser.parse(in);
        } catch (IOException e) {
            throw new ResourceParseException("Failed reading " + citiesFile, e);
        }
    }

    /** XML binding for {@code cities.xml}. */
    record Cities(List<CityElement> city) {
    }

    /** XML binding for one {@code <city .../>}. */
    record CityElement(
            String name, String country, String cc, String slug, String countrySlug,
            double lat, double lng, String tz, boolean page) {
    }
}
