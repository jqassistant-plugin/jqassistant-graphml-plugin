package org.jqassistant.plugin.graphml.report.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.PropertyContainer;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.shared.reflection.ClassHelper;

import org.jqassistant.plugin.graphml.report.api.GraphMLDecorator;

import static org.jqassistant.plugin.graphml.report.impl.MetaInformation.getLabelsString;

/**
 * @author mh
 * @since 21.01.14
 */
class XmlGraphMLWriter {

    private static final String GRAPHML_DECORATOR = "graphml.report.decorator";

    private static final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();

    private final ClassHelper classHelper;

    private final Class<? extends GraphMLDecorator> defaultDecoratorClass;

    private final Map<String, Object> properties;

    /**
     * Constructor.
     *
     * @param classHelper           The class helper instance.
     * @param defaultDecoratorClass The class for the default decorator.
     * @param properties            The properties of the GraphML plugin.
     */
    XmlGraphMLWriter(ClassHelper classHelper, Class<? extends GraphMLDecorator> defaultDecoratorClass, Map<String, Object> properties) {
        this.classHelper = classHelper;
        this.defaultDecoratorClass = defaultDecoratorClass;
        this.properties = properties;
    }

    void write(Result<?> result, SubGraph graph, File file) throws IOException, XMLStreamException {
        Map<Long, Node> nodes = getAllNodes(graph);
        Map<Long, Relationship> relationships = getAllRelationships(graph);
        try (PrintWriter writer = new PrintWriter(new FileWriter(file));
             GraphMLDecorator decorator = getGraphMLDecorator(result)) {
            XMLStreamWriter xmlWriter = xmlOutputFactory.createXMLStreamWriter(writer);
            decorator.initialize(result, graph, xmlWriter, file, properties);
            GraphMLNamespaceContext context = new GraphMLNamespaceContext(decorator.getNamespaces(), decorator.getSchemaLocations());
            xmlWriter.setNamespaceContext(context);
            writeHeader(xmlWriter, context);
            writeKeyTypes(xmlWriter, nodes.values(), relationships.values());
            decorator.writeKeys();

            writeSubgraph(graph, xmlWriter, decorator);

            for (Relationship relationship : relationships.values()) {
                long startId = relationship.getStartNode().getId();
                long endId = relationship.getEndNode().getId();
                if (nodes.containsKey(startId) && nodes.containsKey(endId)) {
                    writeRelationship(xmlWriter, decorator, relationship);
                }
            }

            writeFooter(xmlWriter);
        }
    }

    /**
     * Creates an instance of the select {@link GraphMLDecorator}.
     *
     * @param result The rule result.
     * @return The {@link GraphMLDecorator}.
     */
    private GraphMLDecorator getGraphMLDecorator(Result<?> result) {
        String graphMLDecorator = result.getRule().getReport().getProperties().getProperty(GRAPHML_DECORATOR);
        Class<? extends GraphMLDecorator> decoratorClass;
        if (graphMLDecorator != null) {
            decoratorClass = classHelper.getType(graphMLDecorator);
        } else {
            decoratorClass = defaultDecoratorClass;
        }
        return classHelper.createInstance(decoratorClass);
    }

    private void writeSubgraph(SubGraph graph, XMLStreamWriter writer, GraphMLDecorator decorator) throws XMLStreamException {
        Node parent = graph.getParent();
        if (parent != null) {
            writeNode(writer, decorator, parent, false);
        }

        writer.writeStartElement("graph");
        writer.writeAttribute("id", "G" + graph.hashCode());
        writer.writeAttribute("edgedefault", "directed");
        newLine(writer);

        for (Node node : graph.getNodes().values()) {
            writeNode(writer, decorator, node, true);
        }

        for (SubGraph subgraph : graph.getSubGraphs().values()) {
            writeSubgraph(subgraph, writer, decorator);
        }

        endElement(writer);

        if (parent != null) {
            writer.writeEndElement();
        }
    }

    private void writeKeyTypes(XMLStreamWriter writer, Collection<Node> allNodes, Collection<Relationship> allRelationships) throws XMLStreamException {
        Map<String, Class<?>> keyTypes = new HashMap<>();
        keyTypes.put("labels", String.class);
        for (Node node : allNodes) {
            updateKeyTypes(keyTypes, node);
        }
        writeKeyTypes(writer, keyTypes, "node");
        keyTypes.clear();
        for (Relationship rel : allRelationships) {
            updateKeyTypes(keyTypes, rel);
        }
        writeKeyTypes(writer, keyTypes, "edge");
    }

    private void writeKeyTypes(XMLStreamWriter writer, Map<String, Class<?>> keyTypes, String forType) throws XMLStreamException {
        for (Map.Entry<String, Class<?>> entry : keyTypes.entrySet()) {
            String type = MetaInformation.typeFor(entry.getValue(), MetaInformation.GRAPHML_ALLOWED);

            if (type == null) {
                continue;
            }

            writer.writeEmptyElement("key");
            writer.writeAttribute("id", entry.getKey());
            writer.writeAttribute("for", forType);
            writer.writeAttribute("attr.name", entry.getKey());
            writer.writeAttribute("attr.type", type);
            newLine(writer);
        }
    }

