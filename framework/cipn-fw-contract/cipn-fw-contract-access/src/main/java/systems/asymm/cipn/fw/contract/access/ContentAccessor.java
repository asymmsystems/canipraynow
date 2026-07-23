/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.contract.access;

/**
 * Accessor contract for writing generated site content (write side of the build).
 *
 * <p>Named by CONSUMED TYPE (generated content). {@code FileContentAccessor} writes into
 * the JBake content tree today. Only the generator's own output is ever touched - callers
 * mark generated files and {@link #sweep(String)} removes stale ones from a prior run.
 *
 * @see Accessor
 */
public interface ContentAccessor extends Accessor {

    /**
     * Write a generated file at a path relative to the content root, creating parent
     * directories as needed.
     *
     * @param relativePath path relative to the content root (e.g. {@code "sri-lanka/colombo.html"})
     * @param content      the file content
     */
    void write(String relativePath, String content);

    /**
     * Delete previously generated files (those containing the given marker), so a page
     * dropped from the dataset disappears on the next run.
     *
     * @param marker the marker string every generated file carries
     * @return the number of files removed
     */
    int sweep(String marker);
}
