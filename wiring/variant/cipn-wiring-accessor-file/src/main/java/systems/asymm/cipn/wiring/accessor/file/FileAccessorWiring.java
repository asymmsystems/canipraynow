/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.accessor.file;

import java.nio.file.Path;

import systems.asymm.cipn.fw.contract.access.CalculationMethodAccessor;
import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.access.ContentAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.svc.accessor.file.ClasspathCalculationMethodAccessor;
import systems.asymm.cipn.svc.accessor.file.FileCityAccessor;
import systems.asymm.cipn.svc.accessor.file.FileContentAccessor;
import systems.asymm.cipn.svc.accessor.file.FileTimetableAccessor;

/**
 * Plain-Java wiring for the file-location accessors: the sole place that instantiates them,
 * keeping {@code cipn-svc-accessor-file} out of the app.
 */
public final class FileAccessorWiring {

    private FileAccessorWiring() {
    }

    /** Timetable accessor rooted at the timetables directory. */
    public static TimetableAccessor timetableAccessor(Path timetablesRoot) {
        return new FileTimetableAccessor(timetablesRoot);
    }

    /** City accessor for the given {@code cities.xml}. */
    public static CityAccessor cityAccessor(Path citiesFile) {
        return new FileCityAccessor(citiesFile);
    }

    /** Content accessor writing under the given content root. */
    public static ContentAccessor contentAccessor(Path contentRoot) {
        return new FileContentAccessor(contentRoot);
    }

    /** Calculation-method accessor (E2 config) from the bundled classpath table. */
    public static CalculationMethodAccessor calculationMethodAccessor() {
        return new ClasspathCalculationMethodAccessor();
    }
}
