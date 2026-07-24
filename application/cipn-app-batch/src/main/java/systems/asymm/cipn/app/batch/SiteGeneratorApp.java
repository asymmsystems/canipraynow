/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.app.batch;

import java.nio.file.Path;

import systems.asymm.cipn.fw.contract.manager.SiteGenerationManager;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationResult;
import systems.asymm.cipn.wiring.assembly.standard.SiteGenerationWiring;

/**
 * Build-time entry point for S2 site generation. Thin: it asks the wiring assembly for the
 * wired manager and runs the use case. It declares no impl (svc-*) dependencies and never
 * instantiates an impl - all composition lives in the wiring layer.
 *
 * <p>Usage: {@code SiteGeneratorApp <dataDir> <outputDir>} where {@code dataDir} holds
 * {@code cities.xml} and {@code timetables/<cc>/...}, and generated pages are written under
 * {@code outputDir}.
 */
public final class SiteGeneratorApp {

    private SiteGeneratorApp() {
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: SiteGeneratorApp <dataDir> <outputDir>");
        }
        SiteGenerationManager manager = SiteGenerationWiring.siteGenerationManager(
                Path.of(args[0]), Path.of(args[1]));

        SiteGenerationResult result = manager.generate();
        System.out.printf("Generated %d city pages (%d with a baked authority timetable); swept %d stale.%n",
                result.pagesWritten(), result.pagesWithTimetable(), result.stalePagesSwept());
    }
}
