/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.engine;

/**
 * Supplies the {@link ZoneResolver} to use for a given country - the seam where the
 * per-country resolution strategy (polygon vs centroid) and its data are selected. Wired
 * externally, so orchestrators (managers) never construct a resolver themselves.
 */
@FunctionalInterface
public interface ZoneResolverProvider {

    /**
     * The resolver for a country's authority zones.
     *
     * @param countryCode ISO-3166 alpha-2 code
     * @return the zone resolver for that country
     */
    ZoneResolver resolverFor(String countryCode);
}
