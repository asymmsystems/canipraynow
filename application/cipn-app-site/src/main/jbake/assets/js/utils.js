/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * Shared formatting helpers. All display formatting honours the selected
 * location's IANA timezone, not the device timezone - a preset city page
 * viewed from elsewhere must still read in that city's clock.
 *
 * @author Fahim Farook
 */

/** Format a Date as wall-clock time in a timezone. */
export function fmtTime(date, tz, clock24) {
    try {
        return new Intl.DateTimeFormat(clock24 ? 'en-GB' : 'en-US', {
            hour: clock24 ? '2-digit' : 'numeric',
            minute: '2-digit',
            hour12: !clock24,
            timeZone: tz
        }).format(date).toLowerCase();
    } catch (e) {
        return new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit' }).format(date);
    }
}

/**
 * The absolute instant at which the clock in IANA zone `tz` reads `minutes` past local midnight on
 * the civil date y-mo-d (mo is 0-based, matching Date). Published authority times are wall-clock in
 * the city's zone, so they MUST be anchored in that zone, never the device's (ADR-0019). Pure Intl,
 * no timezone library (ADR-0002): take the wall time as if it were UTC, then subtract the zone's
 * offset at that instant; one refinement pass resolves the offset correctly across a DST boundary.
 */
export function wallClockInstant(y, mo, d, minutes, tz) {
    const asIfUtc = Date.UTC(y, mo, d, 0, minutes, 0);
    let ts = asIfUtc - tzOffsetMs(tz, asIfUtc);
    ts = asIfUtc - tzOffsetMs(tz, ts);   // second pass: offset read at the resolved instant (DST)
    return new Date(ts);
}

/** Zone offset in milliseconds east of UTC in `tz` at instant `ts`: (its wall clock read as UTC) - ts. */
function tzOffsetMs(tz, ts) {
    const p = new Intl.DateTimeFormat('en-US', {
        timeZone: tz, hourCycle: 'h23',
        year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', second: '2-digit'
    }).formatToParts(new Date(ts)).reduce((a, x) => (a[x.type] = x.value, a), {});
    return Date.UTC(Number(p.year), Number(p.month) - 1, Number(p.day), Number(p.hour), Number(p.minute), Number(p.second)) - ts;
}

/** mm:ss for short spans, h:mm:ss beyond an hour (spec §5.3). */
export function fmtCountdown(ms) {
    const total = Math.max(0, Math.floor(ms / 1000));
    const h = Math.floor(total / 3600);
    const m = Math.floor((total % 3600) / 60);
    const s = total % 60;
    const pad = (n) => String(n).padStart(2, '0');
    return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`;
}

/** "in 1 h 12 m" style relative time for the prayer list. */
export function fmtRelative(ms) {
    const totalMin = Math.max(0, Math.round(ms / 60000));
    const h = Math.floor(totalMin / 60);
    const m = totalMin % 60;
    if (h === 0) return `${m} m`;
    if (m === 0) return `${h} h`;
    return `${h} h ${m} m`;
}

/** Civil weekday index (0=Sunday..5=Friday=5) in a timezone, for Friday detection (spec §12). */
export function isFridayIn(tz, date) {
    try {
        const wd = new Intl.DateTimeFormat('en-US', { weekday: 'short', timeZone: tz }).format(date);
        return wd === 'Fri';
    } catch (e) {
        return date.getDay() === 5;
    }
}

/** Great-circle distance in km (haversine). Used to decide whether a device has actually moved. */
export function distanceKm(a, b) {
    const R = 6371;
    const toRad = (d) => (d * Math.PI) / 180;
    const dLat = toRad(b.lat - a.lat);
    const dLng = toRad(b.lng - a.lng);
    const h = Math.sin(dLat / 2) ** 2
        + Math.cos(toRad(a.lat)) * Math.cos(toRad(b.lat)) * Math.sin(dLng / 2) ** 2;
    return 2 * R * Math.asin(Math.min(1, Math.sqrt(h)));
}

/** Escape text nodes for hand-built SVG/HTML strings. */
export function esc(s) {
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
