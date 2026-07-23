/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.manager;

/**
 * Manager contract for the build-time site-generation use case (S2): produce the per-city
 * content, baking each authority-table city's annual timetable (E1) into its page.
 *
 * <p>Orchestration only - it delegates zone resolution to an engine, table reads to an
 * accessor, rendering to a template, and writing to an accessor.
 *
 * @see Manager
 */
public interface SiteGenerationManager extends Manager {

    /**
     * Generate the site content, replacing the previous run's generated pages.
     *
     * @return counts of what was produced
     */
    SiteGenerationResult generate();
}
