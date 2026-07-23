/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * One day of an authority's published timetable: the five prayer times plus sunrise,
 * as whole minutes from local midnight (the authority's own values, not computed).
 *
 * @param month   month of year, 1-12
 * @param day     day of month, 1-31
 * @param fajr    minutes from local midnight
 * @param sunrise minutes from local midnight
 * @param dhuhr   minutes from local midnight
 * @param asr     minutes from local midnight
 * @param maghrib minutes from local midnight
 * @param isha    minutes from local midnight
 */
public record TimetableRow(int month, int day, int fajr, int sunrise, int dhuhr, int asr, int maghrib, int isha) {
}
