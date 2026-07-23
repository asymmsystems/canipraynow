/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * The immutable inputs to the astronomy engine for one calculation method.
 *
 * <p>Selecting an authority's published angles here is what the model calls E2;
 * passing arbitrary user angles is E3 proper. This is pure data - the engine
 * derives night portions and everything else from these values.
 *
 * @param method           the method identifier this set represents (e.g. {@code "MuslimWorldLeague"},
 *                         {@code "ACJU"}); also selects method-specific behaviour such as the
 *                         Moonsighting seasonal twilight
 * @param fajrAngle        sun depression below the horizon, in degrees, at which Fajr begins
 * @param ishaAngle        sun depression below the horizon, in degrees, at which Isha begins;
 *                         ignored when {@code ishaInterval > 0}
 * @param ishaInterval     fixed minutes after Maghrib for Isha (used by Umm al-Qura, Qatar);
 *                         0 means use {@code ishaAngle} instead
 * @param maghribAngle     sun depression, in degrees, for a Shia Maghrib later than sunset;
 *                         0 means Maghrib is sunset
 * @param madhab           the school, which sets the ʿAsr shadow factor
 * @param highLatitudeRule how Fajr/Isha are bounded when true twilight does not occur
 * @param rounding         how the results are rounded to whole minutes
 * @param shafaq           the twilight definition for the Moonsighting seasonal Isha model
 * @param offsets          per-event minute adjustments applied after computation
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
