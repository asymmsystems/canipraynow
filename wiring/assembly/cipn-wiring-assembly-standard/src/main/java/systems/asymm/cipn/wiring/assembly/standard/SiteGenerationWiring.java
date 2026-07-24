/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.assembly.standard;

import java.nio.file.Path;

import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.access.ContentAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.fw.contract.manager.ZoneResolverProvider;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationManager;
import systems.asymm.cipn.wiring.accessor.file.FileAccessorWiring;
import systems.asymm.cipn.wiring.engine.EngineWiring;
import systems.asymm.cipn.wiring.manager.ManagerWiring;

/**
 * The standard assembly: composes the wiring variants into a ready-to-run
 * {@link SiteGenerationManager}. This is the single entry point an application depends on -
 * it never touches impl modules or the individual variants.
 */
public final class SiteGenerationWiring {

    private SiteGenerationWiring() {
    }

    /**
     * The fully-wired site-generation manager.
     *
     * @param dataDir   directory holding {@code cities.xml} and {@code timetables/<cc>/...}
     * @param outputDir directory to write generated pages under
     * @return the wired manager
     */
    public static SiteGenerationManager siteGenerationManager(Path dataDir, Path outputDir) {
        TimetableAccessor timetableAccessor = FileAccessorWiring.timetableAccessor(dataDir.resolve("timetables"));
        CityAccessor cityAccessor = FileAccessorWiring.cityAccessor(dataDir.resolve("cities.xml"));
        ContentAccessor contentAccessor = FileAccessorWiring.contentAccessor(outputDir);
        ZoneResolverProvider resolverProvider = EngineWiring.centroidZoneResolverProvider(timetableAccessor);
        return ManagerWiring.siteGenerationManager(cityAccessor, timetableAccessor, contentAccessor, resolverProvider);
    }
}
