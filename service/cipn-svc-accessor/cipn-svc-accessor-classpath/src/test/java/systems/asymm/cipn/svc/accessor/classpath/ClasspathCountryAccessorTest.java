/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.classpath;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.data.CountryDefaults;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Reads the bundled {@code country.xml} of per-country presentation defaults. */
class ClasspathCountryAccessorTest {

    private final ClasspathCountryAccessor accessor = new ClasspathCountryAccessor();

    @Test
    void readsCountryDefaultsCaseInsensitively() {
        CountryDefaults lk = accessor.defaults("LK");
        assertEquals("All Ceylon Jamiyyathul Ulama (ACJU)", lk.authority());
        assertEquals("the ACJU (Sri Lanka) convention", lk.method());
        assertEquals(lk.method(), accessor.defaults("lk").method());
    }

    @Test
    void resolvesMadhhabWhereKnown() {
        assertEquals("Ḥanbalī", accessor.defaults("SA").madhhab());
    }

    @Test
    void unknownCountryGetsStandardDefaults() {
        CountryDefaults zz = accessor.defaults("ZZ");
        assertEquals("", zz.authority());
        assertEquals("a standard convention", zz.method());
        assertEquals("", zz.madhhab());
    }
}
