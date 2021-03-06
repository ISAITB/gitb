package com.gitb.repository;

import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import com.gitb.utils.XMLUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;

/**
 * Created by senan on 9/11/14.
 */
@MetaInfServices(ITestCaseRepository.class)
public class LocalTestCaseRepository implements ITestCaseRepository {

	private static final Logger logger = LoggerFactory.getLogger(LocalTestCaseRepository.class);

    private Configuration configuration;

    public LocalTestCaseRepository() {
        configuration = Configuration.defaultConfiguration();
    }

    @Override
    public String getName() {
        return "local-repository";
    }

    @Override
    public TestCase getTestCase(String testCaseId) {
        try {
            File resource = getTestResource(testCaseId, testCaseId, true);
            if (resource.exists()) {
                return XMLUtils.unmarshal(TestCase.class, new StreamSource(resource));
            }
        } catch (Exception e) {
            logger.error("Exception when unmarshalling the test case with id ["+testCaseId+"]", e);
        }
        return null;
    }

	@Override
	public Scriptlet getScriptlet(String from, String testCaseId, String scriptletPath) {
        try {
            File resource = getTestResource(testCaseId, scriptletPath, false);
            return XMLUtils.unmarshal(Scriptlet.class, new StreamSource(resource));
        } catch (Exception e) {
            logger.error("Exception when unmarshalling the scriptlet with id ["+scriptletPath+"]", e);
        }
        return null;
	}

    @Override
    public InputStream getTestArtifact(String from, String testCaseId, String pathToResource) {
        try {
            String path = configuration.getRepositoryLocation() + pathToResource;
            File artifact = new File( path );
            FileInputStream fis = new FileInputStream(artifact);
            return fis;
        } catch (Exception e) {
            logger.error("Exception when getting the test artifact with path ["+pathToResource+"]", e);
        }

        return null;
    }

	/**
     * Returns a reference to a test resource (TestSuite, TestCase or Scriptlet) with its identifier
     * @param resourceId Id of the resource
     * @return Reference to a file with given resourceId
     * @throws URISyntaxException
     */
    private File getTestResource(String testCaseId, String resourceId, boolean testCase) {
        //here we guarantee that the resource is an XML file
        String path;
        if (testCase) {
            path = configuration.getRepositoryLocation() + "test-cases/" + resourceId + ".xml";
        } else {
            path = configuration.getRepositoryLocation() + resourceId + ".xml";
        }
        File resource = new File( path );
        return resource;
    }

    /**
     * Checks if the test resource with given identifier exists
     * @param resourceId Id of the resource
     * @return Boolean indicating the existence of the resource
     */
    private boolean isTestResourceAvailable(String testCaseId, String resourceId, boolean testCase) {
        File resource = getTestResource(testCaseId, resourceId, testCase);
        return resource.exists();
    }

}
