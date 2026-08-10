/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * Per-country defaults from the authority register (country.xml): the official authority, the
 * calculation method as both a canonical key and a display phrase, and the predominant madhhab.
 * {@code authority}, {@code method} and {@code madhhab} are display strings; {@code methodKey} is
 * the machine-readable convention id (e.g. {@code "UmmAlQura"}) that the engine computes with and
 * that maps to {@code calculation-method.xml}. {@code authority} and {@code madhhab} may be empty.
 *
 * @param cc              ISO 3166-1 alpha-2 country code (upper-case)
 * @param authority       the authority's display name, or empty
 * @param method          the calculation-method phrase (e.g. "the Umm al-Qura convention")
 * @param methodKey       the calculation-method key (e.g. {@code "UmmAlQura"}); the compute identifier
 * @param madhhab         the predominant madhhab display name, or empty
 * @param redistributable whether the authority's table may be republished: {@code "yes"}, {@code "no"},
 *                        or {@code "unknown"}. Gates whether S2 bakes the authority table when the
 *                        redistribution flag is enforced (ADR-0017 / T-31)
 * @param methodNote      an optional public-facing sentence explaining a notable method choice for the
 *                        country (e.g. why times are computed rather than taken from the local
 *                        authority's own table); empty when there is nothing to explain
 */
public record CountryDefaults(String cc, String authority, String method, String methodKey, String madhhab,
        String redistributable, String methodNote) {
}
