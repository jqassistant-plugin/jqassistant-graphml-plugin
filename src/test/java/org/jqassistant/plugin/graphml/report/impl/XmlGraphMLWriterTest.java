package org.jqassistant.plugin.graphml.report.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import com.buschmais.jqassistant.core.report.api.graph.model.Node;
import com.buschmais.jqassistant.core.report.api.graph.model.Relationship;
import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.Concept;
import com.buschmais.jqassistant.core.rule.api.model.Report;
import com.buschmais.jqassistant.core.shared.reflection.ClassHelper;

import org.jqassistant.plugin.graphml.report.api.GraphMLDecorator;
import org.jqassistant.plugin.graphml.report.decorator.YedGraphMLDecorator;
import org.jqassistant.plugin.graphml.test.CustomGraphMLDecorator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class XmlGraphMLWriterTest {

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
    void setUp() {
        doReturn(concept).when(result).getRule();

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

        SubGraph level2SubGraph = new SubGraph();
        level2SubGraph.getNodes()
            .put(node1.getId(), node1);
        level2SubGraph.getNodes()
            .put(node2.getId(), node2);
        level2SubGraph.getRelationships()
            .put(relationship1.getId(), relationship1);
        level2SubGraph.getRelationships()
            .put(relationship2.getId(), relationship2);

        SubGraph level1SubGraph = new SubGraph();
        level1SubGraph.getSubGraphs().put(2L, level2SubGraph);

        subGraph = new SubGraph();
        subGraph.getSubGraphs()
            .put(1L, level1SubGraph);
    }

    @Test
    void ruleSpecificDecorator() throws IOException, XMLStreamException {
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
    void defaultDecorator() throws IOException, XMLStreamException {
        Report report = Report.builder().build();
        stubDecorator(report, YedGraphMLDecorator.class);
        File file = getFile();
        Map<String, Object> properties = new HashMap<>();
        XmlGraphMLWriter writer = new XmlGraphMLWriter(classHelper, YedGraphMLDecorator.class, properties);

        writer.write(result, subGraph, file);

        verify(classHelper).createInstance(YedGraphMLDecorator.class);
    }

    @Test
    void allElements() throws IOException, XMLStreamException {
        Report report = Report.builder()
            .build();
        YedGraphMLDecorator decorator = stubDecorator(report, YedGraphMLDecorator.class);
        when(decorator.isWriteNode(any(Node.class))).thenReturn(true);
        when(decorator.isWriteRelationship(any(Relationship.class))).thenReturn(true);
        File file = getFile();
        XmlGraphMLWriter writer = new XmlGraphMLWriter(classHelper, YedGraphMLDecorator.class, emptyMap());

        writer.write(result, subGraph, file);

        verify(decorator).isWriteNode(node1);
        verify(decorator).writeNodeAttributes(node1);
        verify(decorator).writeNodeElements(node1);
        verify(decorator).isWriteRelationship(relationship1);
        verify(decorator).writeRelationshipAttributes(relationship1);
        verify(decorator).writeRelationshipElements(relationship1);

        verify(decorator).isWriteNode(node2);
        verify(decorator).writeNodeAttributes(node2);
        verify(decorator).writeNodeElements(node2);
        verify(decorator).isWriteRelationship(relationship2);
        verify(decorator).writeRelationshipAttributes(relationship2);
        verify(decorator).writeRelationshipElements(relationship2);
    }

    @Test
    void decoratorFilter() throws IOException, XMLStreamException {
        Report report = Report.builder().build();
        YedGraphMLDecorator decorator = stubDecorator(report, YedGraphMLDecorator.class);
        when(decorator.isWriteNode(node1)).thenReturn(true);
        when(decorator.isWriteRelationship(relationship1)).thenReturn(true);
        when(decorator.isWriteNode(node2)).thenReturn(false);
        when(decorator.isWriteRelationship(relationship2)).thenReturn(false);
        File file = getFile();
        XmlGraphMLWriter writer = new XmlGraphMLWriter(classHelper, YedGraphMLDecorator.class, emptyMap());

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
