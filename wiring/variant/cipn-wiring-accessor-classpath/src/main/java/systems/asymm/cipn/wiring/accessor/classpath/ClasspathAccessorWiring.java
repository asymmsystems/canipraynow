/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.wiring.accessor.classpath;

import systems.asymm.cipn.fw.contract.access.CalculationMethodAccessor;
import systems.asymm.cipn.fw.contract.access.CityAccessor;
import systems.asymm.cipn.fw.contract.access.TimetableAccessor;
import systems.asymm.cipn.svc.accessor.classpath.ClasspathCalculationMethodAccessor;
import systems.asymm.cipn.svc.accessor.classpath.ClasspathCityAccessor;
import systems.asymm.cipn.svc.accessor.classpath.ClasspathTimetableAccessor;

/**
 * Plain-Java wiring for the classpath-location read accessors: the sole place that
 * instantiates them, keeping {@code cipn-svc-accessor-classpath} out of the app. The
 * timetable accessor is bound to a year (ADR-0012), so the generator asks for one per year.
 */
public final class ClasspathAccessorWiring {

    private ClasspathAccessorWiring() {
    }

    /** City accessor over the bundled {@code city.xml}. */
    public static CityAccessor cityAccessor() {
        return new ClasspathCityAccessor();
    }

    /** Timetable accessor over the bundled {@code timetable/<year>/...} tree. */
    public static TimetableAccessor timetableAccessor(int year) {
        return new ClasspathTimetableAccessor(year);
    }

    /** Calculation-method accessor (E2 config) from the bundled registry. */
    public static CalculationMethodAccessor calculationMethodAccessor() {
        return new ClasspathCalculationMethodAccessor();
    }
}
