/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.manager;

/**
 * Marker interface for Manager services.
 *
 * <p>Per IDesign/Volatility-Based Decomposition, Managers:
 * <ul>
 *   <li>Orchestrate use cases (workflow coordination)</li>
 *   <li>Contain ZERO business logic (delegate to Engines)</li>
 *   <li>Own the unit-of-work boundary of a use case</li>
 *   <li>Have HIGH volatility (use cases change frequently)</li>
 * </ul>
 *
 * <p>Each use case has its own manager interface extending this marker, e.g.
 * {@code interface SiteGenerationManager extends Manager}.
 *
 * @see SiteGenerationManager
 */
public interface Manager {
    // Marker only - defines the Manager service type.
}
