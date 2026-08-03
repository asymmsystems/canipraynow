/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A neutral, immutable XML element: a tag name, its attributes, and its child elements.
 * The output of {@link MinimalXmlParser} - a reflection-free element tree that a caller maps
 * to its own value types by hand, rather than binding via reflection.
 *
 * <p>This exists for the WebAssembly path (ADR-0015 §4): the Jackson-based
 * {@link StructuredXmlParser} does not compile to WASM (it needs StAX and
 * {@code java.util.concurrent.locks}), so the browser fallback reads XML through this
 * dependency-free model instead. Text content is not modelled - the data files this serves
 * are attribute-only.
 */
public final class XmlElement {

    private final String name;
    private final Map<String, String> attributes;
    private final List<XmlElement> children;

    /**
     * @param name       the tag name
     * @param attributes the attribute name-to-value map (insertion-ordered)
     * @param children   the child elements, in document order
     */
    public XmlElement(String name, Map<String, String> attributes, List<XmlElement> children) {
        this.name = name;
        this.attributes = Map.copyOf(attributes);
        this.children = List.copyOf(children);
    }

    /** @return the tag name */
    public String name() {
        return name;
    }

    /**
     * @param attributeName the attribute name
     * @return the attribute value, or {@code null} if the attribute is absent
     */
    public String attribute(String attributeName) {
        return attributes.get(attributeName);
    }

    /**
     * @param attributeName the attribute name
     * @param defaultValue  the value to return when the attribute is absent
     * @return the attribute value, or {@code defaultValue} if absent
     */
    public String attribute(String attributeName, String defaultValue) {
        String value = attributes.get(attributeName);
        return value == null ? defaultValue : value;
    }

    /** @return the child elements in document order */
    public List<XmlElement> children() {
        return children;
    }

    /**
     * @param childName the tag name to match
     * @return the child elements whose tag name equals {@code childName}
     */
    public List<XmlElement> childrenNamed(String childName) {
        List<XmlElement> matches = new ArrayList<>();
        for (XmlElement child : children) {
            if (child.name.equals(childName)) {
                matches.add(child);
            }
        }
        return matches;
    }
}
