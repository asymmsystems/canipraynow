/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.access;

import java.util.List;

import systems.asymm.cipn.fw.contract.data.City;

/**
 * Accessor contract for the bundled city dataset (read-only).
 *
 * <p>Named by CONSUMED TYPE (cities). The location and format are implementation details;
 * {@code ClasspathCityAccessor} reads the bundled {@code city.xml} today.
 *
 * @see Accessor
 */
public interface CityAccessor extends Accessor {

    /**
     * Every city in the dataset, in file order.
     *
     * @return the cities
     */
    List<City> all();
}
