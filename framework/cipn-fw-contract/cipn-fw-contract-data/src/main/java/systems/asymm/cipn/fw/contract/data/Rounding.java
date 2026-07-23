/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * How a computed event time is rounded to whole minutes for display. Some
 * authorities publish times rounded up as a precaution (iḥtiyāṭ); most round to
 * the nearest minute.
 */
public enum Rounding {

    /** Round to the nearest whole minute (&ge; 30 s rounds up, otherwise down). The default. */
    NEAREST,

    /** Always round up to the next whole minute (a safety margin used by e.g. Singapore/MUIS). */
    UP,

    /** No rounding: keep the computed seconds. */
    NONE;
}
