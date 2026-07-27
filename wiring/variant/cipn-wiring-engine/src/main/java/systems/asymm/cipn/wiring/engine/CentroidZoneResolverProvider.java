/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.engine;

import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.engine.ZoneResolver;
import systems.asymm.cipn.fw.contract.manager.ZoneResolverProvider;
import systems.asymm.cipn.svc.engine.NearestCentroidZoneResolver;

/**
 * Wiring: supplies a nearest-centroid {@link ZoneResolver} per country, built from that
 * country's zones. The working strategy until per-country coverage polygons are sourced
 * (S1/T-26), at which point this provider selects {@code PolygonZoneResolver} where coverage
 * exists. Composition logic, so it lives in the wiring layer, not the app or engine impl.
 */
final class CentroidZoneResolverProvider implements ZoneResolverProvider {

    private final TimetableAccessor timetableAccessor;

    CentroidZoneResolverProvider(TimetableAccessor timetableAccessor) {
        this.timetableAccessor = timetableAccessor;
    }

    @Override
    public ZoneResolver resolverFor(String countryCode) {
        return new NearestCentroidZoneResolver(timetableAccessor.zones(countryCode));
    }
}
