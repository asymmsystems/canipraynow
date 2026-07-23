/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * The immutable inputs to the astronomy engine: the Fajr/Isha twilight angles (or
 * a fixed Isha interval in minutes), an optional Maghrib angle (Shia), the madhab,
 * the high-latitude rule, per-event offsets, rounding and shafaq.
 *
 * <p>Selecting an authority's published angles here is what the model calls E2;
 * passing arbitrary user angles is E3 proper. Pure data - the engine derives night
 * portions and everything else from these values.
 */
public record CalculationParameters(
        String method,
        double fajrAngle,
        double ishaAngle,
        double ishaInterval,
        double maghribAngle,
        Madhab madhab,
        HighLatitudeRule highLatitudeRule,
        Rounding rounding,
        Shafaq shafaq,
        EventOffsets offsets) {
}
