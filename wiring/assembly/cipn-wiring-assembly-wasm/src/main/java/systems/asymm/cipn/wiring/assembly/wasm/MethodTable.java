/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.assembly.wasm;

import systems.asymm.cipn.fw.common.util.XmlElement;
import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.EventOffsets;
import systems.asymm.cipn.fw.contract.data.HighLatitudeRule;
import systems.asymm.cipn.fw.contract.data.Madhab;
import systems.asymm.cipn.fw.contract.data.Rounding;
import systems.asymm.cipn.fw.contract.data.Shafaq;

/**
 * Resolves a method key to {@link CalculationParameters} from a parsed
 * {@code calculation-method.xml} tree, for the WASM fallback (R2). This is the WASM-side
 * counterpart of {@code ClasspathCalculationMethodAccessor#toParameters}: same defaults, same
 * meaning, but reading a neutral {@link XmlElement} (from {@code MinimalXmlParser}) rather than
 * binding via Jackson, so it compiles to WASM-GC (ADR-0015 §4). The mapping is asserted equal
 * to the JVM accessor by a parity test.
 *
 * <p>The Madhab is supplied per call (it is the Asr shadow factor, not stored in the table),
 * matching the accessor. Keys are matched case-insensitively against the table's kebab-case
 * keys; an unknown key falls back to the table's {@code default-method}.
 */
final class MethodTable {

    private static final String METHOD = "method";
    private static final String KEY = "key";

    private MethodTable() {
    }

    /**
     * @param root       the parsed {@code <calculation-methods>} root
     * @param methodKey  the requested method key (matched case-insensitively)
     * @param madhab     the Asr shadow-factor school to apply
     * @return the parameters for the method, or for the table default when the key is unknown
     */
    static CalculationParameters resolve(XmlElement root, String methodKey, Madhab madhab) {
        XmlElement method = find(root, methodKey);
        if (method == null) {
            method = find(root, root.attribute("default-method", "muslim-world-league"));
        }
        if (method == null) {
            // Empty/invalid table: fall back to Muslim World League angles so a time still shows.
            return new CalculationParameters("muslim-world-league", 18.0, 17.0, 0.0, 0.0, madhab,
                    HighLatitudeRule.MIDDLE_OF_THE_NIGHT, Rounding.NEAREST, Shafaq.GENERAL, EventOffsets.NONE);
        }
        EventOffsets offsets = new EventOffsets(
                intAttr(method, "fajr-offset"),
                intAttr(method, "sunrise-offset"),
                intAttr(method, "dhuhr-offset"),
                intAttr(method, "asr-offset"),
                intAttr(method, "maghrib-offset"),
                intAttr(method, "isha-offset"));
        return new CalculationParameters(
                method.attribute(KEY),
                doubleAttr(method, "fajr-angle"),
                doubleAttr(method, "isha-angle"),
                doubleAttr(method, "isha-interval"),
                doubleAttr(method, "maghrib-angle"),
                madhab,
                HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
                rounding(method.attribute("rounding")),
                Shafaq.GENERAL,
                offsets);
    }

    private static XmlElement find(XmlElement root, String key) {
        String want = asciiLower(key);
        for (XmlElement method : root.children()) {
            if (METHOD.equals(method.name()) && want.equals(asciiLower(method.attribute(KEY, "")))) {
                return method;
            }
        }
        return null;
    }

    private static Rounding rounding(String value) {
        if (value == null) {
            return Rounding.NEAREST;
        }
        return switch (asciiLower(value)) {
            case "up" -> Rounding.UP;
            case "none" -> Rounding.NONE;
            default -> Rounding.NEAREST;
        };
    }

    private static double doubleAttr(XmlElement element, String name) {
        String value = element.attribute(name);
        return value == null ? 0.0 : Double.parseDouble(value);
    }

    private static int intAttr(XmlElement element, String name) {
        String value = element.attribute(name);
        return value == null ? 0 : Integer.parseInt(value);
    }

    /** ASCII lower-case without pulling locale/CLDR data into WASM; keys are ASCII kebab slugs. */
    private static String asciiLower(String s) {
        char[] chars = s.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c >= 'A' && c <= 'Z') {
                chars[i] = (char) (c + 32);
            }
        }
        return new String(chars);
    }
}
