/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

/**
 * Per-country presentation defaults shown on a city page: the official authority, the
 * calculation-method phrase, and the predominant madhhab. These are display strings (already
 * resolved to their phrases), not keys. {@code authority} and {@code madhhab} may be empty
 * when a country has none.
 *
 * @param cc        ISO 3166-1 alpha-2 country code (upper-case)
 * @param authority the authority's display name, or empty
 * @param method    the calculation-method phrase (e.g. "the Umm al-Qura convention")
 * @param madhhab   the predominant madhhab display name, or empty
 */
public record CountryDefaults(String cc, String authority, String method, String madhhab) {
}
