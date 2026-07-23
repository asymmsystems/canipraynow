/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * The juristic school, which sets the ʿAsr shadow factor: Shāfiʿī (and Mālikī,
 * Ḥanbalī) use a factor of 1, Ḥanafī a factor of 2.
 */
public enum Madhab {
    SHAFI(1),
    HANAFI(2);

    private final int shadowLength;

    Madhab(int shadowLength) {
        this.shadowLength = shadowLength;
    }

    public int shadowLength() {
        return shadowLength;
    }
}
