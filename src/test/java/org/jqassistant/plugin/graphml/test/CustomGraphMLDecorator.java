package org.jqassistant.plugin.graphml.test;

import java.io.File;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
import com.buschmais.jqassistant.core.report.api.model.Result;
import org.jqassistant.plugin.graphml.report.api.GraphMLDecorator;
import org.jqassistant.plugin.graphml.report.decorator.YedGraphMLDecorator;

public class CustomGraphMLDecorator implements GraphMLDecorator {

    private GraphMLDecorator delegate = new YedGraphMLDecorator();

    @Override
    public void initialize(Result<?> result, SubGraph subGraph, XMLStreamWriter xmlWriter, File file, Map<String, Object> properties) {
        delegate.initialize(result, subGraph, xmlWriter, file, properties);
    }

    @Override
    public Map<String, String> getNamespaces() {
        return delegate.getNamespaces();
    }

    @Override
    public Map<String, String> getSchemaLocations() {
        return delegate.getSchemaLocations();
    }

    @Override
    public void writeKeys() throws XMLStreamException {
        delegate.writeKeys();
    }

    @Override
    public boolean isWriteNode(Node node) {
        return true;
    }

    @Override
    public void writeNodeAttributes(Node node) throws XMLStreamException {
        delegate.writeNodeAttributes(node);
    }

    @Override
    public void writeNodeElements(Node node) throws XMLStreamException {
        delegate.writeNodeElements(node);
    }

    @Override
    public boolean isWriteRelationship(Relationship relationship) {
        return true;
    }

    @Override
    public void writeRelationshipAttributes(Relationship relationship) throws XMLStreamException {
        delegate.writeRelationshipAttributes(relationship);
    }

    @Override
    public void writeRelationshipElements(Relationship relationship) throws XMLStreamException {
        delegate.writeRelationshipElements(relationship);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
