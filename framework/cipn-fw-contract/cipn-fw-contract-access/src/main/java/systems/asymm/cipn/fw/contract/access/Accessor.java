/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.access;

/**
 * Marker interface for Accessor services.
 *
 * <p>Per IDesign/Volatility-Based Decomposition, Accessors:
 * <ul>
 *   <li>Encapsulate resource access (files, classpath, databases, external services)</li>
 *   <li>Are the ONLY components with I/O</li>
 *   <li>Contain ZERO business logic</li>
 *   <li>Have LOW volatility of behaviour, but HIGH volatility of location</li>
 * </ul>
 *
 * <p>Contracts are named by the CONSUMED TYPE (e.g. {@code CalculationMethodAccessor});
 * implementations are named by LOCATION (e.g. {@code ClasspathCalculationMethodAccessor}).
 * Type/Schema (Properties, JSON, ...) is an internal detail of the implementation.
 *
 * @see CalculationMethodAccessor
 */
public interface Accessor {
    // Marker only - defines the Accessor service type.
}
