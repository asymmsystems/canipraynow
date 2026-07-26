/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.access;

import systems.asymm.cipn.fw.contract.data.CountryDefaults;

/**
 * Accessor contract for per-country presentation defaults (authority, method phrase, madhhab).
 *
 * <p>Named by CONSUMED TYPE (country defaults). The location and format are implementation
 * details; {@code ClasspathCountryAccessor} reads the bundled {@code country.xml} today.
 *
 * @see Accessor
 */
public interface CountryAccessor extends Accessor {

    /**
     * The defaults for a country.
     *
     * @param countryCode ISO 3166-1 alpha-2 code
     * @return the country's defaults, or empty-string defaults if the country is unknown
     */
    CountryDefaults defaults(String countryCode);
}
