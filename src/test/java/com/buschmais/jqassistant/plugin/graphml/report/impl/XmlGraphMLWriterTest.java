package com.buschmais.jqassistant.plugin.graphml.report.impl;

import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.buschmais.jqassistant.core.analysis.api.Result;
import com.buschmais.jqassistant.core.analysis.api.rule.Concept;
import com.buschmais.jqassistant.core.analysis.api.rule.Report;
import com.buschmais.jqassistant.core.shared.reflection.ClassHelper;
import com.buschmais.jqassistant.plugin.graphml.report.api.GraphMLDecorator;
import com.buschmais.jqassistant.plugin.graphml.report.api.GraphMLNode;
import com.buschmais.jqassistant.plugin.graphml.report.api.GraphMLRelationship;
import com.buschmais.jqassistant.plugin.graphml.report.api.GraphMLSubGraph;
import com.buschmais.jqassistant.plugin.graphml.report.decorator.YedGraphMLDecorator;
import com.buschmais.jqassistant.plugin.graphml.test.CustomGraphMLDecorator;

@RunWith(MockitoJUnitRunner.class)
public class XmlGraphMLWriterTest {

    @Mock
    private ClassHelper classHelper;

    @Mock
    private Concept concept;

    @Mock
    private Result<?> result;

    @Mock
    private GraphMLNode nodeObject1;

    @Mock
    private GraphMLNode nodeObject2;

    @Mock
    private GraphMLRelationship relationshipObject1;

    @Mock
    private GraphMLRelationship relationshipObject2;

    @Before
    public void setUp() {
        when(result.getRule()).thenReturn(concept);

        GraphMLNode node1 = stubNode();
        when(nodeObject1.getId()).thenReturn(1L);
        GraphMLRelationship relationship1 = stubRelationship(node1);
        when(relationshipObject1.getId()).thenReturn(1L);
        GraphMLNode node2 = stubNode();
        when(nodeObject2.getId()).thenReturn(2L);
        when(relationshipObject2.getId()).thenReturn(2L);
        GraphMLRelationship relationship2 = stubRelationship(node2);
    }

    private GraphMLRelationship stubRelationship(GraphMLNode node) {
        GraphMLRelationship relationship = mock(GraphMLRelationship.class);
        when(relationship.getType()).thenReturn("TEST");
        when(relationship.getProperties()).thenReturn(Collections.<String, Object>emptyMap());
        when(relationship.getStartNode()).thenReturn(node);
        when(relationship.getEndNode()).thenReturn(node);
        return relationship;
    }

    private GraphMLNode stubNode() {
        GraphMLNode node = mock(GraphMLNode.class);
        when(node.getProperties()).thenReturn(Collections.<String, Object>emptyMap());
        when(node.getLabels()).thenReturn(Collections.<String>emptySet());
        return node;
    }

    @Test
    public void ruleSpecificDecorator() throws IOException, XMLStreamException {
        Report report = Report.Builder.newInstance().property("graphml.report.decorator", CustomGraphMLDecorator.class.getName()).get();
        stubDecorator(report, CustomGraphMLDecorator.class);
        File file = getFile();
        Map<String, Object> properties = new HashMap<>();
        XmlGraphMLWriter writer = new XmlGraphMLWriter(classHelper, YedGraphMLDecorator.class, properties);
        GraphMLSubGraph subGraph = getSubGraph();

        writer.write(result, subGraph, file);

        verify(classHelper).getType(CustomGraphMLDecorator.class.getName());
        verify(classHelper).createInstance(CustomGraphMLDecorator.class);
    }

    @Test
    public void defaultDecorator() throws IOException, XMLStreamException {
        Report report = Report.Builder.newInstance().get();
        stubDecorator(report, YedGraphMLDecorator.class);
        File file = getFile();
        Map<String, Object> properties = new HashMap<>();
        XmlGraphMLWriter writer = new XmlGraphMLWriter(classHelper, YedGraphMLDecorator.class, properties);
        GraphMLSubGraph subGraph = getSubGraph();

        writer.write(result, subGraph, file);

        verify(classHelper).createInstance(YedGraphMLDecorator.class);
    }

    @Test
    public void decoratorFilter() throws IOException, XMLStreamException {
        Report report = Report.Builder.newInstance().get();
        YedGraphMLDecorator decorator = stubDecorator(report, YedGraphMLDecorator.class);
        when(decorator.isWriteNode(nodeObject1)).thenReturn(true);
        when(decorator.isWriteRelationship(relationshipObject1)).thenReturn(true);
        when(decorator.isWriteNode(nodeObject2)).thenReturn(false);
        when(decorator.isWriteRelationship(relationshipObject2)).thenReturn(false);
        File file = getFile();
        Map<String, Object> properties = new HashMap<>();
        XmlGraphMLWriter writer = new XmlGraphMLWriter(classHelper, YedGraphMLDecorator.class, properties);
        GraphMLSubGraph subGraph = getSubGraph();

        writer.write(result, subGraph, file);

        verify(decorator).isWriteNode(nodeObject1);
        verify(decorator).writeNodeAttributes(nodeObject1);
        verify(decorator).writeNodeElements(nodeObject1);
        verify(decorator).isWriteRelationship(relationshipObject1);
        verify(decorator).writeRelationshipAttributes(relationshipObject1);
        verify(decorator).writeRelationshipElements(relationshipObject1);

        verify(decorator).isWriteNode(nodeObject2);
        verify(decorator, never()).writeNodeAttributes(nodeObject2);
        verify(decorator, never()).writeNodeElements(nodeObject2);
        verify(decorator).isWriteRelationship(relationshipObject2);
        verify(decorator, never()).writeRelationshipAttributes(relationshipObject2);
        verify(decorator, never()).writeRelationshipElements(relationshipObject2);
    }

    private <T extends GraphMLDecorator> T stubDecorator(Report report, Class<T> decoratorClass) {
        when(concept.getReport()).thenReturn(report);
        T decorator = mock(decoratorClass);
        doReturn(decoratorClass).when(classHelper).getType(decoratorClass.getName());
        doReturn(decorator).when(classHelper).createInstance(decoratorClass);
        return decorator;
    }

    private File getFile() throws IOException {
        File file = File.createTempFile("test", ".graphml");
        file.deleteOnExit();
        return file;
    }

    private GraphMLSubGraph getSubGraph() {
        GraphMLSubGraph subGraph = new GraphMLSubGraph();
        subGraph.getNodes().put(nodeObject1.getId(), nodeObject1);
        subGraph.getNodes().put(nodeObject2.getId(), nodeObject2);
        subGraph.getRelationships().put(relationshipObject1.getId(), relationshipObject1);
        subGraph.getRelationships().put(relationshipObject2.getId(), relationshipObject2);
        return subGraph;
    }
}
