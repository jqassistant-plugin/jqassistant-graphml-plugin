package com.buschmais.jqassistant.plugin.graphml.report.api;

import java.util.Set;
import java.util.TreeSet;

import lombok.Data;

@Data
public class GraphMLNode extends GraphMLPropertyContainer {

    private Set<String> labels = new TreeSet<>();

}
