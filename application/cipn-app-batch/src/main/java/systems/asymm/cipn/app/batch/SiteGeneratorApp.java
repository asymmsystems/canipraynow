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
 * <p>Input data is bundled on the classpath (ADR-0012), so no data directory is passed; the
 * only argument is the output root. Usage: {@code SiteGeneratorApp <outputRoot> [year...]}.
 * With no years, it bakes the current year and the next (the annual-rollover contract: next
 * year's pages exist before Dec 31, no cron). The current year is written both to the site
 * root (canonical, clean URLs) and to {@code <outputRoot>/<year>/} alongside every baked year.
 */
public final class SiteGeneratorApp {

    private SiteGeneratorApp() {
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Usage: SiteGeneratorApp <outputRoot> [year...]");
        }
        Path outputRoot = Path.of(args[0]);
        List<Integer> years = years(args);
        int currentYear = years.get(0);

        // Canonical (clean URLs) = the current year, at the site root. Its sweep clears the
        // whole tree of a prior run first; the per-year passes below then rewrite each archive,
        // so a re-run converges to exactly {current, next} with no stale year left behind.
        SiteGenerationResult canonical = generate(outputRoot, currentYear);
        System.out.printf("Canonical year %d: %d pages (%d with a baked table); swept %d stale.%n",
                currentYear, canonical.pagesWritten(), canonical.pagesWithTimetable(), canonical.stalePagesSwept());

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

    /** Years to bake: the given arguments if any (sorted, de-duplicated), else current + next. */
    private static List<Integer> years(String[] args) {
        if (args.length > 1) {
            TreeSet<Integer> parsed = new TreeSet<>();
            for (int i = 1; i < args.length; i++) {
                parsed.add(Integer.parseInt(args[i]));
            }
            return new ArrayList<>(parsed);
        }
        int current = Year.now().getValue();
        return List.of(current, current + 1);
    }
}
