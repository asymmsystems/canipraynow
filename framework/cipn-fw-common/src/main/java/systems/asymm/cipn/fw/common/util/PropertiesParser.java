/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Parses a {@code .properties} stream into an immutable {@code Map<String, String>},
 * suitable for message bundles, configuration or reference tables. Returning a plain
 * map (rather than {@link Properties}, a synchronized {@code Hashtable} subclass)
 * gives accessors a uniform, immutable API independent of the source format.
 *
 * @see ResourceParser
 */
public class PropertiesParser implements ResourceParser<Map<String, String>> {

    @Override
    public Map<String, String> parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new ResourceParseException("Input stream cannot be null");
        }
        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new ResourceParseException("Failed to parse properties file", e);
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            result.put(key, properties.getProperty(key));
        }
        return Collections.unmodifiableMap(result);
    }
}
