/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

import java.time.Instant;

/**
 * The seven daily solar/prayer events for one location and date, as UTC instants.
 * The output of the astronomy engine; the restricted-window and verdict layers read
 * from this.
 *
 * @param fajr    true dawn - start of the Fajr prayer
 * @param sunrise sunrise - end of Fajr, start of the after-Fajr restricted window
 * @param dhuhr   solar transit (midday) plus any method offset - start of Dhuhr
 * @param asr     start of the ʿAsr prayer (per the madhab shadow factor)
 * @param sunset  true sunset - the astronomical event the sunset window anchors to
 * @param maghrib start of the Maghrib prayer (sunset, or a Shia angle after it)
 * @param isha    start of the Isha prayer (twilight angle or fixed interval)
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
