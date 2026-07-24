/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.access;

import java.util.Optional;
import java.util.Set;

import systems.asymm.cipn.fw.contract.data.CalculationParameters;
import systems.asymm.cipn.fw.contract.data.Madhab;

/**
 * Accessor contract for the calculation-method registry (E2 configuration): the
 * per-authority Fajr/Isha angles, interval, Maghrib angle, rounding and offsets that
 * the astronomy engine computes with.
 *
 * <p>Named by CONSUMED TYPE (calculation-method parameters). The location and format
 * are implementation details: {@code ClasspathCalculationMethodAccessor} reads a
 * bundled {@code .properties} table today; a database or CMS could serve it tomorrow
 * without touching this contract. The madhab (which only sets the ʿAsr shadow factor)
 * is supplied per call rather than stored in the table.
 *
 * @see Accessor
 */
public interface CalculationMethodAccessor extends Accessor {

    /**
     * Look up a method's parameters.
     *
     * @param methodKey the method identifier (e.g. {@code "MuslimWorldLeague"}, {@code "ACJU"})
     * @param madhab    the school, applied as the ʿAsr shadow factor
     * @return the parameters, or empty if the key is not in the registry
     */
    Optional<CalculationParameters> parameters(String methodKey, Madhab madhab);

    /**
     * Look up a method's parameters, falling back to the registry default (Muslim World
     * League) when the key is unknown - mirroring the site's silent fallback.
     *
     * @param methodKey the method identifier
     * @param madhab    the school, applied as the ʿAsr shadow factor
     * @return the parameters for the key, or the default method's parameters
     */
    CalculationParameters parametersOrDefault(String methodKey, Madhab madhab);

    /**
     * The set of method keys the registry knows.
     *
     * @return the known method keys
     */
    Set<String> methodKeys();
}
