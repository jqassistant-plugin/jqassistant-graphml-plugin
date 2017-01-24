package com.buschmais.jqassistant.plugin.graphml.report.api;

import java.util.Collection;
import java.util.Map;

import org.neo4j.graphdb.Label;

import com.buschmais.jqassistant.core.analysis.api.Result;
import com.buschmais.jqassistant.core.analysis.api.rule.ExecutableRule;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportHelper;
import com.buschmais.xo.api.CompositeObject;
import com.buschmais.xo.neo4j.api.model.Neo4jNode;
import com.buschmais.xo.neo4j.api.model.Neo4jRelationship;


public final class SubGraphFactory {

    private static final String ROLE = "role";
    private static final String NODE = "node";
    private static final String RELATIONSHIP = "relationship";
    private static final String GRAPH = "graph";

    private static final String LABEL = "label";
    private static final String PROPERTIES = "properties";

    private static final String LABELS = "labels";

    private static final String TYPE = "type";
    private static final String START_NODE = "startNode";
    private static final String END_NODE = "endNode";

    private static final String PARENT = "parent";
    private static final String NODES = "nodes";
    private static final String RELATIONSHIPS = "relationships";

    private long nodeId = -1;
    private long relationshipId = -1;
    private long subgraphId = -1;

    public GraphMLSubGraph createSubGraph(Result<? extends ExecutableRule> result) throws ReportException {
        GraphMLSubGraph graph = new GraphMLSubGraph();
        graph.setId(subgraphId--);
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
                        node.getProperties().putAll(properties);
                        node.setLabel((String) virtualObject.get(LABEL));
                        return (I) node;
                    case RELATIONSHIP:
                        GraphMLRelationship relationship = new GraphMLRelationship();
                        relationship.setId(relationshipId--);
                        GraphMLNode startNode = convert(virtualObject.get(START_NODE));
                        GraphMLNode endNode = convert(virtualObject.get(END_NODE));
                        String type = (String) virtualObject.get(TYPE);
                        relationship.setType(type);
                        relationship.setStartNode(startNode);
                        relationship.setEndNode(endNode);
                        relationship.getProperties().putAll(properties);
                        relationship.setLabel((String) virtualObject.get(LABEL));
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
            node.getProperties().putAll(neo4jNode.getProperties());
            return (I) node;
        } else if (value instanceof Neo4jRelationship) {
            Neo4jRelationship neo4jRelationship = (Neo4jRelationship) value;
            GraphMLRelationship relationship = new GraphMLRelationship();
            relationship.setId(neo4jRelationship.getId());
            relationship.setType(neo4jRelationship.getType().name());
            relationship.setStartNode((GraphMLNode) convert(neo4jRelationship.getStartNode()));
            relationship.setEndNode((GraphMLNode) convert(neo4jRelationship.getEndNode()));
            relationship.getProperties().putAll(neo4jRelationship.getProperties());
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
}
