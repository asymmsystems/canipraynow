/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.engine;

import systems.asymm.cipn.fw.contract.data.Coordinates;
import systems.asymm.cipn.fw.contract.data.Shafaq;

/**
 * Solar-position astronomy, ported faithfully from the vendored adhan.js
 * ({@code assets/js/vendor/adhan.js}) so this JVM engine reproduces the browser
 * engine to the minute. All angles are in degrees unless noted; the golden-vector
 * parity test guards every constant here.
 */
final class Astronomical {

    private Astronomical() {
    }

    static double radians(double deg) {
        return deg * Math.PI / 180.0;
    }

    static double degrees(double rad) {
        return rad * 180.0 / Math.PI;
    }

    /** Floating-point modulo matching JS: t - n * floor(t / n). */
    static double mod(double t, double n) {
        return t - n * Math.floor(t / n);
    }

    static double unwind(double angle) {
        return mod(angle, 360.0);
    }

    /** Normalize to the range (-180, 180]. */
    static double closest(double angle) {
        return (angle >= -180 && angle <= 180) ? angle : angle - 360.0 * Math.round(angle / 360.0);
    }

    static double meanSolarLongitude(double t) {
        double l0 = 280.4664567 + 36000.76983 * t + 0.0003032 * Math.pow(t, 2);
        return unwind(l0);
    }

    static double meanLunarLongitude(double t) {
        double lp = 218.3165 + 481267.8813 * t;
        return unwind(lp);
    }

    static double ascendingLunarNodeLongitude(double t) {
        double omega = 125.04452 - 1934.136261 * t + 0.0020708 * Math.pow(t, 2) + Math.pow(t, 3) / 450000.0;
        return unwind(omega);
    }

    static double meanSolarAnomaly(double t) {
        double m = 357.52911 + 35999.05029 * t - 0.0001537 * Math.pow(t, 2);
        return unwind(m);
    }

    static double solarEquationOfTheCenter(double t, double meanAnomaly) {
        double mrad = radians(meanAnomaly);
        double a = (1.914602 - 0.004817 * t - 0.000014 * Math.pow(t, 2)) * Math.sin(mrad);
        double b = (0.019993 - 0.000101 * t) * Math.sin(2 * mrad);
        double c = 0.000289 * Math.sin(3 * mrad);
        return a + b + c;
    }

    static double apparentSolarLongitude(double t, double meanLongitude) {
        double longitude = meanLongitude + solarEquationOfTheCenter(t, meanSolarAnomaly(t));
        double omega = 125.04 - 1934.136 * t;
        double lambda = longitude - 0.00569 - 0.00478 * Math.sin(radians(omega));
        return unwind(lambda);
    }

    static double meanObliquityOfTheEcliptic(double t) {
        return 23.439291 - 0.013004167 * t - 0.0000001639 * Math.pow(t, 2) + 0.0000005036 * Math.pow(t, 3);
    }

    static double apparentObliquityOfTheEcliptic(double t, double meanObliquity) {
        double o = 125.04 - 1934.136 * t;
        return meanObliquity + 0.00256 * Math.cos(radians(o));
    }

    static double meanSiderealTime(double t) {
        double jd = t * 36525.0 + 2451545.0;
        double theta = 280.46061837 + 360.98564736629 * (jd - 2451545.0)
                + 0.000387933 * Math.pow(t, 2) - Math.pow(t, 3) / 38710000.0;
        return unwind(theta);
    }

    static double nutationInLongitude(double solarLongitude, double lunarLongitude, double ascendingNode) {
        double a = -17.2 / 3600.0 * Math.sin(radians(ascendingNode));
        double b = 1.32 / 3600.0 * Math.sin(2 * radians(solarLongitude));
        double c = 0.23 / 3600.0 * Math.sin(2 * radians(lunarLongitude));
        double d = 0.21 / 3600.0 * Math.sin(2 * radians(ascendingNode));
        return a - b - c + d;
    }

