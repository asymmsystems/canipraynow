/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.manager;

import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.access.ContentAccessor;
import systems.asymm.cipn.fw.contract.access.CountryAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.engine.NeighbourEngine;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationManager;
import systems.asymm.cipn.fw.contract.manager.ZoneResolverProvider;
import systems.asymm.cipn.svc.manager.DefaultSiteGenerationManager;

/**
 * Plain-Java wiring for the manager service type: the sole place that instantiates the
 * site-generation manager, injecting its collaborators, keeping {@code cipn-svc-manager}
 * out of the app.
 */
public final class ManagerWiring {

    private ManagerWiring() {
    }

    public static SiteGenerationManager siteGenerationManager(
            CityAccessor cityAccessor, CountryAccessor countryAccessor,
            TimetableAccessor timetableAccessor, ContentAccessor contentAccessor,
            ZoneResolverProvider resolverProvider, NeighbourEngine neighbourEngine) {
        return new DefaultSiteGenerationManager(
                cityAccessor, countryAccessor, timetableAccessor, contentAccessor,
                resolverProvider, neighbourEngine);
    }
}
