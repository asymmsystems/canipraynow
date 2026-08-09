/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import systems.asymm.cipn.fw.common.util.ResourceParseException;
import systems.asymm.cipn.fw.common.util.ResourceParser;
import systems.asymm.cipn.fw.common.util.StructuredXmlParser;
import systems.asymm.cipn.fw.contract.access.CountryAccessor;
import systems.asymm.cipn.fw.contract.data.CountryDefaults;

/**
 * Classpath-backed {@link CountryAccessor} reading the bundled {@code country.xml}.
 *
 * <p>4-Concerns (ADR-032): Location = classpath (this class); Type/Schema = XML (internal, via
 * {@link StructuredXmlParser}); Consumed Type = country defaults (the contract). Reads the file
 * once at construction and serves an immutable view; an unknown country yields empty-string
 * defaults with the standard method phrase.
 *
 * @see CountryAccessor
 */
public class ClasspathCountryAccessor implements CountryAccessor {

    private static final String DEFAULT_RESOURCE = "country.xml";
    private static final String DEFAULT_METHOD_PHRASE = "a standard convention";
    private static final String DEFAULT_METHOD_KEY = "MuslimWorldLeague";

    private final Map<String, CountryDefaults> byCode;

    /** Loads from the bundled {@code country.xml}. */
    public ClasspathCountryAccessor() {
        this(DEFAULT_RESOURCE, new StructuredXmlParser<>(Countries.class));
    }

    public ClasspathCountryAccessor(String resourcePath, ResourceParser<Countries> parser) {
        Countries doc = load(resourcePath, parser);
        Map<String, CountryDefaults> map = new LinkedHashMap<>();
        if (doc.country() != null) {
            for (CountryElement c : doc.country()) {
                map.put(c.cc().toUpperCase(Locale.ROOT),
                        new CountryDefaults(c.cc(), nullToEmpty(c.authority()),
                                nullToEmpty(c.method()), methodKey(c.methodKey()), nullToEmpty(c.madhhab()),
                                nullToUnknown(c.redistributable()), nullToEmpty(c.methodNote())));
            }
        }
        this.byCode = Map.copyOf(map);
    }

    @Override
    public CountryDefaults defaults(String countryCode) {
        CountryDefaults found = byCode.get(countryCode.toUpperCase(Locale.ROOT));
        return found != null
                ? found
                : new CountryDefaults(countryCode, "", DEFAULT_METHOD_PHRASE, DEFAULT_METHOD_KEY, "", "unknown", "");
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** A missing redistributable answer is conservatively read as "unknown". */
    private static String nullToUnknown(String s) {
        return s == null ? "unknown" : s;
    }

    /** A missing key falls back to the global default convention (Muslim World League). */
    private static String methodKey(String key) {
        return key == null || key.isBlank() ? DEFAULT_METHOD_KEY : key;
    }

    private static Countries load(String resourcePath, ResourceParser<Countries> parser) {
        ClassLoader loader = ClasspathCountryAccessor.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new ResourceParseException("Resource not found on classpath: " + resourcePath);
            }
            return parser.parse(in);
        } catch (IOException e) {
            throw new ResourceParseException("Failed reading " + resourcePath, e);
        }
    }

    /** XML binding for {@code country.xml}. */
    record Countries(List<CountryElement> country) {
    }

    /**
     * XML binding for one {@code <country .../>}. The core five (cc, authority, method, methodKey,
     * madhhab) drive the city-page presentation defaults; the authority-first register fields
     * (has-authority, feed, redistributable, confidence, ...) are parsed so the strict Jackson
     * reader accepts them and the XSD-valid register round-trips. They are not yet consumed by
     * {@link CountryDefaults} (redistribution feature-flag wiring is T-27 follow-up); capturing them
     * here keeps {@code country.xml} the single register without a second file.
     */
    record CountryElement(String cc, Boolean hasAuthority, String authority, String authorityUrl,
            String method, String methodKey, String madhhab, String servedBy, String feed,
            String redistributable, String redistributableBasis, String confidence,
            String sourceUrl, String note, String methodNote) {
    }
}