    static double nutationInObliquity(double solarLongitude, double lunarLongitude, double ascendingNode) {
        double a = 9.2 / 3600.0 * Math.cos(radians(ascendingNode));
        double b = 0.57 / 3600.0 * Math.cos(2 * radians(solarLongitude));
        double c = 0.10 / 3600.0 * Math.cos(2 * radians(lunarLongitude));
        double d = 0.09 / 3600.0 * Math.cos(2 * radians(ascendingNode));
        return a + b + c - d;
    }

    static double altitudeOfCelestialBody(double latitude, double declination, double localHourAngle) {
        double a = Math.sin(radians(latitude)) * Math.sin(radians(declination));
        double b = Math.cos(radians(latitude)) * Math.cos(radians(declination)) * Math.cos(radians(localHourAngle));
        return degrees(Math.asin(a + b));
    }

    static double approximateTransit(double longitude, double siderealTime, double rightAscension) {
        double lw = longitude * -1;
        double h = mod((rightAscension + lw - siderealTime) / 360.0, 1);
        double l = mod((12.0 - longitude / 15.0) / 24.0, 1);
        if (h - l > 0.5) {
            return h - 1;
        }
        if (l - h > 0.5) {
            return h + 1;
        }
        return h;
    }

    static double correctedTransit(double approximateTransit, double longitude, double siderealTime,
                                   double rightAscension, double previousRightAscension, double nextRightAscension) {
        double lw = longitude * -1;
        double theta = unwind(siderealTime + 360.985647 * approximateTransit);
        double a = unwind(interpolateAngles(rightAscension, previousRightAscension, nextRightAscension, approximateTransit));
        double h = closest(theta - lw - a);
        double dm = h / -360.0;
        return (approximateTransit + dm) * 24.0;
    }

    static double correctedHourAngle(double approximateTransit, double angle, Coordinates coordinates,
                                     boolean afterTransit, double siderealTime, double rightAscension,
                                     double previousRightAscension, double nextRightAscension,
                                     double declination, double previousDeclination, double nextDeclination) {
        double lw = coordinates.longitude() * -1;
        double term1 = Math.sin(radians(angle)) - Math.sin(radians(coordinates.latitude())) * Math.sin(radians(declination));
        double term2 = Math.cos(radians(coordinates.latitude())) * Math.cos(radians(declination));
        double h0 = degrees(Math.acos(term1 / term2));
        double m = afterTransit ? approximateTransit + h0 / 360.0 : approximateTransit - h0 / 360.0;
        double theta = unwind(siderealTime + 360.985647 * m);
        double a = unwind(interpolateAngles(rightAscension, previousRightAscension, nextRightAscension, m));
        double delta = interpolate(declination, previousDeclination, nextDeclination, m);
        double localHourAngle = theta - lw - a;
        double h = altitudeOfCelestialBody(coordinates.latitude(), delta, localHourAngle) - angle;
        double dm = h / (360.0 * Math.cos(radians(delta)) * Math.cos(radians(coordinates.latitude())) * Math.sin(radians(localHourAngle)));
        return (m + dm) * 24.0;
    }

    static double interpolate(double y2, double y1, double y3, double factor) {
        double a = y2 - y1;
        double b = y3 - y2;
        double c = b - a;
        return y2 + (factor / 2.0) * (a + b + factor * c);
    }

    static double interpolateAngles(double y2, double y1, double y3, double factor) {
        double a = unwind(y2 - y1);
        double b = unwind(y3 - y2);
        double c = b - a;
        return y2 + (factor / 2.0) * (a + b + factor * c);
    }

    static double julianDay(int year, int month, int day, double hours) {
        int y = month > 2 ? year : year - 1;
        int m = month > 2 ? month : month + 12;
        double d = day + hours / 24.0;
        int a = y / 100;
        int b = 2 - a + a / 4;
        long i0 = (long) Math.floor(365.25 * (y + 4716));
        long i1 = (long) Math.floor(30.6001 * (m + 1));
        return i0 + i1 + d + b - 1524.5;
    }

    static double julianCentury(double julianDay) {
        return (julianDay - 2451545.0) / 36525.0;
    }

