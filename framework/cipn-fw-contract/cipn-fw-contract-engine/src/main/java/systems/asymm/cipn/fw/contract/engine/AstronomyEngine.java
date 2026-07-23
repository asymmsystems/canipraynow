/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.engine;

import java.time.LocalDate;

import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.DayEvents;
import systems.asymm.cipn.fw.contract.data.Madhab;

/**
 * Engine contract for solar/prayer-time astronomy: the single general engine (E3)
 * that computes a day's events for a location, and the E2 configuration lookup that
 * resolves a calculation-method key to its authority's published parameters.
 *
 * <p>Pure and I/O-free: identical results on the JVM (build/server) and, once
 * compiled, in the browser (WASM). Validated to the minute against the vendored
 * adhan.js reference (ADR-0009).
 *
 * @see Engine
 */
public interface AstronomyEngine extends Engine {

    /**
     * Compute the seven daily events (E3).
     *
     * @param coordinates the observer's location
     * @param date        the local calendar date to compute for
     * @param params      the calculation parameters (angles/interval, madhab, rounding, ...)
     * @return the day's events in UTC
     */
    DayEvents compute(Coordinates coordinates, LocalDate date, CalculationParameters params);

    /**
     * Resolve a calculation-method key to its authority's published parameters (E2).
     *
     * <p>Mirrors the site's method registry: the custom-angle methods (ACJU, Kemenag,
     * France, Russia) carry no Dhuhr offset; the rest reproduce the adhan presets. An
     * unknown key falls back to Muslim World League.
     *
     * @param methodKey the method identifier (e.g. {@code "MuslimWorldLeague"}, {@code "ACJU"})
     * @param madhab    the school, which sets the ʿAsr shadow factor
     * @return the resolved, immutable parameters
     */
    CalculationParameters resolveMethod(String methodKey, Madhab madhab);
}
