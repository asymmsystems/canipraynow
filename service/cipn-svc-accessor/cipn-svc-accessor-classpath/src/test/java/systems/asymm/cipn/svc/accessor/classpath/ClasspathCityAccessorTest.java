/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.classpath;

import java.util.List;

import org.junit.jupiter.api.Test;

import systems.asymm.cipn.fw.contract.data.City;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reads the bundled {@code city.xml} and binds each row to a {@link City}. */
class ClasspathCityAccessorTest {

    private final ClasspathCityAccessor accessor = new ClasspathCityAccessor();

    @Test
    void readsEveryCityInFileOrder() {
        List<City> cities = accessor.all();
        assertEquals(2, cities.size());
        assertEquals("Colombo", cities.get(0).name());
        assertEquals("LK", cities.get(0).cc());
        assertEquals("sri-lanka", cities.get(0).countrySlug());
        assertTrue(cities.get(0).page());
        assertEquals("London", cities.get(1).name());
    }
}
