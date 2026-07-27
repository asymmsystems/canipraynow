/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.accessor.file;

import java.nio.file.Path;

import systems.asymm.cipn.fw.contract.access.ContentAccessor;
import systems.asymm.cipn.svc.accessor.file.FileContentAccessor;

/**
 * Plain-Java wiring for the filesystem write accessor: the sole place that instantiates it,
 * keeping {@code cipn-svc-accessor-file} out of the app. The read accessors are classpath
 * (see {@code ClasspathAccessorWiring}); this variant owns only the output writer.
 */
public final class FileAccessorWiring {

    private FileAccessorWiring() {
    }

    /** Content accessor writing generated pages under the given output root. */
    public static ContentAccessor contentAccessor(Path outputRoot) {
        return new FileContentAccessor(outputRoot);
    }
}
