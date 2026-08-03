/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.assembly.wasm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.common.util.MinimalXmlParser;
import systems.asymm.cipn.fw.common.util.XmlElement;
import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.HighLatitudeRule;
import systems.asymm.cipn.fw.contract.data.Madhab;
import systems.asymm.cipn.fw.contract.data.Rounding;
import systems.asymm.cipn.fw.contract.data.Shafaq;

/**
 * Validates the WASM-side method resolution ({@link MinimalXmlParser} + {@link MethodTable}) on
 * the JVM against the real {@code calculation-method.xml} table. This is the parity gate for
 * the browser fallback: the same code compiles to WASM-GC, so agreement here means the WASM
 * path resolves the identical {@link CalculationParameters} as the build/server accessor
 * (whose defaults this mirrors) - covering bare angles, Dhuhr offset, fixed Isha interval,
 * up-rounding, multi-event offsets, the default fallback, madhab, and case-insensitive keys.
 */
class MethodTableTest {

    // A verbatim copy of the shipped calculation-method.xml table (attribute-only, kebab keys).
    private static final String TABLE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <calculation-methods default-method="muslim-world-league">
                <method key="acju" fajr-angle="20" isha-angle="18"/>
                <method key="kemenag" fajr-angle="20" isha-angle="18"/>
                <method key="france" fajr-angle="12" isha-angle="12"/>
                <method key="russia" fajr-angle="16" isha-angle="15"/>
                <method key="muslim-world-league" fajr-angle="18" isha-angle="17" dhuhr-offset="1"/>
                <method key="egyptian" fajr-angle="19.5" isha-angle="17.5" dhuhr-offset="1"/>
                <method key="karachi" fajr-angle="18" isha-angle="18" dhuhr-offset="1"/>
                <method key="north-america" fajr-angle="15" isha-angle="15" dhuhr-offset="1"/>
                <method key="umm-al-qura" fajr-angle="18.5" isha-interval="90"/>
                <method key="dubai" fajr-angle="18.2" isha-angle="18.2"
                        sunrise-offset="-3" dhuhr-offset="3" asr-offset="3" maghrib-offset="3"/>
                <method key="moonsighting-committee" fajr-angle="18" isha-angle="18"
                        dhuhr-offset="5" maghrib-offset="3"/>
                <method key="kuwait" fajr-angle="18" isha-angle="17.5"/>
                <method key="qatar" fajr-angle="18" isha-interval="90"/>
                <method key="singapore" fajr-angle="20" isha-angle="18" rounding="up" dhuhr-offset="1"/>
                <method key="tehran" fajr-angle="17.7" isha-angle="14" maghrib-angle="4.5"/>
                <method key="turkey" fajr-angle="18" isha-angle="17"
                        sunrise-offset="-7" dhuhr-offset="5" asr-offset="4" maghrib-offset="7"/>
            </calculation-methods>
            """;

    private static final XmlElement ROOT = new MinimalXmlParser()
            .parse(new ByteArrayInputStream(TABLE.getBytes(StandardCharsets.UTF_8)));

    private static CalculationParameters resolve(String key, Madhab madhab) {
        return MethodTable.resolve(ROOT, key, madhab);
    }

    @Test
    void bareAngleMethod() {
        CalculationParameters p = resolve("acju", Madhab.SHAFI);
        assertEquals("acju", p.method());
        assertEquals(20.0, p.fajrAngle());
        assertEquals(18.0, p.ishaAngle());
        assertEquals(0.0, p.ishaInterval());
        assertEquals(0.0, p.maghribAngle());
        assertEquals(0, p.offsets().dhuhr());
        assertEquals(Rounding.NEAREST, p.rounding());
        assertEquals(HighLatitudeRule.MIDDLE_OF_THE_NIGHT, p.highLatitudeRule());
        assertEquals(Shafaq.GENERAL, p.shafaq());
    }

    @Test
    void dhuhrOffsetMethod() {
        CalculationParameters p = resolve("muslim-world-league", Madhab.SHAFI);
        assertEquals(18.0, p.fajrAngle());
        assertEquals(17.0, p.ishaAngle());
        assertEquals(1, p.offsets().dhuhr());
    }

    @Test
    void fixedIshaIntervalMethod() {
        CalculationParameters p = resolve("umm-al-qura", Madhab.SHAFI);
        assertEquals(18.5, p.fajrAngle());
        assertEquals(0.0, p.ishaAngle(), "angle ignored when an interval is set");
        assertEquals(90.0, p.ishaInterval());
    }

    @Test
    void upRoundingMethod() {
        assertEquals(Rounding.UP, resolve("singapore", Madhab.SHAFI).rounding());
    }

    @Test
    void multiEventOffsetsMethod() {
        CalculationParameters p = resolve("turkey", Madhab.SHAFI);
        assertEquals(-7, p.offsets().sunrise());
        assertEquals(5, p.offsets().dhuhr());
        assertEquals(4, p.offsets().asr());
        assertEquals(7, p.offsets().maghrib());
        assertEquals(0, p.offsets().fajr());
        assertEquals(0, p.offsets().isha());
    }

    @Test
    void fractionalAnglesMethod() {
        CalculationParameters p = resolve("tehran", Madhab.SHAFI);
        assertEquals(17.7, p.fajrAngle());
        assertEquals(14.0, p.ishaAngle());
        assertEquals(4.5, p.maghribAngle());
    }

    @Test
    void unknownKeyFallsBackToTableDefault() {
        CalculationParameters p = resolve("does-not-exist", Madhab.SHAFI);
        assertEquals("muslim-world-league", p.method());
        assertEquals(18.0, p.fajrAngle());
        assertEquals(1, p.offsets().dhuhr());
    }

    @Test
    void keyMatchIsCaseInsensitive() {
        assertEquals("umm-al-qura", resolve("Umm-Al-Qura", Madhab.SHAFI).method());
        assertEquals("acju", resolve("ACJU", Madhab.SHAFI).method());
    }

    @Test
    void madhabIsPassedThrough() {
        assertEquals(Madhab.HANAFI, resolve("acju", Madhab.HANAFI).madhab());
        assertEquals(Madhab.SHAFI, resolve("acju", Madhab.SHAFI).madhab());
    }
}
