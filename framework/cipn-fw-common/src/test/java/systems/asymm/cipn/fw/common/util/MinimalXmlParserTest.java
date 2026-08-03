/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Validates the dependency-free {@link MinimalXmlParser} on the JVM: the same code TeaVM
 * compiles to WASM-GC, so passing here is the correctness gate for the browser path (the
 * sandbox cannot run WASM). Exercises the declaration, comments, self-closing tags, nested
 * children, both quote styles and entity decoding.
 */
class MinimalXmlParserTest {

    private static XmlElement parse(String xml) {
        return new MinimalXmlParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void readsRootNameAndAttributesSkippingDeclarationAndComments() {
        XmlElement root = parse("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!-- a comment before the root -->
                <calculation-methods default-method="muslim-world-league">
                    <method key="acju" fajr-angle="20" isha-angle="18"/>
                </calculation-methods>
                """);
        assertEquals("calculation-methods", root.name());
        assertEquals("muslim-world-league", root.attribute("default-method"));
        assertEquals(1, root.children().size());
    }

    @Test
    void readsSelfClosingChildrenAndTheirAttributes() {
        XmlElement root = parse("""
                <calculation-methods default-method="mwl">
                    <method key="acju" fajr-angle="20" isha-angle="18"/>
                    <!-- inline comment between children -->
                    <method key="singapore" fajr-angle="20" isha-angle="18" rounding="up" dhuhr-offset="1"/>
                </calculation-methods>
                """);
        List<XmlElement> methods = root.childrenNamed("method");
        assertEquals(2, methods.size());
        assertEquals("acju", methods.get(0).attribute("key"));
        assertEquals("20", methods.get(0).attribute("fajr-angle"));
        assertEquals("up", methods.get(1).attribute("rounding"));
        assertNull(methods.get(0).attribute("rounding"), "absent attribute is null");
    }

    @Test
    void handlesNestedChildrenAndSingleQuotes() {
        XmlElement root = parse("<root a='x'><child b=\"y\"><leaf c='z'/></child></root>");
        assertEquals("x", root.attribute("a"));
        XmlElement child = root.children().get(0);
        assertEquals("child", child.name());
        assertEquals("y", child.attribute("b"));
        assertEquals("leaf", child.children().get(0).name());
        assertEquals("z", child.children().get(0).attribute("c"));
    }

    @Test
    void decodesThePredefinedEntities() {
        XmlElement root = parse("<zone name=\"Sana&apos;a &amp; Aden &lt;area&gt;\"/>");
        assertEquals("Sana'a & Aden <area>", root.attribute("name"));
    }

    @Test
    void rejectsUnterminatedInput() {
        Throwable error = assertThrows(ResourceParseException.class,
                () -> parse("<calculation-methods><method key=\"acju\""));
        assertTrue(error.getMessage() != null && !error.getMessage().isBlank());
    }
}
