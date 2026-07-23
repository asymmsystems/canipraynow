/*
 * Copyright (c) 2026 Asymm Systems (Pvt) Ltd. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.fw.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * A {@link ResourceParser} that deserialises XML into a structured Java type,
 * preserving hierarchy. XML is the house configuration format (per M360 ADR-049):
 * a context-free grammar with no implicit type coercion, truncation-safe (an unclosed
 * tag is always a parse error), and XSD-validatable. The XML library (Jackson XML) is
 * an internal detail not exposed to consumers.
 *
 * <p>Defaults: kebab-case property naming, no wrapper elements for repeated items,
 * namespace-unaware (files may declare a namespace for XSD validation, but element
 * names match Java fields unqualified).
 *
 * @param <T> the target type to deserialise into
 * @see ResourceParser
 */
public class StructuredXmlParser<T> implements ResourceParser<T> {

    /** XML infrastructure attributes (namespace/schema), never domain fields. */
    private static final Set<String> XML_INFRA_ATTRIBUTES = Set.of(
            "xmlns", "xmlns:xsi",
            "schemaLocation", "xsi:schemaLocation",
            "noNamespaceSchemaLocation", "xsi:noNamespaceSchemaLocation",
            "xsi:type", "xsi:nil");

    private final Class<T> targetType;
    private final XmlMapper xmlMapper;

    public StructuredXmlParser(Class<T> targetType) {
        if (targetType == null) {
            throw new IllegalArgumentException("Target type must not be null");
        }
        this.targetType = targetType;

        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        inputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);

        this.xmlMapper = XmlMapper.builder(XmlFactory.builder()
                        .xmlInputFactory(inputFactory)
                        .build())
                .propertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE)
                .defaultUseWrapper(false)
                .addHandler(new XsiAttributeHandler())
                .addModule(new ParameterNamesModule())
                .build();
    }

    @Override
    public T parse(InputStream inputStream) {
        if (inputStream == null) {
            throw new ResourceParseException("Input stream cannot be null");
        }
        try {
            return xmlMapper.readValue(inputStream, targetType);
        } catch (Exception e) {
            throw new ResourceParseException("Failed to parse XML into " + targetType.getSimpleName(), e);
        }
    }

    /** Silently ignores XML namespace/schema attributes that are not domain fields. */
    private static final class XsiAttributeHandler extends DeserializationProblemHandler {
        @Override
        public boolean handleUnknownProperty(DeserializationContext ctxt,
                com.fasterxml.jackson.core.JsonParser p, JsonDeserializer<?> deserializer,
                Object beanOrClass, String propertyName) throws IOException {
            if (XML_INFRA_ATTRIBUTES.contains(propertyName)) {
                p.skipChildren();
                return true;
            }
            return false;
        }
    }
}
