/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.app.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import systems.asymm.cipn.fw.contract.manager.SiteGenerationResult;
import systems.asymm.cipn.wiring.assembly.standard.SiteGenerationWiring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wiring-level integration: the fully-composed pipeline (assembly -> variants -> impls ->
 * FreeMarker) generates real pages from XML fixtures. This is where the whole stack is wired
 * together, so the integration test lives here rather than in any single service module.
 */
class SiteGenerationIntegrationTest {

    @Test
    void wiredPipelineBakesTheAuthorityTableAndLinksNeighbours(@TempDir Path out) throws Exception {
        Path data = Path.of(getClass().getResource("/gen").toURI());

        SiteGenerationResult result = SiteGenerationWiring.siteGenerationManager(data, out).generate();

        assertEquals(2, result.pagesWritten());
        assertEquals(1, result.pagesWithTimetable());

        String colombo = Files.readString(out.resolve("sri-lanka/colombo.html"));
        assertTrue(colombo.contains("All Ceylon Jamiyyathul Ulama annual timetable"));
        assertTrue(colombo.contains("05:00"), "Fajr day 1 (300 min) should render as 05:00");
        assertTrue(colombo.contains("nearby=London|United Kingdom|/united-kingdom/london.html|"),
                "NeighbourEngine should link the nearest page-city");

        String london = Files.readString(out.resolve("united-kingdom/london.html"));
        assertFalse(london.contains("authority-timetable"), "GB has no baked table");
    }
}
