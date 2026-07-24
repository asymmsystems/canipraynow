/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.engine;

import systems.asymm.cipn.fw.contract.data.Coordinates;

import static systems.asymm.cipn.svc.engine.Astronomical.approximateTransit;
import static systems.asymm.cipn.svc.engine.Astronomical.correctedHourAngle;
import static systems.asymm.cipn.svc.engine.Astronomical.correctedTransit;
import static systems.asymm.cipn.svc.engine.Astronomical.degrees;
import static systems.asymm.cipn.svc.engine.Astronomical.julianDay;
import static systems.asymm.cipn.svc.engine.Astronomical.radians;

/**
 * Transit (solar noon), sunrise and sunset for one calendar day at one location,
 * plus the hour-angle helpers used for Fajr, ʿAsr, Maghrib and Isha. Ported from
 * adhan.js {@code SolarTime}. All results are fractional hours in UTC.
 */
final class SolarTime {

    private final Coordinates observer;
    private final SolarCoordinates solar;
    private final SolarCoordinates prevSolar;
    private final SolarCoordinates nextSolar;
    private final double approxTransit;

    final double transit;
    final double sunrise;
    final double sunset;

    SolarTime(int year, int month, int day, Coordinates observer) {
        double jd = julianDay(year, month, day, 0);
        this.observer = observer;
        this.solar = new SolarCoordinates(jd);
        this.prevSolar = new SolarCoordinates(jd - 1);
        this.nextSolar = new SolarCoordinates(jd + 1);

        double m0 = approximateTransit(observer.longitude(), solar.apparentSiderealTime, solar.rightAscension);
        double solarAltitude = -50.0 / 60.0;

        this.approxTransit = m0;
        this.transit = correctedTransit(m0, observer.longitude(), solar.apparentSiderealTime,
                solar.rightAscension, prevSolar.rightAscension, nextSolar.rightAscension);
        this.sunrise = correctedHourAngle(m0, solarAltitude, observer, false, solar.apparentSiderealTime,
                solar.rightAscension, prevSolar.rightAscension, nextSolar.rightAscension,
                solar.declination, prevSolar.declination, nextSolar.declination);
        this.sunset = correctedHourAngle(m0, solarAltitude, observer, true, solar.apparentSiderealTime,
                solar.rightAscension, prevSolar.rightAscension, nextSolar.rightAscension,
                solar.declination, prevSolar.declination, nextSolar.declination);
    }

    double hourAngle(double angle, boolean afterTransit) {
        return correctedHourAngle(approxTransit, angle, observer, afterTransit, solar.apparentSiderealTime,
                solar.rightAscension, prevSolar.rightAscension, nextSolar.rightAscension,
                solar.declination, prevSolar.declination, nextSolar.declination);
    }

    double afternoon(int shadowLength) {
        double tangent = Math.abs(observer.latitude() - solar.declination);
        double inverse = shadowLength + Math.tan(radians(tangent));
        double angle = degrees(Math.atan(1.0 / inverse));
        return hourAngle(angle, true);
    }
}
