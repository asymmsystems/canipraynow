/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.file;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import systems.asymm.cipn.fw.contract.data.City;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reads the fixture {@code cities.xml} and checks fields and kebab-case attribute binding.
 */
class FileCityAccessorTest {

    private List<City> cities() throws Exception {
        Path file = Path.of(getClass().getResource("/cities.xml").toURI());
        return new FileCityAccessor(file).all();
    }

    @Test
    void readsEveryCityInOrder() throws Exception {
        List<City> cities = cities();
        assertEquals(3, cities.size());
        City colombo = cities.get(0);
        assertEquals("Colombo", colombo.name());
        assertEquals("LK", colombo.cc());
        assertEquals("sri-lanka", colombo.countrySlug());
        assertEquals(6.9271, colombo.lat());
        assertEquals("Asia/Colombo", colombo.tz());
        assertTrue(colombo.page());
    }

    @Test
    void carriesThePageFlag() throws Exception {
        assertFalse(cities().get(2).page());
    }
}
