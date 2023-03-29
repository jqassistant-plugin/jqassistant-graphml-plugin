package com.buschmais.jqassistant.plugin.graphml.test;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.rule.api.model.Concept;
import com.buschmais.jqassistant.plugin.graphml.test.set.a.A;
import com.buschmais.jqassistant.plugin.graphml.test.set.b.B;
import com.buschmais.jqassistant.plugin.java.test.AbstractJavaPluginIT;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import static com.buschmais.jqassistant.core.report.api.ReportContext.ReportType.LINK;
import static com.buschmais.jqassistant.plugin.graphml.report.impl.GraphMLReportPlugin.FILEEXTENSION_GRAPHML;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Verifies functionality of the GraphML report plugin.
 */class GraphMLReportPluginIT extends AbstractJavaPluginIT {

    private static final String REPORT_DIR = "target/graphml";

    static class TestClass {

        private String name;

        TestClass() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Override
    protected Map<String, Object> getReportProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("graphml.report.directory", REPORT_DIR);
        properties.put("graphml.decorator", CustomGraphMLDecorator.class.getName());
        return properties;
    }

    @Test
    void renderGraphML() throws Exception {
        reportAndVerify("test:DeclaredMembers", 4);
    }

    @Test
    void renderGraphMLUsingVirtualRelation() throws Exception {
        reportAndVerify("test:DeclaredMembersWithVirtualRelation", 4);
    }

    @Test
    void renderGraphMLUsingSubgraph() throws Exception {
        Document doc = scanAndWriteReport("test:DeclaredMembersWithSubgraph", A.class, B.class);
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        // XPathExpression classExpression =
        // xpath.compile("/graphml/graph/node[contains(@labels,':Class')]/data[@key='fqn']");
        XPathExpression classExpression = xpath.compile("/graphml/graph/node[contains(@labels,':Class')]");
        XPathExpression methodExpression = xpath.compile("graph/node[contains(@labels,':Method')]");
        XPathExpression fqnExpression = xpath.compile("data[@key='fqn']");
        XPathExpression nameExpression = xpath.compile("data[@key='name']");

        NodeList classes = (NodeList) classExpression.evaluate(doc, XPathConstants.NODESET);

        Map<String, Class<?>> expectedClasses = new HashMap<>();
        expectedClasses.put(A.class.getName(), A.class);
        expectedClasses.put(B.class.getName(), B.class);
        int classCount = classes.getLength();
        assertThat("Number of classes in report does not match.", classCount, equalTo(expectedClasses.size()));
        for (int i = 0; i < classCount; i++) {
            Node classNode = classes.item(i);
            Node classNameNode = (Node) fqnExpression.evaluate(classNode, XPathConstants.NODE);
            String className = classNameNode.getTextContent();
            assertThat("Expecting class in report.", expectedClasses.keySet().contains(className), equalTo(true));
            Class<?> expectedClass = expectedClasses.get(className);
            Set<String> expectedMethods = new HashSet<>();
            expectedMethods.add("<init>");
            for (Method method : expectedClass.getDeclaredMethods()) {
                expectedMethods.add(method.getName());
            }
            NodeList methods = (NodeList) methodExpression.evaluate(classNode, XPathConstants.NODESET);
            int methodCount = methods.getLength();
            assertThat(methodCount, equalTo(expectedMethods.size()));
            for (int k = 0; k < methodCount; k++) {
                Node methodNode = methods.item(k);
                Node methodNameNode = (Node) nameExpression.evaluate(methodNode, XPathConstants.NODE);
                String methodName = methodNameNode.getTextContent();
                assertThat(expectedMethods.contains(methodName), equalTo(true));
            }
        }
        XPathExpression edgeExpression = xpath.compile("//edge");
        NodeList edges = (NodeList) edgeExpression.evaluate(doc, XPathConstants.NODESET);
        assertThat(edges.getLength(), equalTo(2));
    }

    @Test
    void renderGraphMLUsingVirtualNode() throws Exception {
        Document doc = scanAndWriteReport("test:DeclaredMembersWithVirtualNode", TestClass.class);

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression classExpression = xpath
                .compile("/graphml/graph/node[contains(@labels,':CyclomaticComplexity')]/data[@key='totalCyclomaticComplexity']");
        String complexity = classExpression.evaluate(doc);
        assertThat(complexity, equalTo("3"));

    }

    @Test
    void uniqueElementsPerSubGraph() throws Exception {
        Document doc = scanAndWriteReport("test:RedundantNodesAndRelations", TestClass.class);
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        NodeList classNodes = (NodeList) xpath.compile("/graphml/graph/node[contains(@labels,':Class')]").evaluate(doc, XPathConstants.NODESET);
        assertThat(classNodes.getLength(), equalTo(1));
        NodeList methodNodes = (NodeList) xpath.compile("/graphml/graph/node[contains(@labels,':Constructor')]").evaluate(doc, XPathConstants.NODESET);
        assertThat(methodNodes.getLength(), equalTo(1));
        NodeList declaresRelations = (NodeList) xpath.compile("/graphml/edge[@label='DECLARES']").evaluate(doc, XPathConstants.NODESET);
        assertThat(declaresRelations.getLength(), equalTo(1));
    }

    private void reportAndVerify(String conceptName, int assertedEdges) throws Exception {
        Document doc = scanAndWriteReport(conceptName, TestClass.class);
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        XPathExpression classExpression = xpath.compile("/graphml/graph/node[contains(@labels,':Class')]/data[@key='fqn']");
        String fqn = classExpression.evaluate(doc);
        assertThat(fqn, equalTo(TestClass.class.getName()));
        XPathExpression labelExpression = xpath
                .compile("/graphml/graph/node[contains(@labels,':Class')]/data/ProxyAutoBoundsNode/Realizers/GroupNode/NodeLabel");
        NodeList labels = (NodeList) labelExpression.evaluate(doc, XPathConstants.NODESET);
        assertThat(labels.getLength(), greaterThan(0));
        for (int i = 0; i < labels.getLength(); i++) {
            assertThat(labels.item(0).getFirstChild().getTextContent().length(), greaterThan(0));
        }
        XPathExpression declaresExpression = xpath.compile("//edge");
        NodeList edges = (NodeList) declaresExpression.evaluate(doc, XPathConstants.NODESET);
        assertThat(edges.getLength(), equalTo(assertedEdges));
    }

    private Document scanAndWriteReport(String conceptName, Class<?>... scanClasses) throws Exception {
        scanClasses(scanClasses);
        applyConcept(conceptName);
        String fileName = conceptName.replace(':', '_') + FILEEXTENSION_GRAPHML;
        File reportFile = new File(REPORT_DIR, fileName);
        assertThat(reportFile.exists(), equalTo(true));

        // Verify report context
        Concept concept = ruleSet.getConceptBucket().getById(conceptName);
        List<ReportContext.Report<?>> reports = reportContext.getReports(concept);
        assertThat(reports.size(), equalTo(1));
        ReportContext.Report<?> report = reports.get(0);
        assertThat(report.getLabel(), equalTo("GraphML"));
        assertThat(report.getRule(), is(concept));
        assertThat(report.getReportType(), equalTo(LINK));
        assertThat(report.getUrl(), equalTo(reportFile.toURI().toURL()));

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new FileReader(reportFile)));
    }
}
