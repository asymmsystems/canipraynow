/*
 * Copyright (c) 2026 Asymm Systems. All rights reserved.
 *
 * @author Fahim Farook
 */
package systems.asymm.cipn.svc.manager;

import java.io.StringWriter;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;

/**
 * Renders a standalone FreeMarker template (loaded from {@code templates/} on the
 * classpath) against a data model into a string. Build-time only, deliberately living in
 * the manager module rather than fw-common/engine so the template engine stays off the
 * WASM/TeaVM dependency path.
 */
final class TemplateRenderer {

    private final Configuration configuration;

    TemplateRenderer() {
        this.configuration = new Configuration(Configuration.VERSION_2_3_33);
        configuration.setClassLoaderForTemplateLoading(TemplateRenderer.class.getClassLoader(), "templates");
        configuration.setDefaultEncoding("UTF-8");
        configuration.setBooleanFormat("c");
        configuration.setNumberFormat("computer");
        configuration.setWrapUncheckedExceptions(true);
    }

    String render(String templateName, Map<String, Object> model) {
        try {
            Template template = configuration.getTemplate(templateName);
            StringWriter out = new StringWriter();
            template.process(model, out);
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed rendering template " + templateName, e);
        }
    }
}
