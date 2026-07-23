/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * Per-event adjustments in whole minutes that an authority applies on top of the
 * astronomical time (e.g. a temkin/iḥtiyāṭ margin, or a Dhuhr offset past transit).
 */
public record EventOffsets(int fajr, int sunrise, int dhuhr, int asr, int maghrib, int isha) {

    /** No adjustment on any event. */
    public static final EventOffsets NONE = new EventOffsets(0, 0, 0, 0, 0, 0);
}
