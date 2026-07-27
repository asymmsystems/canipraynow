/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.engine;

import java.util.List;

import systems.asymm.cipn.fw.contract.data.City;
import systems.asymm.cipn.fw.contract.data.Neighbour;

/**
 * Engine contract for finding a city's nearest neighbours (business rule + geometry) - the
 * related-city links baked onto a page. Pure and I/O-free.
 *
 * @see Engine
 */
public interface NeighbourEngine extends Engine {

    /**
     * The nearest cities to an origin, closest first.
     *
     * @param origin     the city whose neighbours are wanted
     * @param candidates all cities to consider (the origin, non-page cities and coincident
     *                   points are excluded)
     * @param count      the maximum number of neighbours to return
     * @return up to {@code count} neighbours, nearest first
     */
    List<Neighbour> nearest(City origin, List<City> candidates, int count);
}
