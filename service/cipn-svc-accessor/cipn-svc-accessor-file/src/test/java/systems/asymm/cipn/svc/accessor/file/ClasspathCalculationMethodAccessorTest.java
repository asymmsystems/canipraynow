/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.file;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.access.CalculationMethodAccessor;
import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.Madhab;
import systems.asymm.cipn.fw.contract.data.Rounding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the calculation-method registry resolves from the properties table with
 * the right angles, offsets, rounding and defaults.
 */
class ClasspathCalculationMethodAccessorTest {

    private final CalculationMethodAccessor accessor = new ClasspathCalculationMethodAccessor();

    @Test
    void resolvesPresetWithDhuhrOffset() {
        CalculationParameters p = accessor.parametersOrDefault("MuslimWorldLeague", Madhab.SHAFI);
        assertEquals(18.0, p.fajrAngle());
        assertEquals(17.0, p.ishaAngle());
        assertEquals(1, p.offsets().dhuhr());
        assertEquals(Rounding.NEAREST, p.rounding());
        assertEquals(Madhab.SHAFI, p.madhab());
    }

    @Test
    void resolvesCustomAngleMethodWithoutDhuhrOffset() {
        CalculationParameters p = accessor.parametersOrDefault("ACJU", Madhab.HANAFI);
        assertEquals(20.0, p.fajrAngle());
        assertEquals(18.0, p.ishaAngle());
        assertEquals(0, p.offsets().dhuhr());
        assertEquals(Madhab.HANAFI, p.madhab());
    }

    @Test
    void resolvesIntervalIntervalAndShiaAngleAndRoundingUp() {
        assertEquals(90.0, accessor.parametersOrDefault("Qatar", Madhab.SHAFI).ishaInterval());
        assertEquals(4.5, accessor.parametersOrDefault("Tehran", Madhab.SHAFI).maghribAngle());
        assertEquals(Rounding.UP, accessor.parametersOrDefault("Singapore", Madhab.SHAFI).rounding());
        assertEquals(-7, accessor.parametersOrDefault("Turkey", Madhab.SHAFI).offsets().sunrise());
    }

    @Test
    void unknownKeyFallsBackToDefault() {
        assertTrue(accessor.parameters("NoSuchMethod", Madhab.SHAFI).isEmpty());
        CalculationParameters fallback = accessor.parametersOrDefault("NoSuchMethod", Madhab.SHAFI);
        assertEquals("muslim-world-league", fallback.method());
    }

    @Test
    void normalizesCamelCaseIdToSlug() {
        assertTrue(accessor.parameters("NorthAmerica", Madhab.SHAFI).isPresent());
        assertTrue(accessor.parameters("UmmAlQura", Madhab.SHAFI).isPresent());
        assertTrue(accessor.parameters("MoonsightingCommittee", Madhab.SHAFI).isPresent());
    }

    @Test
    void registryHoldsEverySupportedMethod() {
        assertEquals(16, accessor.methodKeys().size());
        assertTrue(accessor.methodKeys().contains("moonsighting-committee"));
    }
}
