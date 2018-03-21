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

    private String testCaseId;

    public SchematronResolver(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException {
        ModuleManager moduleManager = ModuleManager.getInstance();
        ITestCaseRepository repository = moduleManager.getTestCaseRepository();
        InputStream resource;

        if(href == null || href.equals("")) {
            resource = this.getClass().getResourceAsStream(base);
        } else{
            String parentFolder = base.substring(0, base.lastIndexOf("/")+1);
            String artifactPath = parentFolder + href;
            resource = repository.getTestArtifact(testCaseId, artifactPath);
        }
        return new StreamSource(resource);
    }
}
