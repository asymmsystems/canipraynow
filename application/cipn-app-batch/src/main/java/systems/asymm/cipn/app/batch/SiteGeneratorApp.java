/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.app.batch;

import java.nio.file.Path;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import systems.asymm.cipn.fw.contract.manager.SiteGenerationManager;
import systems.asymm.cipn.fw.contract.manager.SiteGenerationResult;
import systems.asymm.cipn.wiring.assembly.standard.SiteGenerationWiring;

/**
 * Build-time entry point for S2 site generation. Thin: it asks the wiring assembly for the
 * wired manager (per year) and runs the use case. It declares no impl (svc-*) dependencies
 * and never instantiates an impl - all composition lives in the wiring layer.
 *
 * <p>Input data is bundled on the classpath (ADR-0012), so no data directory is passed.
 * Usage: {@code SiteGeneratorApp <outputRoot> [--canonical-only] [year...]}.
 * <ul>
 *   <li>Default: bakes the current year and the next (the annual-rollover contract - next
 *       year's pages exist before Dec 31, no cron), writing the current year to the site root
 *       (canonical, clean URLs) and each year to {@code <outputRoot>/<year>/}.</li>
 *   <li>{@code --canonical-only}: writes only the current year to the root, no per-year archive
 *       directories. This is what the JBake site build consumes (one page per city, no
 *       duplicate /YYYY/ pages in the index).</li>
 * </ul>
 */
public final class SiteGeneratorApp {

    private static final String CANONICAL_ONLY = "--canonical-only";

    private SiteGeneratorApp() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    "Usage: SiteGeneratorApp <outputRoot> [--canonical-only] [year...]");
        }
        Path outputRoot = Path.of(args[0]);

        boolean canonicalOnly = false;
        TreeSet<Integer> yearArgs = new TreeSet<>();
        for (int i = 1; i < args.length; i++) {
            if (CANONICAL_ONLY.equals(args[i])) {
                canonicalOnly = true;
            } else {
                yearArgs.add(Integer.parseInt(args[i]));
            }
        }
        int currentYear = yearArgs.isEmpty() ? Year.now().getValue() : yearArgs.first();

        // Canonical (clean URLs) = the current year, at the site root. Its sweep clears the
        // whole tree of a prior run first; the per-year passes below (if any) then rewrite each
        // archive, so a re-run converges to exactly the requested years with no stale year left.
        SiteGenerationResult canonical = generate(outputRoot, currentYear);
        System.out.printf("Canonical year %d: %d pages (%d with a baked table); swept %d stale.%n",
                currentYear, canonical.pagesWritten(), canonical.pagesWithTimetable(), canonical.stalePagesSwept());

        if (canonicalOnly) {
            return;
        }

        List<Integer> years = yearArgs.isEmpty()
                ? List.of(currentYear, currentYear + 1) : new ArrayList<>(yearArgs);
        for (int year : years) {
            SiteGenerationResult r = generate(outputRoot.resolve(String.valueOf(year)), year);
            System.out.printf("Year %d: %d pages (%d with a baked table); swept %d stale.%n",
                    year, r.pagesWritten(), r.pagesWithTimetable(), r.stalePagesSwept());
            if (r.pagesWithTimetable() == 0) {
                System.err.printf("WARNING: year %d baked no authority tables - is timetable/%d data present?%n",
                        year, year);
            }
        }
    }

    private static SiteGenerationResult generate(Path root, int year) {
        SiteGenerationManager manager = SiteGenerationWiring.siteGenerationManager(root, year);
        return manager.generate();
    }
}
