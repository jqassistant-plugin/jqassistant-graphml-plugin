package com.buschmais.jqassistant.plugin.graphml.report.impl;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import com.buschmais.jqassistant.core.analysis.api.Result;
import com.buschmais.jqassistant.core.analysis.api.rule.Concept;
import com.buschmais.jqassistant.core.analysis.api.rule.ExecutableRule;
import com.buschmais.jqassistant.core.analysis.api.rule.Rule;
import com.buschmais.jqassistant.core.report.api.*;
import com.buschmais.jqassistant.core.report.api.ReportPlugin.Default;
import com.buschmais.jqassistant.core.report.api.graph.SubGraphFactory;
import com.buschmais.jqassistant.core.report.api.graph.model.SubGraph;
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
@Default
public class GraphMLReportPlugin implements ReportPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphMLReportPlugin.class);

    public static final String GRAPHML = "graphml";

    public static final String FILEEXTENSION_GRAPHML = ".graphml";

    private static final String CONCEPT_PATTERN = "graphml.report.conceptPattern";
    private static final String GRAPHMML_REPORT_DIRECTORY = "graphml.report.directory";
    private static final String GRAPHML_DEFAULT_DECORATOR = "graphml.report.defaultDecorator";

    private ReportContext reportContext;
    private String conceptPattern = ".*\\.graphml$";
    private File reportDirectory;
    private SubGraphFactory subGraphFactory;
    private XmlGraphMLWriter xmlGraphMLWriter;

    @Override
    public void configure(ReportContext reportContext, Map<String, Object> properties) {
        this.reportContext = reportContext;
        this.conceptPattern = getProperty(properties, CONCEPT_PATTERN, conceptPattern);

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
        Set<String> selectedReports = result.getRule().getReport().getSelectedTypes();
        if ((selectedReports != null && selectedReports.contains(GRAPHML)) || (rule instanceof Concept && rule.getId().matches(conceptPattern))) {
            SubGraph subGraph = subGraphFactory.createSubGraph(result);
            try {
                String fileName = ReportHelper.escapeRuleId(rule);
                if (!fileName.endsWith(FILEEXTENSION_GRAPHML)) {
                    fileName = fileName + FILEEXTENSION_GRAPHML;
                }
                File file = new File(reportDirectory, fileName);
                xmlGraphMLWriter.write(result, subGraph, file);
                reportContext.addReport("GraphML", result.getRule(), ReportContext.ReportType.LINK, file.toURI().toURL());
            } catch (IOException | XMLStreamException e) {
                throw new ReportException("Cannot write custom report.", e);
            }
        }
    }
}
