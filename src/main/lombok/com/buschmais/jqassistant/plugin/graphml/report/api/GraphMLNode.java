package com.buschmais.jqassistant.plugin.graphml.report.api;

import java.util.Set;
import java.util.TreeSet;

import lombok.*;

@Getter
@Setter
@ToString(callSuper = true)
public class GraphMLNode extends GraphMLPropertyContainer {

    private Set<String> labels = new TreeSet<>();

}
