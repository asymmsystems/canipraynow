/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.data;

import java.util.List;

/**
 * A simple polygon given by its exterior ring in order (the ring need not repeat its
 * first point). A location is inside the polygon by the even-odd (ray-casting) rule.
 *
 * <p>Holes are not modelled yet; an administrative district with a hole is represented
 * as its outer boundary, which is sufficient for zone matching. A zone that spans several
 * disjoint areas is a list of these polygons (see {@link ZoneBoundary}).
 *
 * @param ring the exterior ring vertices, in order
 */
public record GeoPolygon(List<GeoPoint> ring) {
}
