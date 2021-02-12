package com.gitb.validation.schematron;

import com.gitb.ModuleManager;
import com.gitb.repository.ITestCaseRepository;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

/**
 * Created by senan on 30.10.2014.
 */
public class SchematronResolver implements URIResolver {

    private static final String PROTOCOL = "file:///";

    /**
     * Path to the folder that contains root resource (i.e. and XSD schema or Schematron file, etc)
     */
    private final String resource;
    private final String testSuiteId;
    private final String testCaseId;

    public SchematronResolver(String testSuiteId, String testCaseId, String path) {
        this.testSuiteId = testSuiteId;
        this.testCaseId = testCaseId;
        this.resource = path;
    }

    @Override
    public Source resolve(String href, String baseURI) throws TransformerException {
        ModuleManager moduleManager = ModuleManager.getInstance();
        ITestCaseRepository repository = moduleManager.getTestCaseRepository();
        String parentFolder;
        if (baseURI == null || baseURI.isBlank()) {
            parentFolder = this.resource.substring(0, this.resource.lastIndexOf("/")+1);
        } else {
            parentFolder = baseURI.substring(PROTOCOL.length(), baseURI.lastIndexOf("/")+1);
        }

        String artifactPath = parentFolder + href;

        InputStream resource  = repository.getTestArtifact(testSuiteId, testCaseId, artifactPath);
        if(resource != null) {
            return new StreamSource(resource);
        }
        return null;
    }
}
