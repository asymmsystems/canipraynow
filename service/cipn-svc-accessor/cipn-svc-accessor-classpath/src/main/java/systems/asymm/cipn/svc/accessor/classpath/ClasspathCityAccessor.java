/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import systems.asymm.cipn.fw.common.util.ResourceParseException;
import systems.asymm.cipn.fw.common.util.ResourceParser;
import systems.asymm.cipn.fw.common.util.StructuredXmlParser;
import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.data.City;

/**
 * Classpath-backed {@link CityAccessor} reading the curated city dataset from the bundled
 * {@code city.xml}.
 *
 * <p>4-Concerns (ADR-032): Location = classpath (this class); Type/Schema = XML (internal,
 * via {@link StructuredXmlParser}); Consumed Type = cities (the contract). Reads only. The
 * city list is curated (not S1 output) and near-static, so it is version-controlled as a
 * resource of this module.
 *
 * @see CityAccessor
 */
public class ClasspathCityAccessor implements CityAccessor {

    private static final String DEFAULT_RESOURCE = "city.xml";

    private final String resourcePath;
    private final ResourceParser<Cities> parser;

    /** Loads from the bundled {@code city.xml}. */
    public ClasspathCityAccessor() {
        this(DEFAULT_RESOURCE, new StructuredXmlParser<>(Cities.class));
    }

    public ClasspathCityAccessor(String resourcePath, ResourceParser<Cities> parser) {
        this.resourcePath = resourcePath;
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
        ClassLoader loader = ClasspathCityAccessor.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new ResourceParseException("Resource not found on classpath: " + resourcePath);
            }
            return parser.parse(in);
        } catch (IOException e) {
            throw new ResourceParseException("Failed reading " + resourcePath, e);
        }
    }

    /** XML binding for {@code city.xml}. */
    record Cities(List<CityElement> city) {
    }

    /** XML binding for one {@code <city .../>}. */
    record CityElement(
            String name, String country, String cc, String slug, String countrySlug,
            double lat, double lng, String tz, boolean page) {
    }
}
