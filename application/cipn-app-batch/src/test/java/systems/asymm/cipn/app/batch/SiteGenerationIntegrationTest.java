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
 * Wiring-level integration: the fully-composed pipeline (assembly -> variants -> classpath
 * accessors -> FreeMarker) generates a year's pages from the bundled classpath data (the real
 * 301-city dataset with LK/MY authority tables). This is where the whole stack is wired
 * together, so the integration test lives here rather than in any single service module.
 */
class SiteGenerationIntegrationTest {

    @Test
    void wiredPipelineGeneratesEveryCityAndBakesAuthorityTables(@TempDir Path out) throws Exception {
        SiteGenerationResult result = SiteGenerationWiring.siteGenerationManager(out, 2026).generate();

        assertEquals(302, result.pagesWritten(), "one page per bundled city");
        assertTrue(result.pagesWithTimetable() > 0, "LK/MY/CY cities get a baked authority table");

        // Kuala Lumpur (MY) snaps to a JAKIM zone -> its table is baked into the page.
        String kl = Files.readString(out.resolve("malaysia/kuala-lumpur.html"));
        assertTrue(kl.contains("authority-timetable"), "MY city should carry a baked table");
        assertTrue(kl.contains("annual timetable 2026"), "the baked year should be labelled");
        assertTrue(kl.contains("city_authority=JAKIM (e-Solat)"), "per-country front-matter is emitted");

        // Nicosia (CY) snaps to the KKTC Lefkoşa zone -> its scraped HTML-takvim table is baked.
        String nicosia = Files.readString(out.resolve("cyprus/nicosia.html"));
        assertTrue(nicosia.contains("authority-timetable"), "CY city should carry its scraped baked table");

        // Jakarta (ID) has no extracted authority table -> no baked table section.
        String jakarta = Files.readString(out.resolve("indonesia/jakarta.html"));
        assertFalse(jakarta.contains("authority-timetable"), "ID has no extracted table");
    }
}
