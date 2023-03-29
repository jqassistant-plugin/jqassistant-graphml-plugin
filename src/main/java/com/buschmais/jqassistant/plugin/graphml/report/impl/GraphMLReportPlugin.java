package com.buschmais.jqassistant.plugin.graphml.report.impl;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportHelper;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.ExecutableRule;
import com.buschmais.jqassistant.core.rule.api.model.Rule;
import com.buschmais.jqassistant.core.shared.reflection.ClassHelper;
import com.buschmais.jqassistant.plugin.graphml.report.api.GraphMLDecorator;
import com.buschmais.jqassistant.plugin.graphml.report.decorator.YedGraphMLDecorator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A report plugin that creates GraphML files based on the results of a concept.
 *
 * @author mh
 * @author Dirk Mahler
 */
public class GraphMLReportPlugin implements ReportPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphMLReportPlugin.class);

    public static final String FILEEXTENSION_GRAPHML = ".graphml";

    private static final String CONCEPT_PATTERN = "graphml.report.conceptPattern";
    private static final String GRAPHMML_REPORT_DIRECTORY = "graphml.report.directory";
    private static final String GRAPHML_DEFAULT_DECORATOR = "graphml.report.defaultDecorator";

    private ReportContext reportContext;
    private File reportDirectory;
    private SubGraphFactory subGraphFactory;
    private XmlGraphMLWriter xmlGraphMLWriter;

    @Override
    public void configure(ReportContext reportContext, Map<String, Object> properties) {
        this.reportContext = reportContext;

        String reportDirectoryValue = (String) properties.get(GRAPHMML_REPORT_DIRECTORY);
        this.reportDirectory = reportDirectoryValue != null ? new File(reportDirectoryValue) : reportContext.getReportDirectory("graphml");
        if (reportDirectory.mkdirs()) {
            LOGGER.info("Created directory " + reportDirectory.getAbsolutePath());
        }
        String defaultDecorator = getProperty(properties, GRAPHML_DEFAULT_DECORATOR, YedGraphMLDecorator.class.getName());
        ClassHelper classHelper = new ClassHelper(GraphMLReportPlugin.class.getClassLoader());
        Class<GraphMLDecorator> defaultDecoratorType = classHelper.getType(defaultDecorator);
        xmlGraphMLWriter = new XmlGraphMLWriter(classHelper, defaultDecoratorType, properties);
    }

    private String getProperty(Map<String, Object> properties, String property, String defaultValue) {
        String value = (String) properties.get(property);
        return value != null ? value : defaultValue;
    }

    @Override
    public void begin() {
        subGraphFactory = new SubGraphFactory();
    }

    @Override
    public void setResult(Result<? extends ExecutableRule> result) throws ReportException {
        Rule rule = result.getRule();
        SubGraph subGraph = subGraphFactory.createSubGraph(result);
        try {
            String fileName = ReportHelper.escapeRuleId(rule) + FILEEXTENSION_GRAPHML;
            File file = new File(reportDirectory, fileName);
            xmlGraphMLWriter.write(result, subGraph, file);
            reportContext.addReport("GraphML", result.getRule(), ReportContext.ReportType.LINK, file.toURI()
                .toURL());
        } catch (IOException | XMLStreamException e) {
            throw new ReportException("Cannot write custom report.", e);
        }
    }
}
