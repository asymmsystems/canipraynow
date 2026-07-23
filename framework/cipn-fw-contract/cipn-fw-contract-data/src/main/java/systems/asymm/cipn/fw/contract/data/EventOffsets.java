/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * Per-event adjustments in whole minutes that an authority applies on top of the
 * astronomical time - for example a temkin/iḥtiyāṭ safety margin, or the small
 * offset past true solar transit that most methods add to Dhuhr. Positive values
 * move the event later, negative earlier.
 *
 * @param fajr    minutes added to Fajr
 * @param sunrise minutes added to sunrise
 * @param dhuhr   minutes added to Dhuhr (transit); typically +1 for most methods
 * @param asr     minutes added to ʿAsr
 * @param maghrib minutes added to Maghrib
 * @param isha    minutes added to Isha
 */
public record EventOffsets(int fajr, int sunrise, int dhuhr, int asr, int maghrib, int isha) {

    /** No adjustment on any event. */
    public static final EventOffsets NONE = new EventOffsets(0, 0, 0, 0, 0, 0);
}
