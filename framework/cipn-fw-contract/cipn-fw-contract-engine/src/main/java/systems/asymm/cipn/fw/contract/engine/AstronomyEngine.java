/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.engine;

import java.time.Instant;
import java.time.LocalDate;

import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.DayEvents;

/**
 * Engine contract for solar/prayer-time astronomy: the single general engine (E3)
 * that computes a day's events for a location and calculation parameters.
 *
 * <p>Pure and I/O-free: identical results on the JVM (build/server) and, once
 * compiled, in the browser (WASM). Validated to the minute against the vendored
 * adhan.js reference (ADR-0009). The method registry (E2) that supplies the
 * parameters is a separate concern, served by a {@code CalculationMethodAccessor}.
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
     * The astronomical solar noon (transit) for a location and date, as a UTC instant.
     *
     * <p>Method- and madhab-independent by definition - it is the moment the sun crosses the
     * local meridian, which no calculation convention alters (unlike the twilight-angle events).
     * That is exactly why it takes no {@link CalculationParameters}. It is the <em>fallback</em>
     * source for the zenith window and the baked {@code solar-noon} column; where a national
     * authority announces its own value, that takes priority over this (ADR-0017). It is never
     * back-derived from the authority's published dhuhr, which carries a safety margin.
     *
     * @param coordinates the observer's location
     * @param date        the local calendar date
     * @return the solar transit as a UTC instant
     */
    Instant solarNoon(Coordinates coordinates, LocalDate date);
}
