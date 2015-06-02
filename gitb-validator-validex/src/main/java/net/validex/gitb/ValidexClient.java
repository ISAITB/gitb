package net.validex.gitb;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONUnmarshaller;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;
import net.validex.gitb.model.ReportResponse;
import net.validex.gitb.model.ValidateResponse;
import net.validex.gitb.model.ValidationRequest;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import javax.net.ssl.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Created by serbay.
 */
public class ValidexClient {

	private static final String API_URL = "https://api.validex.net";
	private static final String API_KEY = "78a92a585e186fd63d51f48a54880ad6";

	private static final String VALIDATE_METHOD = "/validate";
	private static final String REPORT_METHOD = "/report";

	private static ValidexClient instance;

	private ValidexClient() {
	}

	public ValidateResponse validate(String fileName, String content) throws IOException {
		Client client = Client.create(configureClient());

//        MultiPart multiPart = new MultiPart();
//        multiPart.bodyPart(fileName, MediaType.TEXT_PLAIN_TYPE);
//        multiPart.bodyPart(config, MediaType.APPLICATION_OCTET_STREAM_TYPE);
//        File tempFile = File.createTempFile(fileName + RandomStringUtils.random(5, true, true), null);
//        tempFile.deleteOnExit();
//        FileDataBodyPart fileData = new FileDataBodyPart(fileName, tempFile);

//        multiPart.bodyPart(fileData);

		/*FormDataMultiPart payload = new FormDataMultiPart()
			.field("filename", fileName)
			.field("fileContents", StringEscapeUtils.escapeXml(content));
        payload.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);*/

        ValidationRequest vr = new ValidationRequest(fileName, content);

		WebResource webResource = client.resource(API_URL);
		return webResource
			    .path(VALIDATE_METHOD)
    			.accept(MediaType.APPLICATION_JSON_TYPE)
                .type(MediaType.APPLICATION_JSON)
	    		.header(HttpHeaders.AUTHORIZATION, "apikey=\"" + API_KEY + "\"")
		    	.post(ValidateResponse.class, vr);
	}

	public ReportResponse getReport(String reportId) throws JAXBException {
        Client client = Client.create(configureClient());

		WebResource webResource = client.resource(API_URL)
                .path(REPORT_METHOD)
                .path(reportId);
		String response = webResource
			.accept(MediaType.APPLICATION_JSON_TYPE)
            .type(MediaType.APPLICATION_JSON_TYPE)
			.header(HttpHeaders.AUTHORIZATION, "apikey=\""+API_KEY+"\"")
			.get(String.class);

        JSONJAXBContext context = new JSONJAXBContext(ReportResponse.class);
        JSONUnmarshaller unmarshaller = context.createJSONUnmarshaller();
        ReportResponse report = unmarshaller.unmarshalFromJSON(new StringReader(response), ReportResponse.class);

        report.setLink(webResource.getURI().toString());

        return report;
	}

    public static ClientConfig configureClient() {
        TrustManager[ ] certs = new TrustManager[ ] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }
                }
        };
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, certs, new SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());

        ClientConfig config = new DefaultClientConfig();
        try {
            config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(
                    new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    },
                    ctx
            ));
        } catch(Exception e) {
        }
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        return config;
    }

	public static ValidexClient getInstance() {
		if(instance == null) {
			instance = new ValidexClient();
		}

		return instance;
	}
}
