/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * How Fajr and Isha are bounded at high latitudes, where in summer the sun never
 * dips far enough below the horizon for true twilight to occur, so the pure
 * twilight-angle time is undefined or unreasonable.
 *
 * <p>Each rule caps how far Fajr may sit before sunrise (and Isha after sunset) as
 * a portion of the night - the interval between sunset and the next sunrise.
 */
public enum HighLatitudeRule {

    /**
     * Split the night in half: Fajr no earlier than the midpoint between sunset and
     * sunrise, Isha no later than that midpoint. The most conservative, common default.
     */
    MIDDLE_OF_THE_NIGHT,

    /**
     * Allot one seventh of the night each: Fajr within the last seventh before sunrise,
     * Isha within the first seventh after sunset.
     */
    SEVENTH_OF_THE_NIGHT,

    /**
     * Scale the night portion by the twilight angle (angle / 60), so a deeper Fajr/Isha
     * angle is allowed a proportionally larger slice of the night.
     */
    TWILIGHT_ANGLE;
}
