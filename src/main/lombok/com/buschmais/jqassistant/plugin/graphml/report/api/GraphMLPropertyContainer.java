package com.buschmais.jqassistant.plugin.graphml.report.api;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public abstract class GraphMLPropertyContainer extends Identifiable {

    private String label;

    private Map<String,Object> properties = new HashMap<>();

}