    static int daysSinceSolstice(int dayOfYear, int year, double latitude) {
        int daysInYear = isLeapYear(year) ? 366 : 365;
        int southernOffset = isLeapYear(year) ? 173 : 172;
        int days;
        if (latitude >= 0) {
            days = dayOfYear + 10;
            if (days >= daysInYear) {
                days -= daysInYear;
            }
        } else {
            days = dayOfYear - southernOffset;
            if (days < 0) {
                days += daysInYear;
            }
        }
        return days;
    }

    static boolean isLeapYear(int year) {
        return !(year % 4 != 0 || (year % 100 == 0 && year % 400 != 0));
    }

    /** Season-adjusted morning twilight (Moonsighting Committee). Returns seconds to add to sunrise (negative). */
    static long seasonAdjustedMorningTwilightSeconds(double latitude, int dayOfYear, int year) {
        double a = 75 + (28.65 / 55.0) * Math.abs(latitude);
        double b = 75 + (19.44 / 55.0) * Math.abs(latitude);
        double c = 75 + (32.74 / 55.0) * Math.abs(latitude);
        double d = 75 + (48.10 / 55.0) * Math.abs(latitude);
        int dyy = daysSinceSolstice(dayOfYear, year, latitude);
        double adjustment;
        if (dyy < 91) {
            adjustment = a + (b - a) / 91.0 * dyy;
        } else if (dyy < 137) {
            adjustment = b + (c - b) / 46.0 * (dyy - 91);
        } else if (dyy < 183) {
            adjustment = c + (d - c) / 46.0 * (dyy - 137);
        } else if (dyy < 229) {
            adjustment = d + (c - d) / 46.0 * (dyy - 183);
        } else if (dyy < 275) {
            adjustment = c + (b - c) / 46.0 * (dyy - 229);
        } else {
            adjustment = b + (a - b) / 91.0 * (dyy - 275);
        }
        return Math.round(adjustment * -60.0);
    }

    /** Season-adjusted evening twilight (Moonsighting Committee). Returns seconds to add to sunset. */
    static long seasonAdjustedEveningTwilightSeconds(double latitude, int dayOfYear, int year, Shafaq shafaq) {
        double a, b, c, d;
        switch (shafaq) {
            case AHMER -> {
                a = 62 + (17.40 / 55.0) * Math.abs(latitude);
                b = 62 - (7.16 / 55.0) * Math.abs(latitude);
                c = 62 + (5.12 / 55.0) * Math.abs(latitude);
                d = 62 + (19.44 / 55.0) * Math.abs(latitude);
            }
            case ABYAD -> {
                a = 75 + (25.60 / 55.0) * Math.abs(latitude);
                b = 75 + (7.16 / 55.0) * Math.abs(latitude);
                c = 75 + (36.84 / 55.0) * Math.abs(latitude);
                d = 75 + (81.84 / 55.0) * Math.abs(latitude);
            }
            default -> {
                a = 75 + (25.60 / 55.0) * Math.abs(latitude);
                b = 75 + (2.050 / 55.0) * Math.abs(latitude);
                c = 75 - (9.210 / 55.0) * Math.abs(latitude);
                d = 75 + (6.140 / 55.0) * Math.abs(latitude);
            }
        }
        int dyy = daysSinceSolstice(dayOfYear, year, latitude);
        double adjustment;
        if (dyy < 91) {
            adjustment = a + (b - a) / 91.0 * dyy;
        } else if (dyy < 137) {
            adjustment = b + (c - b) / 46.0 * (dyy - 91);
        } else if (dyy < 183) {
            adjustment = c + (d - c) / 46.0 * (dyy - 137);
        } else if (dyy < 229) {
            adjustment = d + (c - d) / 46.0 * (dyy - 183);
        } else if (dyy < 275) {
            adjustment = c + (b - c) / 46.0 * (dyy - 229);
        } else {
            adjustment = b + (a - b) / 91.0 * (dyy - 275);
        }
        return Math.round(adjustment * 60.0);
    }
}
