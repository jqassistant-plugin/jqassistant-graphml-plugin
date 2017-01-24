package com.buschmais.jqassistant.plugin.graphml.report.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class GraphMLSubGraph extends Identifiable {

    private GraphMLNode parent = null;

    private Map<Long, GraphMLNode> nodes = new HashMap<>();

    private Map<Long, GraphMLRelationship> relationships = new HashMap<>();

    private Map<Long, GraphMLSubGraph> subGraphs = new HashMap<>();
}
