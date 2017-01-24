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
import com.buschmais.xo.neo4j.api.model.Neo4jNode;
import com.buschmais.xo.neo4j.api.model.Neo4jRelationship;


/** A report plugin that creates GraphML files based on the results of a concept.
 *
 * @author mh
 * @author Dirk Mahler */
public class GraphMLReportPlugin implements ReportPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphMLReportPlugin.class);

    public static final String GRAPHML = "graphml";

    public static final String FILEEXTENSION_GRAPHML = ".graphml";

    private static final String CONCEPT_PATTERN = "graphml.report.conceptPattern";
    private static final String DIRECTORY = "graphml.report.directory";
    private static final String GRAPHML_DEFAULT_DECORATOR = "graphml.report.defaultDecorator";

    private static final String ROLE = "role";
    private static final String NODE = "node";
    private static final String RELATIONSHIP = "relationship";
    private static final String GRAPH = "graph";

    private static final String LABELS = "labels";
    private static final String PROPERTIES = "properties";

    private static final String TYPE = "type";
    private static final String START_NODE = "startNode";
    private static final String END_NODE = "endNode";

    private static final String PARENT = "parent";
    private static final String NODES = "nodes";
    private static final String RELATIONSHIPS = "relationships";

    private String conceptPattern = ".*\\.graphml$";
    private String directory = "jqassistant/report";
    private XmlGraphMLWriter xmlGraphMLWriter;

    private long nodeId;
    private long relationshipId;
    private long subgraphId;

    @Override
    public void initialize() throws ReportException {}

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
    public void end() throws ReportException {}

    @Override
    public void beginConcept(Concept concept) throws ReportException {}

    @Override
    public void endConcept() throws ReportException {}

    @Override
    public void beginGroup(Group group) throws ReportException {}

    @Override
    public void endGroup() throws ReportException {}

    @Override
    public void beginConstraint(Constraint constraint) throws ReportException {}

    @Override
    public void endConstraint() throws ReportException {}

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
        graph.setId(0);
        for (Map<String, Object> row : result.getRows()) {
            for (Object value : row.values()) {
                convert(graph, value);
            }
        }
        return graph;
    }

    private <I extends Identifiable> I convert(GraphMLSubGraph parent, Object value) throws ReportException {
        I identifiable = convert(value);
        if (identifiable != null) {
            if (identifiable instanceof GraphMLNode) {
                parent.getNodes().put(identifiable.getId(), (GraphMLNode) identifiable);
            } else if (identifiable instanceof GraphMLRelationship) {
                parent.getRelationships().put(identifiable.getId(), (GraphMLRelationship) identifiable);
            } else if (identifiable instanceof GraphMLSubGraph) {
                parent.getSubGraphs().put(identifiable.getId(), (GraphMLSubGraph) identifiable);
            }
        }
        return identifiable;
    }

    private <I extends Identifiable> I convert(Object value) throws ReportException {
        if (value == null) {
            return null;
        } else if (value instanceof Map) {
            Map<String, Object> virtualObject = (Map) value;
            Object role = virtualObject.get(ROLE);
            if (role != null) {
                Map<String, Object> properties = (Map<String, Object>) virtualObject.get(PROPERTIES);
                switch (role.toString().toLowerCase()) {
                    case NODE:
                        GraphMLNode node = new GraphMLNode();
                        node.setId(nodeId--);
                        Collection<String> labels = (Collection<String>) virtualObject.get(LABELS);
                        node.getLabels().addAll(labels);
                        setProperties(properties, node);
                        return (I) node;
                    case RELATIONSHIP:
                        GraphMLRelationship relationship = new GraphMLRelationship();
                        relationship.setId(relationshipId--);
                        setProperties(properties, relationship);
                        GraphMLNode startNode = convert(virtualObject.get(START_NODE));
                        GraphMLNode endNode = convert(virtualObject.get(END_NODE));
                        String type = (String) virtualObject.get(TYPE);
                        relationship.setType(type);
                        relationship.setStartNode(startNode);
                        relationship.setEndNode(endNode);
                        return (I) relationship;
                    case GRAPH:
                        GraphMLSubGraph subgraph = new GraphMLSubGraph();
                        subgraph.setId(subgraphId--);
                        subgraph.setParent((GraphMLNode) convert(subgraph, virtualObject.get(PARENT)));
                        addSubGraphChildren(subgraph, virtualObject, NODES, subgraph.getNodes());
                        addSubGraphChildren(subgraph, virtualObject, RELATIONSHIPS, subgraph.getRelationships());
                        return (I) subgraph;
                }
            }
        } else if (value instanceof CompositeObject) {
            CompositeObject compositeObject = (CompositeObject) value;
            I identifiable = convert(compositeObject.getDelegate());
            identifiable.setLabel(ReportHelper.getLabel(value));
            return identifiable;
        } else if (value instanceof Neo4jNode) {
            Neo4jNode neo4jNode = (Neo4jNode) value;
            GraphMLNode node = new GraphMLNode();
            node.setId(neo4jNode.getId());
            for (Label label : neo4jNode.getLabels()) {
                node.getLabels().add(label.name());
            }
            setProperties(neo4jNode.getProperties(), node);
            return (I) node;
        } else if (value instanceof Neo4jRelationship) {
            Neo4jRelationship neo4jRelationship = (Neo4jRelationship) value;
            GraphMLRelationship relationship = new GraphMLRelationship();
            relationship.setId(neo4jRelationship.getId());
            relationship.setType(neo4jRelationship.getType().name());
            relationship.setStartNode((GraphMLNode) convert(neo4jRelationship.getStartNode()));
            relationship.setEndNode((GraphMLNode) convert(neo4jRelationship.getEndNode()));
            setProperties(neo4jRelationship.getProperties(), relationship);
            return (I) relationship;
        }
        throw new ReportException("Element type not supported: " + value);
    }

    private <I extends Identifiable> void addSubGraphChildren(GraphMLSubGraph subgraph, Map<String, Object> virtualObject, String key, Map<Long, I> childMap) throws ReportException {
        Collection<Object> children = (Collection<Object>) virtualObject.get(key);
        if (children != null) {
            for (Object child : children) {
                I identifiable = convert(subgraph, child);
                childMap.put(identifiable.getId(), identifiable);
            }
        }
    }

    private void setProperties(Map<String, Object> m, GraphMLPropertyContainer propertyContainer) {
        Map<String, Object> properties = propertyContainer.getProperties();
        for (Map.Entry<String, Object> entry : m.entrySet()) {
            String propertyKey = entry.getKey();
            Object propertyValue = entry.getValue();
            properties.put(propertyKey, propertyValue);
        }
    }
}
