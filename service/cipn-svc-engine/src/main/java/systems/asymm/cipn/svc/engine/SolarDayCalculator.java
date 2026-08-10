/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.engine;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;

import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.DayEvents;
import systems.asymm.cipn.fw.contract.data.Rounding;

import static systems.asymm.cipn.svc.engine.Astronomical.seasonAdjustedEveningTwilightSeconds;
import static systems.asymm.cipn.svc.engine.Astronomical.seasonAdjustedMorningTwilightSeconds;

/**
 * The core of the general astronomy engine (E3): assembles the seven daily events
 * from {@link SolarTime} - twilight angles, the night-portion safety limit, the
 * Moonsighting seasonal twilight, the Shia Maghrib angle, interval Isha, per-event
 * offsets and rounding. A faithful port of adhan.js {@code PrayerTimes}, validated
 * to the minute against golden vectors captured from that library (ADR-0009).
 *
 * <p>The polar-circle resolution branch is intentionally omitted (the site never
 * enables it); latitudes beyond ~55&deg; where twilight does not resolve are a
 * follow-up (see the module README).
 */
final class SolarDayCalculator {

    private static final String MOONSIGHTING = "moonsighting-committee";

    private SolarDayCalculator() {
    }

    /**
     * Solar noon (transit) only, as a UTC instant - the raw meridian crossing, with no method
     * offset or rounding. Method-independent (no {@link CalculationParameters}); see
     * {@link systems.asymm.cipn.fw.contract.engine.AstronomyEngine#solarNoon}.
     */
    static Instant solarNoon(Coordinates coordinates, LocalDate date) {
        SolarTime solarTime = new SolarTime(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), coordinates);
        return fromFractionalHours(date, solarTime.transit);
    }

    static DayEvents compute(Coordinates coordinates, LocalDate date, CalculationParameters params) {
        SolarTime solarTime = new SolarTime(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), coordinates);
        LocalDate tomorrow = date.plusDays(1);
        SolarTime tomorrowSolarTime = new SolarTime(
                tomorrow.getYear(), tomorrow.getMonthValue(), tomorrow.getDayOfMonth(), coordinates);

        Instant transit = fromFractionalHours(date, solarTime.transit);
        Instant sunriseTime = fromFractionalHours(date, solarTime.sunrise);
        Instant sunsetTime = fromFractionalHours(date, solarTime.sunset);
        Instant asrTime = fromFractionalHours(date, solarTime.afternoon(params.madhab().shadowLength()));
        Instant tomorrowSunrise = fromFractionalHours(tomorrow, tomorrowSolarTime.sunrise);

        // Night length in seconds: today's sunset to tomorrow's sunrise.
        double nightSeconds = (tomorrowSunrise.toEpochMilli() - sunsetTime.toEpochMilli()) / 1000.0;
        double latitude = coordinates.latitude();
        boolean moonsighting = MOONSIGHTING.equals(params.method());
        int dayOfYear = date.getDayOfYear();
        double[] nightPortions = nightPortions(params);

        // Fajr: the twilight-angle time, floored by the high-latitude safety limit.
        Instant fajrTime = fromFractionalHours(date, solarTime.hourAngle(-1 * params.fajrAngle(), false));
        if (moonsighting && latitude >= 55) {
            fajrTime = plusFractionalSeconds(sunriseTime, -(nightSeconds / 7.0));
        }
        Instant fajrLimit = moonsighting
                ? plusSeconds(sunriseTime, seasonAdjustedMorningTwilightSeconds(latitude, dayOfYear, date.getYear()))
                : plusFractionalSeconds(sunriseTime, -(nightPortions[0] * nightSeconds));
        if (fajrTime == null || fajrLimit.isAfter(fajrTime)) {
            fajrTime = fajrLimit;
        }

        // Isha: a fixed interval, or the twilight-angle time capped by the safety limit.
        Instant ishaTime;
        if (params.ishaInterval() > 0) {
            ishaTime = plusMinutes(sunsetTime, params.ishaInterval());
        } else {
            ishaTime = fromFractionalHours(date, solarTime.hourAngle(-1 * params.ishaAngle(), true));
            if (moonsighting && latitude >= 55) {
                ishaTime = plusFractionalSeconds(sunsetTime, nightSeconds / 7.0);
            }
            Instant ishaLimit = moonsighting
                    ? plusSeconds(sunsetTime, seasonAdjustedEveningTwilightSeconds(latitude, dayOfYear, date.getYear(), params.shafaq()))
                    : plusFractionalSeconds(sunsetTime, nightPortions[1] * nightSeconds);
            if (ishaTime == null || ishaLimit.isBefore(ishaTime)) {
                ishaTime = ishaLimit;
            }
        }

        // Maghrib: sunset, or a Shia Maghrib angle when it falls between sunset and Isha.
        Instant maghribTime = sunsetTime;
        if (params.maghribAngle() != 0) {
            Instant angleMaghrib = fromFractionalHours(date, solarTime.hourAngle(-1 * params.maghribAngle(), true));
            if (sunsetTime.isBefore(angleMaghrib) && ishaTime.isAfter(angleMaghrib)) {
                maghribTime = angleMaghrib;
            }
        }

        Rounding rounding = params.rounding();
        return new DayEvents(
                round(plusMinutes(fajrTime, params.offsets().fajr()), rounding),
                round(plusMinutes(sunriseTime, params.offsets().sunrise()), rounding),
                round(plusMinutes(transit, params.offsets().dhuhr()), rounding),
                round(plusMinutes(asrTime, params.offsets().asr()), rounding),
                round(sunsetTime, rounding),
                round(plusMinutes(maghribTime, params.offsets().maghrib()), rounding),
                round(plusMinutes(ishaTime, params.offsets().isha()), rounding));
    }

    private static double[] nightPortions(CalculationParameters p) {
        return switch (p.highLatitudeRule()) {
            case MIDDLE_OF_THE_NIGHT -> new double[]{1.0 / 2.0, 1.0 / 2.0};
            case SEVENTH_OF_THE_NIGHT -> new double[]{1.0 / 7.0, 1.0 / 7.0};
            case TWILIGHT_ANGLE -> new double[]{p.fajrAngle() / 60.0, p.ishaAngle() / 60.0};
        };
    }

    /** Fractional UTC hours on {@code date} -> instant, matching adhan's TimeComponents + Date.UTC. */
    private static Instant fromFractionalHours(LocalDate date, double fractionalHours) {
        if (Double.isNaN(fractionalHours)) {
            return null;
        }
        int hours = (int) Math.floor(fractionalHours);
        int minutes = (int) Math.floor((fractionalHours - hours) * 60.0);
        int seconds = (int) Math.floor((fractionalHours - (hours + minutes / 60.0)) * 60.0 * 60.0);
        long offset = (long) hours * 3600L + (long) minutes * 60L + seconds;
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().plusSeconds(offset);
    }

    private static Instant plusMinutes(Instant t, double minutes) {
        return t == null ? null : plusFractionalSeconds(t, minutes * 60.0);
    }

    private static Instant plusSeconds(Instant t, long seconds) {
        return t == null ? null : t.plusSeconds(seconds);
    }

    /** Add fractional seconds, truncating to whole milliseconds toward zero (JS Date semantics). */
    private static Instant plusFractionalSeconds(Instant t, double seconds) {
        return t == null ? null : t.plusMillis((long) (seconds * 1000.0));
    }

    private static Instant round(Instant t, Rounding rounding) {
        long secondsField = Math.floorMod(t.getEpochSecond(), 60);
        long delta = switch (rounding) {
            case NEAREST -> secondsField >= 30 ? 60 - secondsField : -secondsField;
            case UP -> 60 - secondsField;
            case NONE -> 0;
        };
        return t.plusSeconds(delta).with(ChronoField.NANO_OF_SECOND, 0);
    }
}
