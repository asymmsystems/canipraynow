/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.engine;

import java.time.LocalDate;

import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.DayEvents;
import systems.asymm.cipn.fw.contract.engine.AstronomyEngine;

/**
 * Default {@link AstronomyEngine}: the one general solar/prayer-time engine (E3).
 * Pure and I/O-free; carries no framework annotations and is wired externally (per
 * the M360 engine convention). The method registry it computes with is served by a
 * {@code CalculationMethodAccessor}, keeping this engine free of any configuration.
 *
 * @see AstronomyEngine
 */
public class DefaultAstronomyEngine implements AstronomyEngine {

    @Override
    public DayEvents compute(Coordinates coordinates, LocalDate date, CalculationParameters params) {
        return SolarDayCalculator.compute(coordinates, date, params);
    }
}
