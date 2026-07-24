/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import systems.asymm.cipn.fw.common.geo.Geometry;
import systems.asymm.cipn.fw.contract.data.City;
import systems.asymm.cipn.fw.contract.data.Neighbour;
import systems.asymm.cipn.fw.contract.engine.NeighbourEngine;

/**
 * Default {@link NeighbourEngine}: nearest cities by great-circle distance, excluding the
 * origin itself, cities without their own page, and coincident points (duplicates). Pure,
 * no I/O; uses the {@link Geometry} utility for distance.
 *
 * @see NeighbourEngine
 */
public class DefaultNeighbourEngine implements NeighbourEngine {

    @Override
    public List<Neighbour> nearest(City origin, List<City> candidates, int count) {
        List<Neighbour> neighbours = new ArrayList<>();
        for (City candidate : candidates) {
            if (!candidate.page() || candidate.slug().equals(origin.slug()) && candidate.cc().equals(origin.cc())) {
                continue;
            }
            long km = Math.round(Geometry.haversineKm(origin.lat(), origin.lng(), candidate.lat(), candidate.lng()));
            if (km == 0) {
                continue;   // same point: a duplicate entry, not a neighbour
            }
            neighbours.add(new Neighbour(candidate, (int) km));
        }
        neighbours.sort(Comparator.comparingInt(Neighbour::distanceKm));
        return List.copyOf(neighbours.subList(0, Math.min(count, neighbours.size())));
    }
}
