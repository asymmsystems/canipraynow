/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

import java.time.Instant;

/**
 * The seven daily solar/prayer events for one location and date, in UTC. The
 * output of the astronomy engine; the restricted-window and verdict layers read
 * from this.
 */
public record DayEvents(
        Instant fajr,
        Instant sunrise,
        Instant dhuhr,
        Instant asr,
        Instant sunset,
        Instant maghrib,
        Instant isha) {
}
