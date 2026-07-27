/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.engine;

/**
 * Marker interface for Engine services.
 *
 * <p>Per IDesign/Volatility-Based Decomposition, Engines:
 * <ul>
 *   <li>Contain ALL business logic and rules</li>
 *   <li>Have ZERO I/O (no database, network, file system)</li>
 *   <li>Have ZERO dependencies on other Engines</li>
 *   <li>Are purely functional (input to output)</li>
 * </ul>
 *
 * <p>Each domain area has its own engine interface extending this marker, e.g.
 * {@code interface AstronomyEngine extends Engine}. The marker enables compile-time
 * engine identification, ArchUnit constraints, and interface-based wiring.
 *
 * @see AstronomyEngine
 */
public interface Engine {
    // Marker only - defines the Engine service type.
}
