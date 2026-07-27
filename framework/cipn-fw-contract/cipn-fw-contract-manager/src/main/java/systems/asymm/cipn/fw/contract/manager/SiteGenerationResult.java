/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.manager;

/**
 * The outcome of a site-generation run.
 *
 * @param pagesWritten        number of city pages written
 * @param stalePagesSwept     number of prior generated pages removed before the run
 * @param pagesWithTimetable  number of pages that got a baked authority timetable (E1)
 */
public record SiteGenerationResult(int pagesWritten, int stalePagesSwept, int pagesWithTimetable) {
}
