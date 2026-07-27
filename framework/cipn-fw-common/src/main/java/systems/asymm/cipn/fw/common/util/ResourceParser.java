/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.common.util;

import java.io.InputStream;

/**
 * Parses a raw resource stream into structured data.
 *
 * <p>Per the 4-Concerns Framework, Type/Schema (Properties, JSON, ...) is a
 * technical concern handled by these stateless utilities, not exposed in accessor
 * contracts. An accessor owns the Location concern (which resource, opening it) and
 * delegates the parse to a {@code ResourceParser}.
 *
 * @param <T> the parsed result type
 */
@FunctionalInterface
public interface ResourceParser<T> {

    /**
     * Parses an input stream into a structured result.
     *
     * @param inputStream the stream to parse; the caller is responsible for closing it
     * @return the parsed result
     * @throws ResourceParseException if the stream is null or cannot be parsed
     */
    T parse(InputStream inputStream);
}
