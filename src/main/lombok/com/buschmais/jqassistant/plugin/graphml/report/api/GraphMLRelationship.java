package com.buschmais.jqassistant.plugin.graphml.report.api;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class GraphMLRelationship extends GraphMLPropertyContainer {

    private String type;

    private GraphMLNode startNode;

    private GraphMLNode endNode;
    
}
