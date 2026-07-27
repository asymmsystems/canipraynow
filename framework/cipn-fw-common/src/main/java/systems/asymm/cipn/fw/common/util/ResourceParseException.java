/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.common.util;

/**
 * Thrown when a {@link ResourceParser} cannot parse its input.
 */
public class ResourceParseException extends RuntimeException {

    public ResourceParseException(String message) {
        super(message);
    }

    public ResourceParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
