/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import systems.asymm.cipn.fw.common.util.PropertiesParser;
import systems.asymm.cipn.fw.common.util.ResourceParseException;
import systems.asymm.cipn.fw.common.util.ResourceParser;
import systems.asymm.cipn.fw.contract.access.CalculationMethodAccessor;
import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.EventOffsets;
import systems.asymm.cipn.fw.contract.data.HighLatitudeRule;
import systems.asymm.cipn.fw.contract.data.Madhab;
import systems.asymm.cipn.fw.contract.data.Rounding;
import systems.asymm.cipn.fw.contract.data.Shafaq;

/**
 * Classpath-backed {@link CalculationMethodAccessor}.
 *
 * <p>4-Concerns: Location = classpath (this class); Type/Schema = Properties (internal,
 * via {@link PropertiesParser}); Consumed Type = calculation-method parameters (the
 * contract). Reads {@code calculation-methods.properties} once at construction and
 * serves an immutable view; only non-default fields need to appear in the file.
 *
 * <p>Keys follow the asymm convention (lower-case, dot-namespaced, kebab-case):
 * {@code calculation-method.<slug>.<field>}. A caller's CamelCase method id
 * (e.g. {@code MuslimWorldLeague}) is normalized to its slug ({@code muslim-world-league})
 * for lookup.
 *
 * @see CalculationMethodAccessor
 */
public class ClasspathCalculationMethodAccessor implements CalculationMethodAccessor {

    private static final String DEFAULT_RESOURCE = "calculation-methods.properties";
    private static final String NAMESPACE = "calculation-method.";
    private static final String DEFAULT_KEY = NAMESPACE + "default";

    private final Map<String, String> table;
    private final String defaultSlug;
    private final Set<String> slugs;

    /** Loads the registry from the bundled {@code calculation-methods.properties}. */
    public ClasspathCalculationMethodAccessor() {
        this(DEFAULT_RESOURCE, new PropertiesParser());
    }

    /**
     * Loads the registry from a classpath resource.
     *
     * @param resourcePath classpath path to the properties table
     * @param parser       the properties parser (Type/Schema concern)
     */
    public ClasspathCalculationMethodAccessor(String resourcePath, ResourceParser<Map<String, String>> parser) {
        this.table = load(resourcePath, parser);
        this.defaultSlug = text(DEFAULT_KEY, "muslim-world-league");
        this.slugs = extractSlugs(table);
    }

    @Override
    public Optional<CalculationParameters> parameters(String methodKey, Madhab madhab) {
        String slug = slug(methodKey);
        return slugs.contains(slug) ? Optional.of(build(slug, madhab)) : Optional.empty();
    }

    @Override
    public CalculationParameters parametersOrDefault(String methodKey, Madhab madhab) {
        return parameters(methodKey, madhab).orElseGet(() -> build(defaultSlug, madhab));
    }

    @Override
    public Set<String> methodKeys() {
        return slugs;
    }

    private CalculationParameters build(String slug, Madhab madhab) {
        String base = NAMESPACE + slug + ".";
        Rounding rounding = Rounding.valueOf(text(base + "rounding", "nearest").toUpperCase(Locale.ROOT));
        EventOffsets offsets = new EventOffsets(
                intValue(base + "fajr-offset"),
                intValue(base + "sunrise-offset"),
                intValue(base + "dhuhr-offset"),
                intValue(base + "asr-offset"),
                intValue(base + "maghrib-offset"),
                intValue(base + "isha-offset"));
        return new CalculationParameters(
                slug,
                doubleValue(base + "fajr-angle"),
                doubleValue(base + "isha-angle"),
                doubleValue(base + "isha-interval"),
                doubleValue(base + "maghrib-angle"),
                madhab,
                HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
                rounding,
                Shafaq.GENERAL,
                offsets);
    }

    /** CamelCase id -> lower-case kebab slug, e.g. {@code MuslimWorldLeague -> muslim-world-league}. */
    private static String slug(String methodKey) {
        return methodKey
                .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
                .toLowerCase(Locale.ROOT);
    }

    private String text(String key, String fallback) {
        String v = table.get(key);
        return v == null ? fallback : v.trim();
    }

    private double doubleValue(String key) {
        String v = table.get(key);
        return v == null ? 0.0 : Double.parseDouble(v.trim());
    }

    private int intValue(String key) {
        String v = table.get(key);
        return v == null ? 0 : Integer.parseInt(v.trim());
    }

    private static Set<String> extractSlugs(Map<String, String> table) {
        Set<String> keys = new TreeSet<>();
        for (String key : table.keySet()) {
            if (!key.startsWith(NAMESPACE) || key.equals(DEFAULT_KEY)) {
                continue;
            }
            String rest = key.substring(NAMESPACE.length());
            int dot = rest.indexOf('.');
            if (dot > 0) {
                keys.add(rest.substring(0, dot));
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    private static Map<String, String> load(String resourcePath, ResourceParser<Map<String, String>> parser) {
        ClassLoader loader = ClasspathCalculationMethodAccessor.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new ResourceParseException("Resource not found on classpath: " + resourcePath);
            }
            return parser.parse(in);
        } catch (IOException e) {
            throw new ResourceParseException("Failed reading " + resourcePath, e);
        }
    }
}
