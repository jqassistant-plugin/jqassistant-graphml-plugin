package com.buschmais.jqassistant.plugin.graphml.report.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.neo4j.graphdb.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buschmais.jqassistant.core.analysis.api.Result;
import com.buschmais.jqassistant.core.analysis.api.rule.*;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportHelper;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.shared.reflection.ClassHelper;
import com.buschmais.jqassistant.plugin.graphml.report.api.*;
import com.buschmais.jqassistant.plugin.graphml.report.decorator.YedGraphMLDecorator;
import com.buschmais.xo.api.CompositeObject;
import com.buschmais.xo.neo4j.api.model.AbstractNeo4jPropertyContainer;
import com.buschmais.xo.neo4j.api.model.Neo4jNode;
import com.buschmais.xo.neo4j.api.model.Neo4jRelationship;

/**
 * A report plugin that creates GraphML files based on the results of a concept.
 *
 * @author mh
 * @author Dirk Mahler
 */
public class GraphMLReportPlugin implements ReportPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphMLReportPlugin.class);

    public static final String GRAPHML = "graphml";

    public static final String FILEEXTENSION_GRAPHML = ".graphml";

    private static final String CONCEPT_PATTERN = "graphml.report.conceptPattern";
    private static final String DIRECTORY = "graphml.report.directory";
    private static final String GRAPHML_DEFAULT_DECORATOR = "graphml.report.defaultDecorator";

    private String conceptPattern = ".*\\.graphml$";
    private String directory = "jqassistant/report";
    private XmlGraphMLWriter xmlGraphMLWriter;

    long nodeId;
    long relationshipId;
    long subgraphId;

    @Override
    public void initialize() throws ReportException {
    }

    @Override
    public void configure(Map<String, Object> properties) throws ReportException {
        this.conceptPattern = getProperty(properties, CONCEPT_PATTERN, conceptPattern);
        this.directory = getProperty(properties, DIRECTORY, directory);
        String defaultDecorator = getProperty(properties, GRAPHML_DEFAULT_DECORATOR, YedGraphMLDecorator.class.getName());
        ClassHelper classHelper = new ClassHelper(GraphMLReportPlugin.class.getClassLoader());
        Class<GraphMLDecorator> defaultDecoratorType = classHelper.getType(defaultDecorator);
        xmlGraphMLWriter = new XmlGraphMLWriter(classHelper, defaultDecoratorType, properties);
    }

    private String getProperty(Map<String, Object> properties, String property, String defaultValue) throws ReportException {
        String value = (String) properties.get(property);
        return value != null ? value : defaultValue;
    }

    @Override
    public void begin() throws ReportException {
        nodeId = -1;
        relationshipId = -1;
        subgraphId = -1;
    }

    @Override
    public void end() throws ReportException {
    }

    @Override
    public void beginConcept(Concept concept) throws ReportException {
    }

    @Override
    public void endConcept() throws ReportException {
    }

    @Override
    public void beginGroup(Group group) throws ReportException {
    }

    @Override
    public void endGroup() throws ReportException {
    }

    @Override
    public void beginConstraint(Constraint constraint) throws ReportException {
    }

    @Override
    public void endConstraint() throws ReportException {
    }

    @Override
    public void setResult(Result<? extends ExecutableRule> result) throws ReportException {
        Rule rule = result.getRule();
        Set<String> selectedReports = result.getRule().getReport().getSelectedTypes();
        if ((selectedReports != null && selectedReports.contains(GRAPHML)) || (rule instanceof Concept && rule.getId().matches(conceptPattern))) {
            GraphMLSubGraph subGraph = getSubGraph(result);
            try {
                String fileName = rule.getId().replaceAll("\\:", "_");
                if (!fileName.endsWith(FILEEXTENSION_GRAPHML)) {
                    fileName = fileName + FILEEXTENSION_GRAPHML;
                }
                File directory = new File(this.directory);
                if (directory.mkdirs()) {
                    LOGGER.info("Created directory " + directory.getAbsolutePath());
                }
                File file = new File(directory, fileName);
                xmlGraphMLWriter.write(result, subGraph, file);
            } catch (IOException | XMLStreamException e) {
                throw new ReportException("Cannot write custom report.", e);
            }
        }
    }

    private GraphMLSubGraph getSubGraph(Result<? extends ExecutableRule> result) throws ReportException {
        GraphMLSubGraph graph = new GraphMLSubGraph();
        for (Map<String, Object> row : result.getRows()) {
            for (Object value : row.values()) {
                convert(graph, value);
            }
        }
        return graph;
    }

    private <I extends Identifiable> I convert(GraphMLSubGraph parent, Object value) throws ReportException {
        if (value instanceof Map) {
            Map<String, Object> virtualObject = (Map) value;
            Object role = virtualObject.get("role");
            if (role != null) {
                switch (role.toString().toLowerCase()) {
                case "node":
                    GraphMLNode node = new GraphMLNode();
                    node.setId(nodeId--);
                    Collection<String> labels = (Collection<String>) virtualObject.get("labels");
                    node.getLabels().addAll(labels);
                    setProperties(virtualObject, node);
                    parent.getNodes().put(node.getId(), node);
                    return (I) node;
                case "relationship":
                    GraphMLRelationship relationship = new GraphMLRelationship();
                    relationship.setId(relationshipId--);
                    setProperties(virtualObject, relationship);
                    GraphMLNode startNode = convert(parent, virtualObject.get("startNode"));
                    GraphMLNode endNode = convert(parent, virtualObject.get("endNode"));
                    String type = (String) virtualObject.get("type");
                    relationship.setType(type);
                    relationship.setStartNode(startNode);
                    relationship.setEndNode(endNode);
                    parent.getRelationships().put(relationship.getId(), relationship);
                    return (I) relationship;
                case "graph":
                    GraphMLSubGraph subgraph = new GraphMLSubGraph();
                    subgraph.setId(subgraphId--);
                    parent.getSubGraphs().put(subgraph.getId(), subgraph);
                    return (I) subgraph;
                }
            }
        } else if (value instanceof CompositeObject) {
            CompositeObject compositeObject = (CompositeObject) value;
            AbstractNeo4jPropertyContainer<?> propertyContainer = compositeObject.getDelegate();
            Map<String, Object> properties = propertyContainer.getProperties();
            GraphMLPropertyContainer graphMLPropertyContainer;
            if (propertyContainer instanceof Neo4jNode) {
                GraphMLNode graphMLNode = new GraphMLNode();
                Neo4jNode neo4jNode = (Neo4jNode) propertyContainer;
                graphMLNode.setId(neo4jNode.getId());
                for (Label label : neo4jNode.getLabels()) {
                    graphMLNode.getLabels().add(label.name());
                }
                parent.getNodes().put(graphMLNode.getId(), graphMLNode);
                graphMLPropertyContainer = graphMLNode;
            } else if (propertyContainer instanceof Neo4jRelationship) {
                GraphMLRelationship graphMLRelationship = new GraphMLRelationship();
                Neo4jRelationship neo4jRelationship = (Neo4jRelationship) propertyContainer;
                graphMLRelationship.setType(neo4jRelationship.getType().name());
                parent.getRelationships().put(graphMLRelationship.getId(), graphMLRelationship);
                graphMLPropertyContainer = graphMLRelationship;
            } else {
                throw new ReportException("Element type not supported: " + compositeObject);
            }
            setProperties(properties, graphMLPropertyContainer);
            String nodeLabel = ReportHelper.getLabel(value);
            graphMLPropertyContainer.setLabel(nodeLabel);
            return (I) graphMLPropertyContainer;
        }
        return null;
    }

    private void setProperties(Map<String, Object> m, GraphMLPropertyContainer propertyContainer) {
        Map<String, Object> properties = propertyContainer.getProperties();
        for (Map.Entry<String, Object> entry : m.entrySet()) {
            String propertyKey = entry.getKey();
            Object propertyValue = entry.getValue();
            if (!"role".equals(propertyKey)) {
                properties.put(propertyKey, propertyValue);
            } else if ("label".equals(propertyKey)) {
                propertyContainer.setLabel(propertyValue.toString());
            }
        }
    }
}
