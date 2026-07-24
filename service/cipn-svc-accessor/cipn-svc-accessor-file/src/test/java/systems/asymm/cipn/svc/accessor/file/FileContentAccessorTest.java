/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.accessor.file;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import systems.asymm.cipn.fw.contract.access.ContentAccessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Writing generated pages and sweeping only marked (generated) files.
 */
class FileContentAccessorTest {

    private static final String MARKER = "generated_by=SiteGenerator";

    @Test
    void writesPageCreatingParentDirs(@TempDir Path root) throws Exception {
        ContentAccessor content = new FileContentAccessor(root);
        content.write("sri-lanka/colombo.html", "hello " + MARKER);
        Path written = root.resolve("sri-lanka/colombo.html");
        assertTrue(Files.isRegularFile(written));
        assertTrue(Files.readString(written).contains(MARKER));
    }

    @Test
    void sweepRemovesOnlyMarkedFiles(@TempDir Path root) throws Exception {
        ContentAccessor content = new FileContentAccessor(root);
        content.write("a/generated.html", "x " + MARKER);
        content.write("b/handwritten.html", "not generated");

        assertEquals(1, content.sweep(MARKER));
        assertFalse(Files.exists(root.resolve("a/generated.html")));
        assertTrue(Files.exists(root.resolve("b/handwritten.html")));
    }
}
