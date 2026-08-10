/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.common.util;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

import systems.asymm.cipn.fw.contract.data.AuthorityTimetable;
import systems.asymm.cipn.fw.contract.data.TimetableRow;

/**
 * Serialises an {@link AuthorityTimetable} to the standard {@code zone-<id>.xml} document and
 * validates it against the bundled {@code schema/timetable.xsd} - the write-side inverse of
 * {@link StructuredXmlParser}, used by the S1 scraper (ADR-0017) to emit classpath timetable
 * XML (ADR-0012). Output is minutes-from-midnight, one {@code <day>} per row, in calendar
 * order. Solar noon is an E3 enrichment: it is written only when present (&gt; 0), so freshly
 * scraped tables (which carry the authority's published times only) omit it.
 *
 * <p>Pure: it builds a string and validates it. No filesystem I/O - the caller writes the
 * returned string through a write accessor.
 */
public final class TimetableXml {

    private static final String SCHEMA_RESOURCE = "/schema/timetable.xsd";
    private static final String UNIT = "minutes-from-midnight";

    private TimetableXml() {
    }

    /**
     * Serialise a zone's annual timetable to a schema-valid {@code zone-<id>.xml} document.
     *
     * @param timetable the zone's annual table (authority identity, zone, source, rows)
     * @return the XML document text
     * @throws ResourceParseException if the produced document does not satisfy {@code timetable.xsd}
     */
    public static String serialize(AuthorityTimetable timetable) {
        StringBuilder sb = new StringBuilder(1 << 16);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!--\n");
        sb.append("  Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.\n");
        sb.append("  ").append(esc(timetable.authority())).append(" zone ").append(esc(timetable.zoneId()))
                .append(" annual timetable (S1 generated, ADR-0017).\n");
        sb.append("  @author Fahim Farook\n");
        sb.append("-->\n");
        sb.append("<timetable authority=\"").append(esc(timetable.authority()))
                .append("\" zone=\"").append(esc(timetable.zoneId()))
                .append("\" name=\"").append(esc(timetable.zoneName()))
                .append("\" source=\"").append(esc(timetable.source()))
                .append("\" unit=\"").append(UNIT).append("\"");
        if (timetable.note() != null && !timetable.note().isBlank()) {
            sb.append(" note=\"").append(esc(timetable.note())).append("\"");
        }
        sb.append(">\n");
        for (TimetableRow r : timetable.rows()) {
            sb.append("    <day month=\"").append(r.month())
                    .append("\" day=\"").append(r.day())
                    .append("\" fajr=\"").append(r.fajr())
                    .append("\" sunrise=\"").append(r.sunrise())
                    .append("\" dhuhr=\"").append(r.dhuhr())
                    .append("\" asr=\"").append(r.asr())
                    .append("\" maghrib=\"").append(r.maghrib())
                    .append("\" isha=\"").append(r.isha()).append("\"");
            if (r.solarNoon() > 0) {
                sb.append(" solar-noon=\"").append(r.solarNoon()).append("\"");
            }
            sb.append("/>\n");
        }
        sb.append("</timetable>\n");

        String xml = sb.toString();
        validate(xml);
        return xml;
    }

    /**
     * Validate a timetable document against the bundled {@code schema/timetable.xsd}.
     *
     * @param xml the document text
     * @throws ResourceParseException if it is not well-formed or not schema-valid
     */
    public static void validate(String xml) {
        URL schemaUrl = TimetableXml.class.getResource(SCHEMA_RESOURCE);
        if (schemaUrl == null) {
            throw new ResourceParseException("Missing bundled schema " + SCHEMA_RESOURCE);
        }
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = factory.newSchema(schemaUrl);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new StringReader(xml)));
        } catch (SAXException | IOException e) {
            throw new ResourceParseException("Timetable XML failed schema validation", e);
        }
    }

    /** Escape the five XML predefined entities for use inside a double-quoted attribute. */
    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
