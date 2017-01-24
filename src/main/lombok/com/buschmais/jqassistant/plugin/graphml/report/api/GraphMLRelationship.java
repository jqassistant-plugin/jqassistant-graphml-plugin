package com.buschmais.jqassistant.plugin.graphml.report.api;

import lombok.Data;

@Data
public class GraphMLRelationship extends GraphMLPropertyContainer {

    private String type;

    private GraphMLNode startNode;

    private GraphMLNode endNode;
    
}
