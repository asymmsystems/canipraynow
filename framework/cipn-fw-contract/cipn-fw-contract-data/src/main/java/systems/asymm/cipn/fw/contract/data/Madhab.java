/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * The juristic school, whose only effect on the astronomy is the ʿAsr shadow
 * factor: ʿAsr begins when an object's shadow, measured from its noon minimum,
 * grows to the object's height times this factor.
 */
public enum Madhab {

    /**
     * Shāfiʿī (also Mālikī and Ḥanbalī): ʿAsr when the shadow equals the object's own
     * length (factor 1) - the earlier ʿAsr.
     */
    SHAFI(1),

    /**
     * Ḥanafī: ʿAsr when the shadow equals twice the object's length (factor 2) - the
     * later ʿAsr.
     */
    HANAFI(2);

    private final int shadowLength;

    Madhab(int shadowLength) {
        this.shadowLength = shadowLength;
    }

    /**
     * The ʿAsr shadow multiplier: 1 for Shāfiʿī, 2 for Ḥanafī.
     *
     * @return the shadow-length factor
     */
    public int shadowLength() {
        return shadowLength;
    }
}
