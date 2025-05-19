package com.gitb.engine.repository;

import com.gitb.engine.TestEngineConfiguration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import com.gitb.utils.HmacUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by serbay on 10/20/14.
 */
public class RemoteTestCaseRepository implements ITestCaseRepository {

	private static final Logger logger = LoggerFactory.getLogger(RemoteTestCaseRepository.class);

	public RemoteTestCaseRepository() {
	}

	@Override
	public String getName() {
		return "remote-repository";
	}

	@Override
	public TestCase getTestCase(String testCaseId) {
		return getTestCaseResource(testCaseId);
	}

	@Override
	public Scriptlet getScriptlet(String from, String testCaseId, String scriptletPath) {
		return getXMLTestResource(from, testCaseId, Scriptlet.class, scriptletPath);
	}

	@Override
	public InputStream getTestArtifact(String from, String testCaseId, String artifactPath) {
		try {
			return getTestResource(toLocationKey(from, testCaseId), artifactPath);
		} catch (Exception e) {
			throw new GITBEngineInternalError(e);
		}
	}

	private String toLocationKey(String from, String testCaseId) {
		String locationKey = testCaseId;
		if (from != null) {
			locationKey = from + "|" + testCaseId;
		}
		return locationKey;
	}

	private <T> T getXMLTestResource(String from, String testCaseId, Class<? extends T> clazz, String resourcePath) {
		try {
			InputStream inputStream = getTestResource(toLocationKey(from, testCaseId), resourcePath);
			if (inputStream != null) {
				return XMLUtils.unmarshal(clazz, new StreamSource(inputStream));
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
			String uri = TestEngineConfiguration.TEST_CASE_REPOSITORY_URL.replace(":" + TestEngineConfiguration.TEST_ID_PARAMETER, codec.encode(testCaseId));

			InputStream inputStream = retrieveRemoteTestResource(testCaseId, uri);

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

	private InputStream getTestResource(String locationKey, String path) throws IOException, EncoderException {
		URLCodec codec = new URLCodec();
		String uri = TestEngineConfiguration.TEST_RESOURCE_REPOSITORY_URL
				.replace(":" + TestEngineConfiguration.TEST_ID_PARAMETER, codec.encode(locationKey))
				.replace(":" + TestEngineConfiguration.RESOURCE_ID_PARAMETER, codec.encode(path));

		return retrieveRemoteTestResource(locationKey, uri);
	}

	private InputStream retrieveRemoteTestResource(String resourceId, String uri) throws IOException {
		InputStream stream = null;

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			logger.debug("Requesting test resource definition ["+uri+"]");
			HttpGet request = new HttpGet(uri);
			HmacUtils.TokenData tokenData = HmacUtils.getTokenData(resourceId);
			request.addHeader(HmacUtils.HMAC_HEADER_TOKEN, tokenData.getTokenValue());
			request.addHeader(HmacUtils.HMAC_HEADER_TIMESTAMP, tokenData.getTokenTimestamp());
			try (CloseableHttpResponse httpResponse = httpClient.execute(request)) {
				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					HttpEntity entity = httpResponse.getEntity();
					byte[] content = IOUtils.toByteArray(entity.getContent());
					stream = new ByteArrayInputStream(content);
				}
			} catch (Exception e) {
				logger.debug("Test case definition retrieval failed", e);
				throw new GITBEngineInternalError(e);
			}
		}
		return stream;

	}
}
