/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Fans assets/data/cities.json out into one JBake content stub per city page.
 *
 * WHY THIS EXISTS AT ALL, given FreeMarker is right there:
 * JBake's DocumentsRenderer emits exactly one output file per crawled content
 * file, and a FreeMarker template renders one document into one string. Neither
 * can turn a 301-entry array into 110 URLs - so the content files have to exist
 * before JBake runs. Creating them is the ONLY job of this program.
 *
 * It therefore writes DATA (front matter) and no presentation: no prose, no
 * markup. page/city.ftl owns every rendered byte. The one piece of computation
 * here that a template genuinely cannot do is the nearest-neighbour distance
 * (FreeMarker has no sqrt/trig), so `nearby` is precomputed as front matter.
 * The city index needs no stub at all - page/cities.ftl reads cities.json
 * straight from JBake's data model (data.folder=assets/data).
 *
 * Runs at generate-sources, before JBake bakes. Output is git-ignored: derived,
 * like the compiled CSS. Single-file source launch - no compile step, no deps.
 *
 *   java scripts/GenerateCityPages.java <projectBasedir>
 *
 * @author Fahim Farook
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public final class GenerateCityPages {

    /** Front-matter marker identifying a stub this generator owns. */
    private static final String MARKER = "generated_by=GenerateCityPages";

    /** How many neighbours each city page links to. */
    private static final int NEARBY_COUNT = 5;

    /** Data only. Every rendered byte belongs to page/city.ftl. */
    private static final String STUB = """
            title=Can I pray now in %1$s?
            type=city
            status=published
            %8$s
            city_name=%1$s
            country_name=%2$s
            city_lat=%3$s
            city_lng=%4$s
            city_tz=%5$s
            city_cc=%6$s
            nearby=%7$s
            city_authority=%9$s
            city_method=%10$s
            city_madhhab=%11$s
            ~~~~~~
            """;

    /** Method key -> human phrase, so each country page names its own convention. */
    private static final Map<String, String> METHOD_LABEL = Map.ofEntries(
            Map.entry("MuslimWorldLeague", "the Muslim World League convention"),
            Map.entry("NorthAmerica", "the ISNA (North America) convention"),
            Map.entry("Egyptian", "the Egyptian General Authority of Survey convention"),
            Map.entry("UmmAlQura", "the Umm al-Qura convention"),
            Map.entry("Karachi", "the University of Islamic Sciences, Karachi convention"),
            Map.entry("Tehran", "the Institute of Geophysics, Tehran convention"),
            Map.entry("Dubai", "the Dubai (UAE) convention"),
            Map.entry("Kuwait", "the Kuwait convention"),
            Map.entry("Qatar", "the Qatar convention"),
            Map.entry("Singapore", "the MUIS (Singapore) convention"),
            Map.entry("Turkey", "the Diyanet (Turkey) convention"),
            Map.entry("France", "the UOIF (France) convention"),
            Map.entry("Russia", "the Spiritual Administration of Muslims of Russia convention"),
            Map.entry("MoonsightingCommittee", "the Moonsighting Committee Worldwide convention"),
            Map.entry("ACJU", "the ACJU (Sri Lanka) convention"),
            Map.entry("Kemenag", "the Kemenag (Indonesia) convention"));

    /** Predominant-madhhab key -> display name. */
    private static final Map<String, String> MADHHAB_LABEL = Map.of(
            "maliki", "Mālikī", "hanbali", "Ḥanbalī", "jafari", "Jaʿfarī",
            "ibadi", "Ibāḍī", "hanafi", "Ḥanafī", "shafii", "Shāfiʿī");

    public static void main(String[] args) throws IOException {
        Path basedir = Path.of(args.length > 0 ? args[0] : ".");
        Path cities = basedir.resolve("src/main/jbake/assets/data/cities.json");
        Path content = basedir.resolve("src/main/jbake/content");

        Object parsed = Json.parse(Files.readString(cities, StandardCharsets.UTF_8));
        List<?> all = (List<?>) ((Map<?, ?>) parsed).get("cities");

        // Per-country context (authority, method, dominant madhhab), so each city
        // page carries genuinely unique, factual content rather than templated
        // near-duplicates - the difference between ranking and being demoted.
        Map<?, ?> defaults = (Map<?, ?>) Json.parse(
                Files.readString(basedir.resolve("src/main/jbake/assets/data/method-defaults.json"), StandardCharsets.UTF_8));
        Map<?, ?> byCountry = (Map<?, ?>) defaults.get("byCountry");
        Map<?, ?> predominant = (Map<?, ?>) defaults.get("predominantMadhhab");
        Map<?, ?> authorities = (Map<?, ?>) ((Map<?, ?>) Json.parse(
                Files.readString(basedir.resolve("scripts/authorities.json"), StandardCharsets.UTF_8))).get("authorities");

        int removed = sweep(content);
        int written = 0;
        for (Object entry : all) {
            Map<?, ?> city = (Map<?, ?>) entry;
            if (!Boolean.TRUE.equals(city.get("page"))) {
                continue;
            }
            String cc = str(city, "cc");
            String methodKey = byCountry.get(cc) instanceof String s ? s : "MuslimWorldLeague";
            String methodLabel = METHOD_LABEL.getOrDefault(methodKey, "a standard convention");
            String authority = authorities.get(cc) instanceof Map<?, ?> a
                    && a.get("name") instanceof String n ? n : "";
            String madhhab = predominant.get(cc) instanceof String m
                    ? MADHHAB_LABEL.getOrDefault(m, m) : "";

            Path dir = content.resolve(str(city, "countrySlug"));
            Files.createDirectories(dir);
            String stub = STUB.formatted(
                    str(city, "name"), str(city, "country"),
                    num(city, "lat"), num(city, "lng"),
                    str(city, "tz"), str(city, "cc"),
                    nearby(city, all), MARKER,
                    authority, methodLabel, madhhab);
            Files.writeString(dir.resolve(str(city, "slug") + ".html"), stub, StandardCharsets.UTF_8);
            written++;
        }
        System.out.printf("city stubs: removed %d, wrote %d%n", removed, written);
    }

    /** Delete stubs from a previous run, so a city dropped from the dataset disappears. */
    private static int sweep(Path content) throws IOException {
        if (!Files.isDirectory(content)) {
            return 0;
        }
        List<Path> stale = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(content)) {
            for (Path p : walk.filter(Files::isRegularFile).toList()) {
                if (p.toString().endsWith(".html") && Files.readString(p, StandardCharsets.UTF_8).contains(MARKER)) {
                    stale.add(p);
                }
            }
        }
        for (Path p : stale) {
            Files.delete(p);
        }
        return stale.size();
    }

    /**
     * The nearest other cities that have pages, as "name|country|href|km"
     * records joined by ';' - parsed back apart by page/city.ftl.
     *
     * Precomputed here only because FreeMarker has no sqrt or trigonometry to
     * run a haversine with. It is data, not presentation.
     */
    private static String nearby(Map<?, ?> city, List<?> all) {
        double lat = (Double) city.get("lat");
        double lng = (Double) city.get("lng");
        record Near(String name, String country, String href, long km) { }
        List<Near> near = new ArrayList<>();
        for (Object o : all) {
            Map<?, ?> c = (Map<?, ?>) o;
            if (!Boolean.TRUE.equals(c.get("page")) || c == city) {
                continue;
            }
            long km = Math.round(haversineKm(lat, lng, (Double) c.get("lat"), (Double) c.get("lng")));
            if (km == 0) {
                continue;   // same point: a duplicate entry, not a neighbour
            }
            near.add(new Near(str(c, "name"), str(c, "country"),
                    "/" + str(c, "countrySlug") + "/" + str(c, "slug") + ".html", km));
        }
        near.sort((a, b) -> Long.compare(a.km(), b.km()));
        List<String> out = new ArrayList<>();
        for (Near n : near.subList(0, Math.min(NEARBY_COUNT, near.size()))) {
            // '|' and ';' cannot occur in a slug or an ASCII city name.
            out.add(n.name() + "|" + n.country() + "|" + n.href() + "|" + n.km());
        }
        return String.join(";", out);
    }

    /** Great-circle distance in km. */
    private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 2 * r * Math.asin(Math.min(1, Math.sqrt(h)));
    }

    private static String str(Map<?, ?> city, String key) {
        Object v = city.get(key);
        if (!(v instanceof String s) || s.isBlank()) {
            throw new IllegalStateException("cities.json: missing/blank '" + key + "' in " + city);
        }
        return s;
    }

    /** Coordinates must render as plain decimals, never in scientific notation. */
    private static String num(Map<?, ?> city, String key) {
        Object v = city.get(key);
        if (!(v instanceof Double d) || d.isNaN() || d.isInfinite()) {
            throw new IllegalStateException("cities.json: missing/invalid '" + key + "' in " + city);
        }
        return new java.math.BigDecimal(d).setScale(4, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    /** Minimal JSON reader: objects, arrays, strings, numbers, booleans, null. */
    private static final class Json {
        private final String src;
        private int i;

        private Json(String src) {
            this.src = src;
        }

        static Object parse(String src) {
            Json p = new Json(src);
            p.ws();
            Object v = p.value();
            p.ws();
            if (p.i != src.length()) {
                throw new IllegalStateException("trailing content at " + p.i);
            }
            return v;
        }

        private Object value() {
            char c = peek();
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't', 'f' -> bool();
                case 'n' -> nul();
                default -> number();
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> map = new LinkedHashMap<>();
            expect('{');
            ws();
            if (peek() == '}') {
                i++;
                return map;
            }
            while (true) {
                ws();
                String key = string();
                ws();
                expect(':');
                ws();
                map.put(key, value());
                ws();
                char c = src.charAt(i++);
                if (c == '}') {
                    return map;
                }
                if (c != ',') {
                    throw new IllegalStateException("expected , or } at " + i);
                }
            }
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            expect('[');
            ws();
            if (peek() == ']') {
                i++;
                return list;
            }
            while (true) {
                ws();
                list.add(value());
                ws();
                char c = src.charAt(i++);
                if (c == ']') {
                    return list;
                }
                if (c != ',') {
                    throw new IllegalStateException("expected , or ] at " + i);
                }
            }
        }

        private String string() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = src.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = src.charAt(i++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        sb.append((char) Integer.parseInt(src.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw new IllegalStateException("bad escape \\" + e + " at " + i);
                }
            }
        }

        private Double number() {
            int start = i;
            while (i < src.length() && "+-.eE0123456789".indexOf(src.charAt(i)) >= 0) {
                i++;
            }
            return Double.valueOf(src.substring(start, i));
        }

        private Boolean bool() {
            if (src.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            throw new IllegalStateException("bad literal at " + i);
        }

        private Object nul() {
            if (!src.startsWith("null", i)) {
                throw new IllegalStateException("bad literal at " + i);
            }
            i += 4;
            return null;
        }

        private char peek() {
            return src.charAt(i);
        }

        private void expect(char c) {
            if (src.charAt(i++) != c) {
                throw new IllegalStateException("expected " + c + " at " + (i - 1));
            }
        }

        private void ws() {
            while (i < src.length() && Character.isWhitespace(src.charAt(i))) {
                i++;
            }
        }
    }

    private GenerateCityPages() {
    }
}
