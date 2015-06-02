package com.gitb.messaging;

import com.gitb.core.Configuration;
import com.gitb.messaging.layer.application.http.HttpMessagingHandler;
import com.gitb.types.BinaryType;
import com.gitb.types.StringType;
import com.gitb.utils.ConfigurationUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Created by serbay on 9/25/14.
 */
public class HttpMessagingHandlerTest extends MessagingHandlerTest {

	private static Message message;

	@BeforeClass
	public static void init() {
		receiverHandler = new HttpMessagingHandler();
		senderHandler = new HttpMessagingHandler();
        listenerHandler = new HttpMessagingHandler();
	}

	@Test
	public void handlersShouldNotBeNull() throws IOException {
		assertNotNull(receiverHandler);
		assertNotNull(senderHandler);

		message = new Message();
		message.getFragments().put(HttpMessagingHandler.HTTP_PATH_FIELD_NAME, new StringType("/test"));

		StringEntity stringEntity = new StringEntity("test123");
		byte[] content = EntityUtils.toByteArray(stringEntity);
		BinaryType data = new BinaryType();
		data.setValue(content);
		message.getFragments().put(HttpMessagingHandler.HTTP_BODY_FIELD_NAME, data);
	}

	@Override
	protected void messageReceived(Message message) {
		BinaryType data = (BinaryType) message.getFragments().get(HttpMessagingHandler.HTTP_BODY_FIELD_NAME);
		byte[] content = (byte[]) data.getValue();

		assertEquals(new String(content), "test123");
	}

	@Override
	protected Message constructMessage() {
		return message;
	}

	@Override
	protected List<Configuration> sendConfigurations() {
		List<Configuration> configurations = new ArrayList<>();
		configurations.add(ConfigurationUtils.constructConfiguration(HttpMessagingHandler.HTTP_METHOD_CONFIG_NAME, "POST"));

		return configurations;
	}

	@Override
	protected List<Configuration> receiveConfigurations() {
		return new ArrayList<>();
	}

	/*@Override
	public Thread getTesterThread(final String host, final int port) {
		return new Thread(new Runnable() {
			@Override
			public void run() {

				try {
					CloseableHttpClient httpclient = HttpClients.createDefault();

					HttpPost httpPost = new HttpPost("http://"+host+":"+port+"/login");
					List <NameValuePair> nvps = new ArrayList <NameValuePair>();
					nvps.add(new BasicNameValuePair("username", "vip"));
					nvps.add(new BasicNameValuePair("password", "secret"));
					httpPost.setEntity(new UrlEncodedFormEntity(nvps));
					CloseableHttpResponse response2 = httpclient.execute(httpPost);

					System.out.println(response2.getStatusLine());
					HttpEntity entity2 = response2.getEntity();
					// do something useful with the response body
					// and ensure it is fully consumed
					EntityUtils.consume(entity2);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}*/
}
