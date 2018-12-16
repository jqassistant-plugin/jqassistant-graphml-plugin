package com.buschmais.jqassistant.plugin.graphml.report.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import com.buschmais.jqassistant.core.analysis.api.Result;
import com.buschmais.jqassistant.core.analysis.api.rule.Concept;
import com.buschmais.jqassistant.core.analysis.api.rule.Report;
import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
import com.buschmais.jqassistant.core.shared.reflection.ClassHelper;
import com.buschmais.jqassistant.plugin.graphml.report.api.GraphMLDecorator;
import com.buschmais.jqassistant.plugin.graphml.report.decorator.YedGraphMLDecorator;
import com.buschmais.jqassistant.plugin.graphml.test.CustomGraphMLDecorator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class XmlGraphMLWriterTest {

    @Mock
    private ClassHelper classHelper;

    @Mock
    private Concept concept;

    @Mock
    private Result<?> result;

    private Node node1;

    private Node node2;

    private Relationship relationship1;

    private Relationship relationship2;

    private SubGraph subGraph;

    @BeforeEach
    public void setUp() {
        when(result.getRule()).thenReturn(concept);

        node1 = new Node();
        node1.setId(1);
        relationship1 = new Relationship();
        relationship1.setId(1);
        relationship1.setType("TEST");
        relationship1.setStartNode(node1);
        relationship1.setEndNode(node1);

        node2 = new Node();
        node2.setId(2);
        relationship2 = new Relationship();
        relationship2.setId(2);
        relationship2.setType("TEST");
        relationship2.setStartNode(node2);
        relationship2.setEndNode(node2);

        subGraph = new SubGraph();
        subGraph.getNodes().put(node1.getId(), node1);
        subGraph.getNodes().put(node2.getId(), node2);
        subGraph.getRelationships().put(relationship1.getId(), relationship1);
        subGraph.getRelationships().put(relationship2.getId(), relationship2);
    }

    @Test
    public void ruleSpecificDecorator() throws IOException, XMLStreamException {
        Properties reportProperties = new Properties();
        reportProperties.setProperty("graphml.report.decorator", CustomGraphMLDecorator.class.getName());
        Report report = Report.builder().properties(reportProperties).build();
        stubDecorator(report, CustomGraphMLDecorator.class);
        File file = getFile();
        Map<String, Object> properties = new HashMap<>();
        XmlGraphMLWriter writer = new XmlGraphMLWriter(classHelper, YedGraphMLDecorator.class, properties);

        writer.write(result, subGraph, file);

        verify(classHelper).getType(CustomGraphMLDecorator.class.getName());
        verify(classHelper).createInstance(CustomGraphMLDecorator.class);
    }

    @Test
    public void defaultDecorator() throws IOException, XMLStreamException {
        Report report = Report.builder().build();
        stubDecorator(report, YedGraphMLDecorator.class);
        File file = getFile();
        Map<String, Object> properties = new HashMap<>();
        XmlGraphMLWriter writer = new XmlGraphMLWriter(classHelper, YedGraphMLDecorator.class, properties);

        writer.write(result, subGraph, file);

        verify(classHelper).createInstance(YedGraphMLDecorator.class);
    }

    @Test
    public void decoratorFilter() throws IOException, XMLStreamException {
        Report report = Report.builder().build();
        YedGraphMLDecorator decorator = stubDecorator(report, YedGraphMLDecorator.class);
        when(decorator.isWriteNode(node1)).thenReturn(true);
        when(decorator.isWriteRelationship(relationship1)).thenReturn(true);
        when(decorator.isWriteNode(node2)).thenReturn(false);
        when(decorator.isWriteRelationship(relationship2)).thenReturn(false);
        File file = getFile();
        Map<String, Object> properties = new HashMap<>();
        XmlGraphMLWriter writer = new XmlGraphMLWriter(classHelper, YedGraphMLDecorator.class, properties);

        writer.write(result, subGraph, file);

        verify(decorator).isWriteNode(node1);
        verify(decorator).writeNodeAttributes(node1);
        verify(decorator).writeNodeElements(node1);
        verify(decorator).isWriteRelationship(relationship1);
        verify(decorator).writeRelationshipAttributes(relationship1);
        verify(decorator).writeRelationshipElements(relationship1);

        verify(decorator).isWriteNode(node2);
        verify(decorator, never()).writeNodeAttributes(node2);
        verify(decorator, never()).writeNodeElements(node2);
        verify(decorator).isWriteRelationship(relationship2);
        verify(decorator, never()).writeRelationshipAttributes(relationship2);
        verify(decorator, never()).writeRelationshipElements(relationship2);
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

}
