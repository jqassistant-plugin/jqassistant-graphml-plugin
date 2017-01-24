package com.buschmais.jqassistant.plugin.graphml.report.api;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public abstract class GraphMLPropertyContainer extends Identifiable {

    private Map<String,Object> properties = new HashMap<>();

}
