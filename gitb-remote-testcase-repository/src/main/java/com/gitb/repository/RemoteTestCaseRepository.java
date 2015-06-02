package com.gitb.repository;

import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import com.gitb.tdl.TestSuite;
import com.gitb.utils.XMLUtils;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 10/20/14.
 */
@MetaInfServices(ITestCaseRepository.class)
public class RemoteTestCaseRepository implements ITestCaseRepository {

	private static final Logger logger = LoggerFactory.getLogger(RemoteTestCaseRepository.class);

	// TODO fix the LRU cache capacity
	public static final int LRU_RESOURCE_CACHE_INITIAL_CAPACITY = 1;

	private Map<String, byte[]> testResourceCache;

	public RemoteTestCaseRepository() {
		testResourceCache = Collections.synchronizedMap(new LRUMap<String, byte[]>(LRU_RESOURCE_CACHE_INITIAL_CAPACITY));
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
	public boolean isScriptletAvailable(String scriptletId) {
		return getScriptlet(scriptletId) != null;
	}

	@Override
	public Scriptlet getScriptlet(String scriptletId) {
		return getXMLTestResource(Scriptlet.class, scriptletId);
	}

	@Override
	public boolean isTestSuiteAvailable(String testSuiteId) {
		 return getTestSuite(testSuiteId) != null;
	}

	@Override
	public TestSuite getTestSuite(String testSuiteId) {
		return getXMLTestResource(TestSuite.class, testSuiteId);
	}

	@Override
	public InputStream getTestArtifact(String path) {
		try {
			return getTestResource(path);
		} catch (Exception e) {
			throw new GITBEngineInternalError(e);
		}
	}

	@Override
	public boolean isTestArtifactAvailable(String path) {
		return getTestArtifact(path) != null;
	}

	private <T> T getXMLTestResource(Class<? extends T> clazz, String resourceId) {
		try {
			InputStream inputStream = getTestResource(resourceId + ".xml");

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

	private InputStream getTestResource(String path) throws IOException, EncoderException {
		URLCodec codec = new URLCodec();
		String uri = TestCaseRepositoryConfiguration.TEST_RESOURCE_REPOSITORY_URL.replace(":" + TestCaseRepositoryConfiguration.RESOURCE_ID_PARAMETER, codec.encode(path));

		return retrieveRemoteTestResource(uri);
	}

	private InputStream retrieveRemoteTestResource(String uri) throws IOException, EncoderException {
		if(testResourceCache.containsKey(uri)) {
			return new ByteArrayInputStream(testResourceCache.get(uri));
		} else {
			InputStream stream = null;

			CloseableHttpClient httpClient = HttpClients.createDefault();

			logger.debug("Requesting test resource definition ["+uri+"]");

			HttpGet request = new HttpGet(uri);
			CloseableHttpResponse httpResponse = httpClient.execute(request);
			try {
				if(httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					HttpEntity entity = httpResponse.getEntity();

					byte[] content = IOUtils.toByteArray(entity.getContent());

					testResourceCache.put(uri, content);

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
}
