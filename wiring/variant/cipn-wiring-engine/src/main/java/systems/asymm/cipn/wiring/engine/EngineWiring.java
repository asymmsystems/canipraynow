/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.engine;

import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.engine.AstronomyEngine;
import systems.asymm.cipn.fw.contract.manager.ZoneResolverProvider;
import systems.asymm.cipn.svc.engine.DefaultAstronomyEngine;

/**
 * Plain-Java wiring for the engine service type: the sole place that instantiates the
 * engine impls, keeping {@code cipn-svc-engine} framework-free (ADR-029) and out of the app.
 */
public final class EngineWiring {

    private EngineWiring() {
    }

    /** The general astronomy engine (E3). */
    public static AstronomyEngine astronomyEngine() {
        return new DefaultAstronomyEngine();
    }

    /** A per-country zone-resolver provider using the nearest-centroid strategy. */
    public static ZoneResolverProvider centroidZoneResolverProvider(TimetableAccessor timetableAccessor) {
        return new CentroidZoneResolverProvider(timetableAccessor);
    }
}
