/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * The twilight (<em>shafaq</em>) definition that marks the start of Isha, used by
 * the Moonsighting Committee's seasonal evening-twilight model.
 *
 * <p>After sunset the glow on the horizon fades in two stages: first the red
 * (<em>aḥmar</em>) glow, then the fainter white (<em>abyaḍ</em>) glow. Which one's
 * disappearance begins Isha differs by school, so the same night yields a slightly
 * earlier or later Isha depending on this choice.
 */
public enum Shafaq {

    /**
     * The committee's blended default. A seasonally-adjusted model that is neither the
     * pure red nor pure white limit; the standard choice unless a school is specified.
     */
    GENERAL,

    /**
     * Red twilight (<em>shafaq aḥmar</em>): Isha at the disappearance of the red glow -
     * the earlier Isha. The majority (Shāfiʿī, Mālikī, Ḥanbalī) position.
     */
    AHMER,

    /**
     * White twilight (<em>shafaq abyaḍ</em>): Isha at the disappearance of the later
     * white glow - the later Isha. The Ḥanafī position.
     */
    ABYAD;
}
