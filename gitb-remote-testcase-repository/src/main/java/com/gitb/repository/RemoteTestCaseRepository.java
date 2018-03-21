package com.gitb.repository;

import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import com.gitb.utils.XMLUtils;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by serbay on 10/20/14.
 */
@MetaInfServices(ITestCaseRepository.class)
public class RemoteTestCaseRepository implements ITestCaseRepository {

	private static final Logger logger = LoggerFactory.getLogger(RemoteTestCaseRepository.class);

	public RemoteTestCaseRepository() {
	}

	@Override
	public String getName() {
		return "remote-repository";
	}

	@Override
	public boolean isTestCaseAvailable(String testCaseId) {
		return getTestCase(testCaseId) != null;
	}

	@Override
	public TestCase getTestCase(String testCaseId) {
		return getTestCaseResource(testCaseId);
	}

	@Override
	public boolean isScriptletAvailable(String testCaseId, String scriptletId) {
		return getScriptlet(testCaseId, scriptletId) != null;
	}

	@Override
	public Scriptlet getScriptlet(String testCaseId, String scriptletId) {
		return getXMLTestResource(testCaseId, Scriptlet.class, scriptletId);
	}

	@Override
	public InputStream getTestArtifact(String testCaseId, String path) {
		try {
			return getTestResource(testCaseId, path);
		} catch (Exception e) {
			throw new GITBEngineInternalError(e);
		}
	}

	@Override
	public boolean isTestArtifactAvailable(String testCaseId, String path) {
		return getTestArtifact(testCaseId, path) != null;
	}

	private <T> T getXMLTestResource(String testCaseId, Class<? extends T> clazz, String resourceId) {
		try {
			InputStream inputStream = getTestResource(testCaseId,resourceId + ".xml");

			if (inputStream != null) {
				T resource = XMLUtils.unmarshal(clazz, new StreamSource(inputStream));

				return resource;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new GITBEngineInternalError(e);
		}
	}

	private TestCase getTestCaseResource(String testCaseId) {
		try {
			URLCodec codec = new URLCodec();
			String uri = TestCaseRepositoryConfiguration.TEST_CASE_REPOSITORY_URL.replace(":" + TestCaseRepositoryConfiguration.TEST_ID_PARAMETER, codec.encode(testCaseId));

			InputStream inputStream = retrieveRemoteTestResource(uri);

			if (inputStream != null) {
				TestCase resource = XMLUtils.unmarshal(TestCase.class, new StreamSource(inputStream));

				return resource;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new GITBEngineInternalError(e);
		}
	}

	private InputStream getTestResource(String testId, String path) throws IOException, EncoderException {
		URLCodec codec = new URLCodec();
		String uri = TestCaseRepositoryConfiguration.TEST_RESOURCE_REPOSITORY_URL
				.replace(":" + TestCaseRepositoryConfiguration.TEST_ID_PARAMETER, codec.encode(testId))
				.replace(":" + TestCaseRepositoryConfiguration.RESOURCE_ID_PARAMETER, codec.encode(path));

		return retrieveRemoteTestResource(uri);
	}

	private InputStream retrieveRemoteTestResource(String uri) throws IOException, EncoderException {
		InputStream stream = null;

		CloseableHttpClient httpClient = HttpClients.createDefault();

		logger.debug("Requesting test resource definition ["+uri+"]");

		HttpGet request = new HttpGet(uri);
		CloseableHttpResponse httpResponse = httpClient.execute(request);
		try {
			if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity entity = httpResponse.getEntity();
				byte[] content = IOUtils.toByteArray(entity.getContent());
				stream = new ByteArrayInputStream(content);
			}
		} catch (Exception e) {
			logger.debug("Test case definition retrieval was failed", e);
			throw new GITBEngineInternalError(e);
		} finally {
			httpResponse.close();
		}

		return stream;

	}
}
