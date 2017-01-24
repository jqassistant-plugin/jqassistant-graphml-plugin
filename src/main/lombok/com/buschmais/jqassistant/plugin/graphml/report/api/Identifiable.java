package com.buschmais.jqassistant.plugin.graphml.report.api;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@ToString
public abstract class Identifiable {

    private long id;

    private String label;

}