    private void updateKeyTypes(Map<String, Class<?>> keyTypes, PropertyContainer pc) {
        for (Map.Entry<String, Object> entry : pc.getProperties().entrySet()) {
            String prop = entry.getKey();
            Object value = entry.getValue();
            Class<?> storedClass = keyTypes.computeIfAbsent(prop, p -> value.getClass());
            if (!(storedClass == void.class || storedClass.equals(value.getClass()))) {
                keyTypes.put(prop, void.class);
            }
        }
    }

    private void writeNode(XMLStreamWriter writer, GraphMLDecorator decorator, Node node, boolean withEnd) throws XMLStreamException {
        if (decorator.isWriteNode(node)) {
            writer.writeStartElement("node");
            writer.writeAttribute("id", id(node));
            decorator.writeNodeAttributes(node);
            writeLabels(writer, node);
            writeLabelsAsData(writer, node);
            decorator.writeNodeElements(node);
            writeProps(writer, node);

            if (withEnd) {
                endElement(writer);
            }
        }
    }

    private String id(Node node) {
        return "n" + node.getId();
    }

    private void writeLabels(XMLStreamWriter writer, Node node) throws XMLStreamException {
        String labelsString = getLabelsString(node);
        if (!labelsString.isEmpty()) {
            writer.writeAttribute("labels", labelsString);
        }
    }

    private void writeLabelsAsData(XMLStreamWriter writer, Node node) throws XMLStreamException {
        String labelsString = getLabelsString(node);

        if (labelsString.isEmpty()) {
            return;
        }

        writeData(writer, "labels", labelsString);
    }

    private void writeRelationship(XMLStreamWriter writer, GraphMLDecorator decorator, Relationship relationship) throws XMLStreamException {
        if (decorator.isWriteRelationship(relationship)) {
            writer.writeStartElement("edge");
            writer.writeAttribute("id", id(relationship));
            writer.writeAttribute("source", id(relationship.getStartNode()));
            writer.writeAttribute("target", id(relationship.getEndNode()));
            writer.writeAttribute("label", relationship.getType());
            decorator.writeRelationshipAttributes(relationship);
            writeData(writer, "label", relationship.getType());
            decorator.writeRelationshipElements(relationship);
            writeProps(writer, relationship);
            endElement(writer);
        }
    }

    private String id(Relationship rel) {
        return "e" + rel.getId();
    }

    private void endElement(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeEndElement();
        newLine(writer);
    }

    private void writeProps(XMLStreamWriter writer, PropertyContainer node) throws XMLStreamException {
        for (Map.Entry<String, Object> entry : node.getProperties().entrySet()) {
            writeData(writer, entry.getKey(), entry.getValue());
        }
    }

    private void writeData(XMLStreamWriter writer, String prop, Object value) throws XMLStreamException {
        writer.writeStartElement("data");
        writer.writeAttribute("key", prop);

        if (value != null) {
            writer.writeCharacters(value.toString());
        }

        writer.writeEndElement();
    }

    private void writeFooter(XMLStreamWriter writer) throws XMLStreamException {
        endElement(writer);
        writer.writeEndDocument();
    }

    private void writeHeader(XMLStreamWriter writer, GraphMLNamespaceContext context) throws XMLStreamException {
        writer.writeStartDocument("UTF-8", "1.0");
        newLine(writer);
        writer.writeStartElement("graphml");
        writer.writeNamespace("xmlns", "http://graphml.graphdrawing.org/xmlns");
        for (Map.Entry<String, String> entry : context.getNamespaces().entrySet()) {
            writer.writeAttribute("xmlns", "http://graphml.graphdrawing.org/xmlns", entry.getKey(), entry.getValue());
        }
        if (!context.getSchemaLocations().isEmpty()) {
            StringBuilder schemaLocations = new StringBuilder();
            for (Map.Entry<String, String> entry : context.getSchemaLocations().entrySet()) {
                schemaLocations.append(entry.getKey()).append(" ").append(entry.getValue());
            }
            writer.writeAttribute("xsi", "", "schemaLocation", schemaLocations.toString());
        }
        newLine(writer);
    }

    private void newLine(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeCharacters("\n");
    }

    private Map<Long, Node> getAllNodes(SubGraph graph) {
        Map<Long, Node> allNodes = new LinkedHashMap<>(graph.getNodes());
        Node parentNode = graph.getParent();
        if (parentNode != null) {
            allNodes.put(parentNode.getId(), parentNode);
        }
        for (SubGraph subgraph : graph.getSubGraphs().values()) {
            allNodes.putAll(getAllNodes(subgraph));
        }
        return allNodes;
    }

    private Map<Long, Relationship> getAllRelationships(SubGraph graph) {
        Map<Long, Relationship> allRels = new LinkedHashMap<>(graph.getRelationships());
        for (SubGraph subgraph : graph.getSubGraphs().values()) {
            allRels.putAll(getAllRelationships(subgraph));
        }
        return allRels;
    }

}
