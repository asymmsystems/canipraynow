/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.engine;

import static systems.asymm.cipn.svc.engine.Astronomical.apparentObliquityOfTheEcliptic;
import static systems.asymm.cipn.svc.engine.Astronomical.apparentSolarLongitude;
import static systems.asymm.cipn.svc.engine.Astronomical.ascendingLunarNodeLongitude;
import static systems.asymm.cipn.svc.engine.Astronomical.degrees;
import static systems.asymm.cipn.svc.engine.Astronomical.julianCentury;
import static systems.asymm.cipn.svc.engine.Astronomical.meanLunarLongitude;
import static systems.asymm.cipn.svc.engine.Astronomical.meanObliquityOfTheEcliptic;
import static systems.asymm.cipn.svc.engine.Astronomical.meanSiderealTime;
import static systems.asymm.cipn.svc.engine.Astronomical.meanSolarLongitude;
import static systems.asymm.cipn.svc.engine.Astronomical.nutationInLongitude;
import static systems.asymm.cipn.svc.engine.Astronomical.nutationInObliquity;
import static systems.asymm.cipn.svc.engine.Astronomical.radians;
import static systems.asymm.cipn.svc.engine.Astronomical.unwind;

/**
 * The sun's declination, right ascension and apparent sidereal time for a given
 * Julian day. Ported from adhan.js {@code SolarCoordinates}.
 */
final class SolarCoordinates {

    final double declination;
    final double rightAscension;
    final double apparentSiderealTime;

    SolarCoordinates(double julianDay) {
        double t = julianCentury(julianDay);
        double meanSolarLongitude = meanSolarLongitude(t);
        double meanLunarLongitude = meanLunarLongitude(t);
        double ascendingNode = ascendingLunarNodeLongitude(t);
        double apparentLongitude = radians(apparentSolarLongitude(t, meanSolarLongitude));
        double meanSidereal = meanSiderealTime(t);
        double nutationLongitude = nutationInLongitude(meanSolarLongitude, meanLunarLongitude, ascendingNode);
        double nutationObliquity = nutationInObliquity(meanSolarLongitude, meanLunarLongitude, ascendingNode);
        double meanObliquity = meanObliquityOfTheEcliptic(t);
        double apparentObliquity = radians(apparentObliquityOfTheEcliptic(t, meanObliquity));

        this.declination = degrees(Math.asin(Math.sin(apparentObliquity) * Math.sin(apparentLongitude)));
        this.rightAscension = unwind(degrees(
                Math.atan2(Math.cos(apparentObliquity) * Math.sin(apparentLongitude), Math.cos(apparentLongitude))));
        this.apparentSiderealTime = meanSidereal
                + nutationLongitude * Math.cos(radians(meanObliquity + nutationObliquity));
    }
}
