/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.assembly.standard;

import java.nio.file.Path;

import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.access.ContentAccessor;
import systems.asymm.cipn.fw.contract.access.CountryAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.engine.NeighbourEngine;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationManager;
import systems.asymm.cipn.fw.contract.manager.ZoneResolverProvider;
import systems.asymm.cipn.wiring.accessor.classpath.ClasspathAccessorWiring;
import systems.asymm.cipn.wiring.accessor.file.FileAccessorWiring;
import systems.asymm.cipn.wiring.engine.EngineWiring;
import systems.asymm.cipn.wiring.manager.ManagerWiring;

/**
 * The standard assembly: composes the wiring variants into a ready-to-run
 * {@link SiteGenerationManager} for one year. This is the single entry point an application
 * depends on - it never touches impl modules or the individual variants. The read data is
 * bundled on the classpath (ADR-0012); only the output root and the year are supplied.
 */
public final class SiteGenerationWiring {

    private SiteGenerationWiring() {
    }

    /**
     * The fully-wired site-generation manager for one year.
     *
     * @param outputRoot directory to write generated pages under
     * @param year       the Gregorian year to bake (selects {@code timetable/<year>/...})
     * @return the wired manager
     */
    public static SiteGenerationManager siteGenerationManager(Path outputRoot, int year) {
        TimetableAccessor timetableAccessor = ClasspathAccessorWiring.timetableAccessor(year);
        CityAccessor cityAccessor = ClasspathAccessorWiring.cityAccessor();
        CountryAccessor countryAccessor = ClasspathAccessorWiring.countryAccessor();
        ContentAccessor contentAccessor = FileAccessorWiring.contentAccessor(outputRoot);
        ZoneResolverProvider resolverProvider = EngineWiring.centroidZoneResolverProvider(timetableAccessor);
        NeighbourEngine neighbourEngine = EngineWiring.neighbourEngine();
        return ManagerWiring.siteGenerationManager(
                cityAccessor, countryAccessor, timetableAccessor, contentAccessor, resolverProvider, neighbourEngine);
    }
}
