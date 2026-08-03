/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny, dependency-free {@link ResourceParser} that reads the project's XML data subset into
 * a neutral {@link XmlElement} tree. It handles the XML declaration, comments, the DOCTYPE,
 * elements with double- or single-quoted attributes, self-closing tags, nested children, and
 * the five predefined entities; it deliberately ignores text content (the data files are
 * attribute-only) and namespaces (the files are read namespace-unaware, per house style).
 *
 * <p>Why this exists next to {@link StructuredXmlParser}: the Jackson/StAX parser is the rich
 * default on the JVM, but does not compile to WebAssembly (TeaVM has no
 * {@code javax.xml.stream} and no {@code java.util.concurrent.locks}). The browser fallback
 * engine (ADR-0015 §4) needs an XML reader that compiles to WASM-GC, so this one uses only
 * plain string scanning, {@code ArrayList}/{@code LinkedHashMap} and {@code StringBuilder} -
 * no reflection, no regex, no service loaders. It is a legitimate second impl of the
 * {@code ResourceParser} concept, not a bespoke one-off: callers map the tree to their own
 * types (see the WASM assembly's method-table reader).
 */
public class MinimalXmlParser implements ResourceParser<XmlElement> {

    @Override
    public XmlElement parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new ResourceParseException("Input stream cannot be null");
        }
        String xml = readAll(inputStream);
        int[] pos = {0};
        skipProlog(xml, pos);
        XmlElement root = parseElement(xml, pos);
        if (root == null) {
            throw new ResourceParseException("No root element found");
        }
        return root;
    }

    private static String readAll(InputStream in) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResourceParseException("Failed reading XML stream", e);
        }
    }

    /** Skip whitespace, the {@code <?xml?>} declaration, comments and any {@code <!...>}. */
    private static void skipProlog(String s, int[] pos) {
        while (pos[0] < s.length()) {
            skipWhitespace(s, pos);
            if (looksLike(s, pos[0], "<?")) {
                pos[0] = requireIndexOf(s, "?>", pos[0]) + 2;
            } else if (looksLike(s, pos[0], "<!--")) {
                pos[0] = requireIndexOf(s, "-->", pos[0]) + 3;
            } else if (looksLike(s, pos[0], "<!")) {
                pos[0] = requireIndexOf(s, ">", pos[0]) + 1;
            } else {
                return;
            }
        }
    }

    private static XmlElement parseElement(String s, int[] pos) {
        skipProlog(s, pos);
        if (pos[0] >= s.length() || s.charAt(pos[0]) != '<') {
            return null;
        }
        pos[0]++;
        String name = readName(s, pos);
        Map<String, String> attributes = new LinkedHashMap<>();
        while (true) {
            skipWhitespace(s, pos);
            char c = charAt(s, pos[0]);
            if (c == '/' || c == '>') {
                break;
            }
            String attrName = readName(s, pos);
            skipWhitespace(s, pos);
            expect(s, pos, '=');
            skipWhitespace(s, pos);
            attributes.put(attrName, readQuoted(s, pos));
        }

        List<XmlElement> children = new ArrayList<>();
        if (charAt(s, pos[0]) == '/') {
            pos[0]++;
            expect(s, pos, '>');
        } else {
            expect(s, pos, '>');
            while (true) {
                skipProlog(s, pos);
                if (looksLike(s, pos[0], "</")) {
                    pos[0] += 2;
                    readName(s, pos);
                    skipWhitespace(s, pos);
                    expect(s, pos, '>');
                    break;
                }
                if (pos[0] < s.length() && s.charAt(pos[0]) == '<') {
                    XmlElement child = parseElement(s, pos);
                    if (child != null) {
                        children.add(child);
                    }
                } else {
                    // Text content: not modelled; advance to the next tag.
                    int next = s.indexOf('<', pos[0]);
                    if (next < 0) {
                        throw new ResourceParseException("Unterminated element: " + name);
                    }
                    pos[0] = next;
                }
            }
        }
        return new XmlElement(name, attributes, children);
    }

    private static String readName(String s, int[] pos) {
        int start = pos[0];
        while (pos[0] < s.length()) {
            char c = s.charAt(pos[0]);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '=' || c == '>' || c == '/') {
                break;
            }
            pos[0]++;
        }
        if (pos[0] == start) {
            throw new ResourceParseException("Expected a name at position " + start);
        }
        return s.substring(start, pos[0]);
    }

    private static String readQuoted(String s, int[] pos) {
        char quote = charAt(s, pos[0]);
        if (quote != '"' && quote != '\'') {
            throw new ResourceParseException("Expected a quoted value at position " + pos[0]);
        }
        pos[0]++;
        int start = pos[0];
        int end = s.indexOf(quote, start);
        if (end < 0) {
            throw new ResourceParseException("Unterminated attribute value at position " + start);
        }
        pos[0] = end + 1;
        return decodeEntities(s.substring(start, end));
    }

    private static String decodeEntities(String value) {
        if (value.indexOf('&') < 0) {
            return value;
        }
        return value
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    private static void skipWhitespace(String s, int[] pos) {
        while (pos[0] < s.length()) {
            char c = s.charAt(pos[0]);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                return;
            }
            pos[0]++;
        }
    }

    private static boolean looksLike(String s, int at, String token) {
        return s.startsWith(token, at);
    }

    private static int requireIndexOf(String s, String token, int from) {
        int found = s.indexOf(token, from);
        if (found < 0) {
            throw new ResourceParseException("Expected '" + token + "' after position " + from);
        }
        return found;
    }

    private static char charAt(String s, int pos) {
        if (pos >= s.length()) {
            throw new ResourceParseException("Unexpected end of XML");
        }
        return s.charAt(pos);
    }

    private static void expect(String s, int[] pos, char expected) {
        if (charAt(s, pos[0]) != expected) {
            throw new ResourceParseException("Expected '" + expected + "' at position " + pos[0]);
        }
        pos[0]++;
    }
}
