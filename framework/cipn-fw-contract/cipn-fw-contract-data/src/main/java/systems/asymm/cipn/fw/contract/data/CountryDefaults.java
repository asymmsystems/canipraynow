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
 * @param cc        ISO 3166-1 alpha-2 country code (upper-case)
 * @param authority the authority's display name, or empty
 * @param method    the calculation-method phrase (e.g. "the Umm al-Qura convention")
 * @param methodKey the calculation-method key (e.g. {@code "UmmAlQura"}); the compute identifier
 * @param madhhab   the predominant madhhab display name, or empty
 */
public record CountryDefaults(String cc, String authority, String method, String methodKey, String madhhab) {
}
