package com.gitb.vs.tdl;

import com.gitb.core.Actor;
import com.gitb.core.Documentation;
import com.gitb.core.TestRole;
import com.gitb.tdl.TestCase;
import com.gitb.tdl.TestCaseEntry;
import com.gitb.tdl.TestSuite;
import com.gitb.vs.tdl.util.ResourceResolver;
import com.gitb.vs.tdl.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Context {

    private static QName QNAME_TEST_CASE = new QName("http://www.gitb.com/tdl/v1/", "testcase");
    private static QName QNAME_TEST_SUITE = new QName("http://www.gitb.com/tdl/v1/", "testsuite");
    private static QName QNAME_ID = new QName("http://www.gitb.com/tdl/v1/", "id");

    private JAXBContext jaxbContext;
    private Schema tdlSchema;
    private Path testSuiteRootPath;
    private TestSuite testSuite;
    private boolean testSuiteLoaded;

    private Map<String, List<Path>> testSuitePaths;
    private Map<String, List<Path>> testCasePaths;

    private Integer testSuiteCount;
    private Integer testCaseCount;

    private Map<String, TestCase> testCases;
    private boolean testCasesLoaded;

    private Map<String, Actor> testSuiteActors;
    private Actor defaultActor;
    private boolean defaultActorLoaded;
    private Set<Path> resourcePaths;
    private boolean resourcePathsLoaded;
    private Set<String> testCaseIdsReferencedByTestSuite;
    private Set<Path> referencedResourcePaths;

    private Map<String, Map<String, TestRole>> testCaseActors;

    private ExternalConfiguration externalConfiguration;

    public Set<Path> getReferencedResourcePaths() {
        if (referencedResourcePaths == null) {
            referencedResourcePaths = new HashSet<>();
        }
        return referencedResourcePaths;
    }

    public void setTestSuiteRootPath(Path testSuiteRootPath) {
        this.testSuiteRootPath = testSuiteRootPath;
    }

    public void setExternalConfiguration(ExternalConfiguration externalConfiguration) {
        this.externalConfiguration = externalConfiguration;
    }

    public ExternalConfiguration getExternalConfiguration() {
        return externalConfiguration;
    }

    public Path resolveTestSuiteResourceIfValid(String resourcePath) {
        Path testSuiteRootPath = getTestSuiteRootPath();
        Path resolvedPath = testSuiteRootPath.resolve(resourcePath);
        if (!Files.exists(resolvedPath)) {
            if (getTestSuite() != null && getTestSuite().getMetadata() != null) {
                String testSuiteIdentifier = getTestSuite().getId();
                if (testSuiteIdentifier != null) {
                    String testSuiteIdentifierPath = testSuiteIdentifier+"/";
                    if (resourcePath.startsWith(testSuiteIdentifierPath) && resourcePath.length() > testSuiteIdentifier.length()) {
                        String pathWithoutTestSuiteIdentifier = resourcePath.substring(testSuiteIdentifierPath.length());
                        resolvedPath = testSuiteRootPath.resolve(pathWithoutTestSuiteIdentifier);
                    } else {
                        resolvedPath = null;
                    }
                }
            }
        }
        if (resolvedPath != null && !resolvedPath.normalize().startsWith(testSuiteRootPath)) {
            resolvedPath = null;
        }
        return resolvedPath;
    }

    public TestSuite getTestSuite() {
        if (!testSuiteLoaded) {
            testSuiteLoaded = true;
            if (getTestSuiteCount() == 1) {
                Path testSuitePath = getTestSuitePaths().values().iterator().next().get(0);
                try (InputStream in = Files.newInputStream(testSuitePath)) {
                    testSuite = Utils.unmarshal(in, TestSuite.class, getJAXBContext(), null, null).getValue();
                } catch (IOException | JAXBException e) {
                    // Ignore parsing errors.
                }
            }
        }
        return testSuite;
    }

    public Set<String> getTestCaseIdsReferencedByTestSuite() {
        if (testCaseIdsReferencedByTestSuite == null) {
            testCaseIdsReferencedByTestSuite = new HashSet<>();
            TestSuite testSuite = getTestSuite();
            if (testSuite != null) {
                List<TestCaseEntry> testCaseEntries = testSuite.getTestcase();
                if (testCaseEntries != null) {
                    for (TestCaseEntry entry: testCaseEntries) {
                        testCaseIdsReferencedByTestSuite.add(entry.getId());
                    }
                }

            }
        }
        return testCaseIdsReferencedByTestSuite;
    }
    
    public Map<String, TestCase> getTestCases() {
        if (!testCasesLoaded) {
            Set<String> referencedTestCaseIds = getTestCaseIdsReferencedByTestSuite();
            for (Map.Entry<String, List<Path>> entry: getTestCasePaths().entrySet()) {
                if (referencedTestCaseIds.contains(entry.getKey())) {
                    for (Path testCasePath: entry.getValue()) {
                        try (InputStream in = Files.newInputStream(testCasePath)) {
                            addTestCaseInternal(Utils.unmarshal(in, TestCase.class, getJAXBContext(), null, null).getValue());
                        } catch (IOException | JAXBException e) {
                            // Ignore parsing errors.
                        }
                    }
                }
            }
            if (testCases == null) {
                testCases = Collections.emptyMap();
            }
            testCasesLoaded = true;
        }
        return testCases;
    }

    private void addTestCaseInternal(TestCase testCase) {
        if (!StringUtils.isBlank(testCase.getId())) {
            if (testCases == null) {
                testCases = new HashMap<>();
            }
            testCases.put(testCase.getId(), testCase);
        }
    }

    public Integer getTestSuiteCount() {
        if (testSuiteCount == null) {
            int count = 0;
            for (List<Path> paths: getTestSuitePaths().values()) {
                count += paths.size();
            }
            testSuiteCount = count;
        }
        return testSuiteCount;
    }

    public Integer getTestCaseCount() {
        if (testCaseCount == null) {
            int count = 0;
            for (List<Path> paths: getTestCasePaths().values()) {
                count += paths.size();
            }
            testCaseCount = count;
        }
        return testCaseCount;
    }

    public Map<String, List<Path>> getTestCasePaths() {
        if (testCasePaths == null) {
            scanFiles();
        }
        return testCasePaths;
    }

    public Map<String, List<Path>> getTestSuitePaths() {
        if (testSuitePaths == null) {
            scanFiles();
        }
        return testSuitePaths;
    }

    private void scanFiles() {
        if (testSuiteRootPath == null) {
            throw new IllegalStateException("The test suite path is not set");
        }
        testSuitePaths = new HashMap<>();
        testCasePaths = new HashMap<>();
        resourcePaths = new HashSet<>();
        scanFiles(testSuiteRootPath.toFile());
    }

    private boolean matchesQName(Element element, QName qName) {
        return qName.getLocalPart().equals(element.getLocalName())
                && qName.getNamespaceURI().equals(element.getNamespaceURI());
    }

    private String getDocumentId(Element element) {
        String id = null;
        Attr attribute = element.getAttributeNode(QNAME_ID.getLocalPart());
        if (attribute != null) {
            id = attribute.getValue();
        }
        return id;
    }

    private void scanFiles(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file: files) {
                if (file.isDirectory()) {
                    scanFiles(file);
                } else {
                    Document document;
                    try {
                        document = Utils.readAsXML(Files.newInputStream(file.toPath()));
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                    boolean documentIsOtherResource = true;
                    if (document != null) {
                        // XML file.
                        if (matchesQName(document.getDocumentElement(), QNAME_TEST_CASE)) {
                            // Test case.
                            String id = StringUtils.defaultString(getDocumentId(document.getDocumentElement()));
                            List<Path> paths = testCasePaths.get(id);
                            if (paths == null) {
                                paths = new ArrayList<>();
                                testCasePaths.put(id, paths);
                            }
                            paths.add(file.toPath());
                            documentIsOtherResource = false;
                        } else if (matchesQName(document.getDocumentElement(), QNAME_TEST_SUITE)) {
                            // Test suite.
                            String id = StringUtils.defaultString(getDocumentId(document.getDocumentElement()));
                            List<Path> paths = testSuitePaths.get(id);
                            if (paths == null) {
                                paths = new ArrayList<>();
                                testSuitePaths.put(id, paths);
                            }
                            paths.add(file.toPath());
                            documentIsOtherResource = false;
                        }
                    }
                    if (documentIsOtherResource) {
                        resourcePaths.add(file.toPath());
                    }
                }
            }
        }
    }

    public JAXBContext getJAXBContext() {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(TestCase.class, TestSuite.class);
            } catch (JAXBException e) {
                throw new IllegalStateException(e);
            }
        }
        return jaxbContext;
    }

    public Schema getTDLSchema() {
        if (tdlSchema == null) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(new ResourceResolver());
            try {
                tdlSchema = factory.newSchema(new StreamSource(Thread.currentThread().getContextClassLoader().getResourceAsStream("schema/gitb_tdl.xsd")));
            } catch (SAXException e) {
                throw new IllegalStateException(e);
            }
        }
        return tdlSchema;
    }

    public Map<String, Actor> getTestSuiteActors() {
        if (testSuiteActors == null) {
            testSuiteActors = new HashMap<>();
            TestSuite testSuite = getTestSuite();
            if (testSuite != null && testSuite.getActors() != null && testSuite.getActors().getActor() != null) {
                for (Actor actor: testSuite.getActors().getActor()) {
                    testSuiteActors.put(actor.getId(), actor);
                }
            }
        }
        return testSuiteActors;
    }

    public Actor getDefaultActor() {
        if (!defaultActorLoaded) {
            for (Actor actor: getTestSuiteActors().values()) {
                if (actor.isDefault()) {
                    defaultActor = actor;
                }
            }
            defaultActorLoaded = true;
        }
        return defaultActor;
    }

    public Set<Path> getResourcePaths() {
        if (!resourcePathsLoaded) {
            scanFiles();
            // Consider also as plain resources any test cases that are not referenced in the test suite.
            Set<String> referencedTestCases = getTestCaseIdsReferencedByTestSuite();
            for (Map.Entry<String, List<Path>> entry: getTestCasePaths().entrySet()) {
                if (!referencedTestCases.contains(entry.getKey())) {
                    resourcePaths.addAll(entry.getValue());
                }
            }
            resourcePathsLoaded = true;
        }
        return resourcePaths;
    }

    public Path getTestSuiteRootPath() {
        return testSuiteRootPath;
    }

    public Map<String, Map<String, TestRole>> getTestCaseActors() {
        if (testCaseActors == null) {
            testCaseActors = new HashMap<>();
            for (Map.Entry<String, TestCase> entry: getTestCases().entrySet()) {
                Map<String, TestRole> roleMap = new HashMap<>();
                if (entry.getValue().getActors() != null && entry.getValue().getActors().getActor() != null) {
                    for (TestRole role: entry.getValue().getActors().getActor()) {
                        roleMap.put(role.getId(), role);
                    }
                }
                testCaseActors.put(entry.getKey(), roleMap);
            }
        }
        return testCaseActors;
    }

    public void validateDocumentation(Documentation documentation, BiConsumer<String, String> referenceAndEmbeddedFunction, Consumer<String> resourceNotFoundFunction) {
        if (documentation != null) {
            boolean hasDocumentationReference = documentation.getImport() != null && !documentation.getImport().isBlank();
            boolean hasDocumentationEmbedded = documentation.getValue() != null && !documentation.getValue().isBlank();
            if (hasDocumentationReference && hasDocumentationEmbedded) {
                referenceAndEmbeddedFunction.accept(documentation.getImport(), documentation.getValue());
            } else if (hasDocumentationReference) {
                Path resolvedPath = resolveTestSuiteResourceIfValid(documentation.getImport());
                if (resolvedPath == null) {
                    resourceNotFoundFunction.accept(documentation.getImport());
                } else {
                    getReferencedResourcePaths().add(resolvedPath.toAbsolutePath());
                }
            }
        }
    }

}
