/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.engine;

import java.time.LocalDate;

import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.DayEvents;
import systems.asymm.cipn.fw.contract.data.EventOffsets;
import systems.asymm.cipn.fw.contract.data.HighLatitudeRule;
import systems.asymm.cipn.fw.contract.data.Madhab;
import systems.asymm.cipn.fw.contract.data.Rounding;
import systems.asymm.cipn.fw.contract.data.Shafaq;
import systems.asymm.cipn.fw.contract.engine.AstronomyEngine;

/**
 * Default {@link AstronomyEngine}: the one general solar/prayer-time engine (E3),
 * plus the E2 method registry. Pure and I/O-free; carries no framework annotations
 * and is wired externally (per the M360 engine convention).
 *
 * @see AstronomyEngine
 */
public class DefaultAstronomyEngine implements AstronomyEngine {

    @Override
    public DayEvents compute(Coordinates coordinates, LocalDate date, CalculationParameters params) {
        return SolarDayCalculator.compute(coordinates, date, params);
    }

    @Override
    public CalculationParameters resolveMethod(String methodKey, Madhab madhab) {
        return switch (methodKey) {
            // Custom-angle methods: no Dhuhr offset (bare parameters).
            case "ACJU" -> preset("ACJU", 20, 18, 0, 0, Rounding.NEAREST, EventOffsets.NONE, madhab);
            case "Kemenag" -> preset("Kemenag", 20, 18, 0, 0, Rounding.NEAREST, EventOffsets.NONE, madhab);
            case "France" -> preset("France", 12, 12, 0, 0, Rounding.NEAREST, EventOffsets.NONE, madhab);
            case "Russia" -> preset("Russia", 16, 15, 0, 0, Rounding.NEAREST, EventOffsets.NONE, madhab);

            // adhan.js presets.
            case "Egyptian" -> preset("Egyptian", 19.5, 17.5, 0, 0, Rounding.NEAREST, dhuhr(1), madhab);
            case "Karachi" -> preset("Karachi", 18, 18, 0, 0, Rounding.NEAREST, dhuhr(1), madhab);
            case "NorthAmerica" -> preset("NorthAmerica", 15, 15, 0, 0, Rounding.NEAREST, dhuhr(1), madhab);
            case "UmmAlQura" -> preset("UmmAlQura", 18.5, 0, 90, 0, Rounding.NEAREST, EventOffsets.NONE, madhab);
            case "Dubai" -> preset("Dubai", 18.2, 18.2, 0, 0, Rounding.NEAREST,
                    new EventOffsets(0, -3, 3, 3, 3, 0), madhab);
            case "MoonsightingCommittee" -> preset("MoonsightingCommittee", 18, 18, 0, 0, Rounding.NEAREST,
                    new EventOffsets(0, 0, 5, 0, 3, 0), madhab);
            case "Kuwait" -> preset("Kuwait", 18, 17.5, 0, 0, Rounding.NEAREST, EventOffsets.NONE, madhab);
            case "Qatar" -> preset("Qatar", 18, 0, 90, 0, Rounding.NEAREST, EventOffsets.NONE, madhab);
            case "Singapore" -> preset("Singapore", 20, 18, 0, 0, Rounding.UP, dhuhr(1), madhab);
            case "Tehran" -> preset("Tehran", 17.7, 14, 0, 4.5, Rounding.NEAREST, EventOffsets.NONE, madhab);
            case "Turkey" -> preset("Turkey", 18, 17, 0, 0, Rounding.NEAREST,
                    new EventOffsets(0, -7, 5, 4, 7, 0), madhab);

            case "MuslimWorldLeague" -> preset("MuslimWorldLeague", 18, 17, 0, 0, Rounding.NEAREST, dhuhr(1), madhab);
            default -> preset("MuslimWorldLeague", 18, 17, 0, 0, Rounding.NEAREST, dhuhr(1), madhab);
        };
    }

    private static CalculationParameters preset(String method, double fajrAngle, double ishaAngle,
                                                double ishaInterval, double maghribAngle, Rounding rounding,
                                                EventOffsets offsets, Madhab madhab) {
        return new CalculationParameters(method, fajrAngle, ishaAngle, ishaInterval, maghribAngle,
                madhab, HighLatitudeRule.MIDDLE_OF_THE_NIGHT, rounding, Shafaq.GENERAL, offsets);
    }

    private static EventOffsets dhuhr(int minutes) {
        return new EventOffsets(0, 0, minutes, 0, 0, 0);
    }
}
