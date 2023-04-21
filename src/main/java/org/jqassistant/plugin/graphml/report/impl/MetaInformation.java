package org.jqassistant.plugin.graphml.report.impl;

import java.util.HashSet;
import java.util.Set;

import com.buschmais.jqassistant.core.report.api.graph.model.Node;

import static java.util.Arrays.asList;

/**
 * @author mh
 * @since 19.01.14
 */
public class MetaInformation {

    public final static Set<String> GRAPHML_ALLOWED = new HashSet<>(asList("boolean", "int", "long", "float", "double", "string"));

    public static String typeFor(Class value, Set<String> allowed) {
        if (value == void.class) {
            return null;
        }

        if (value.isArray()) {
            return null; // TODO arrays
        }

        String name = value.getSimpleName().toLowerCase();

        if (name.equals("integer")) {
            name = "int";
        }

        if (allowed == null || allowed.contains(name)) {
            return name;
        }

        if (Number.class.isAssignableFrom(value)) {
            return "int";
        }

        return null;
    }

    public static String getLabelsString(Node node) {
        if (node.getLabels().isEmpty()) {
            return "";
        } else {
            StringBuilder labels = new StringBuilder();
            for (String label : node.getLabels()) {
                labels.append(':').append(label);
            }
            return labels.toString();
        }
    }

}
