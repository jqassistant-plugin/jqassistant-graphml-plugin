package org.jqassistant.plugin.graphml.report.api;

import java.io.Closeable;
import java.io.File;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
import com.buschmais.jqassistant.core.report.api.model.Result;

/**
 * Defines the interface for a GraphML decorator.
 *
 * Provides methods that are called if elements for nodes or relationships are written which may
 * be implemented for adding attributes or XML elements.
 */
public interface GraphMLDecorator extends Closeable {

    /**
     * Initialize the decorator.
     *
     * @param result     The current result that is written as a graph.
     * @param subGraph   The sub graph to render.
     * @param xmlWriter  The {@link XMLStreamWriter} that is used for writing.
     * @param file       The output file.
     * @param properties The properties from the GraphML plugin configuration.
     */
    void initialize(Result<?> result, SubGraph subGraph, XMLStreamWriter xmlWriter, File file, Map<String, Object> properties);

    /**
     * Return the additional namespaces identified by their prefixes which are used by the decorator.
     *
     * @return The additional namespaces identified by their prefixes.
     */
    Map<String, String> getNamespaces();

    /**
     * Return the schema locations identified by their namespaces.
     *
     * @return The schema locations.
     */
    Map<String, String> getSchemaLocations();

    /**
     * Writes a bunch of keys in the graphml-Tag that will be used for formating or so. This method
     * can be overwritten if any special default keys are necessary. Please call super to ensure all
     * needed keys will be created.
     *
     * @throws XMLStreamException If writing fails.
     */
    void writeKeys() throws XMLStreamException;

    /**
     * Determine if a node shall be written.
     *
     * @param node The node.
     * @return <code>true</code> if the node shall be written.
     */
    boolean isWriteNode(Node node);

    /**
     * Add node attributes.
     *
     * @param node the node
     * @throws XMLStreamException
     */
    void writeNodeAttributes(Node node) throws XMLStreamException;

    /**
     * Add elements inside a node-element.
     *
     * @param node the node
     * @throws XMLStreamException
     */
    void writeNodeElements(Node node) throws XMLStreamException;

    /**
     * Determine if a relationship shall be written.
     *
     * @param relationship The relationship.
     * @return <code>true</code> if the relationship shall be written.
     */
    boolean isWriteRelationship(Relationship relationship);

    /**
     * Add relationship attributes.
     *
     * @param relationship the relationship
     * @throws XMLStreamException
     */
    void writeRelationshipAttributes(Relationship relationship) throws XMLStreamException;

    /**
     * Add elements inside a relationship-element.
     *
     * @param relationship the relationship
     * @throws XMLStreamException
     */
    void writeRelationshipElements(Relationship relationship) throws XMLStreamException;

    /**
     * Finish writing the GraphML document.
     */
    void close();

}
