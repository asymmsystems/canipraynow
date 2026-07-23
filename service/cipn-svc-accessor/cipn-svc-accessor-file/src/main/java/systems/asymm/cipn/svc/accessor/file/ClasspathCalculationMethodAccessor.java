/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.file;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import systems.asymm.cipn.fw.common.util.ResourceParseException;
import systems.asymm.cipn.fw.common.util.ResourceParser;
import systems.asymm.cipn.fw.common.util.StructuredXmlParser;
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
 * <p>4-Concerns: Location = classpath (this class); Type/Schema = XML (internal, via
 * {@link StructuredXmlParser}); Consumed Type = calculation-method parameters (the
 * contract). Reads {@code calculation-methods.xml} once at construction and serves an
 * immutable view; only non-default fields need to appear in the file.
 *
 * <p>Keys in the file are lower-case kebab slugs; a caller's CamelCase method id
 * (e.g. {@code MuslimWorldLeague}) normalizes to its slug ({@code muslim-world-league}).
 *
 * @see CalculationMethodAccessor
 */
public class ClasspathCalculationMethodAccessor implements CalculationMethodAccessor {

    private static final String DEFAULT_RESOURCE = "calculation-methods.xml";

    private final Map<String, MethodEntry> bySlug;
    private final String defaultSlug;

    /** Loads the registry from the bundled {@code calculation-methods.xml}. */
    public ClasspathCalculationMethodAccessor() {
        this(DEFAULT_RESOURCE, new StructuredXmlParser<>(CalculationMethods.class));
    }

    /**
     * Loads the registry from a classpath resource.
     *
     * @param resourcePath classpath path to the XML table
     * @param parser       the XML parser (Type/Schema concern)
     */
    public ClasspathCalculationMethodAccessor(String resourcePath, ResourceParser<CalculationMethods> parser) {
        CalculationMethods doc = load(resourcePath, parser);
        Map<String, MethodEntry> map = new LinkedHashMap<>();
        for (MethodEntry entry : doc.method()) {
            map.put(entry.key(), entry);
        }
        this.bySlug = Map.copyOf(map);
        this.defaultSlug = doc.defaultMethod() == null ? "muslim-world-league" : doc.defaultMethod();
    }

    @Override
    public Optional<CalculationParameters> parameters(String methodKey, Madhab madhab) {
        MethodEntry entry = bySlug.get(slug(methodKey));
        return entry == null ? Optional.empty() : Optional.of(toParameters(entry, madhab));
    }

    @Override
    public CalculationParameters parametersOrDefault(String methodKey, Madhab madhab) {
        return parameters(methodKey, madhab).orElseGet(() -> toParameters(bySlug.get(defaultSlug), madhab));
    }

    @Override
    public Set<String> methodKeys() {
        return bySlug.keySet();
    }

    private static CalculationParameters toParameters(MethodEntry e, Madhab madhab) {
        Rounding rounding = e.rounding() == null
                ? Rounding.NEAREST : Rounding.valueOf(e.rounding().toUpperCase(Locale.ROOT));
        EventOffsets offsets = new EventOffsets(
                e.fajrOffset(), e.sunriseOffset(), e.dhuhrOffset(), e.asrOffset(), e.maghribOffset(), e.ishaOffset());
        return new CalculationParameters(e.key(), e.fajrAngle(), e.ishaAngle(), e.ishaInterval(), e.maghribAngle(),
                madhab, HighLatitudeRule.MIDDLE_OF_THE_NIGHT, rounding, Shafaq.GENERAL, offsets);
    }

    /** CamelCase id -> lower-case kebab slug, e.g. {@code MuslimWorldLeague -> muslim-world-league}. */
    private static String slug(String methodKey) {
        return methodKey
                .replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2")
                .toLowerCase(Locale.ROOT);
    }

    private static CalculationMethods load(String resourcePath, ResourceParser<CalculationMethods> parser) {
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

    /** XML binding (Type/Schema detail): {@code <calculation-methods default-method="...">}. */
    record CalculationMethods(String defaultMethod, List<MethodEntry> method) {
    }

    /** XML binding for one {@code <method .../>} element. Absent numeric attributes default to 0. */
    record MethodEntry(
            String key,
            double fajrAngle,
            double ishaAngle,
            double ishaInterval,
            double maghribAngle,
            String rounding,
            int fajrOffset,
            int sunriseOffset,
            int dhuhrOffset,
            int asrOffset,
            int maghribOffset,
            int ishaOffset) {
    }
}
