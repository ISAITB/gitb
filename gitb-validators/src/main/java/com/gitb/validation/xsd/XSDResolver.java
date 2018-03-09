package com.gitb.validation.xsd;

import com.gitb.ModuleManager;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.repository.ITestCaseRepository;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import java.io.*;

/**
 * Created by senan on 10/10/14.
 */
public class XSDResolver implements LSResourceResolver {

    private static final String PROTOCOL = "file:///";

    /**
     * Path to the folder that contains root resource (i.e. and XSD schema or Schematron file, etc)
     */
    private String resource;
    private String testCaseId;

    public XSDResolver(String testCaseId, String path) {
        this.resource = path;
        this.testCaseId = testCaseId;
    }

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        ModuleManager moduleManager = ModuleManager.getInstance();
        ITestCaseRepository repository = moduleManager.getTestCaseRepository();

        String parentFolder;
        String artifactPath;

        if(baseURI == null) {
            parentFolder = resource.substring(0, resource.lastIndexOf("/")+1);
        } else {
            parentFolder = baseURI.substring(PROTOCOL.length(), baseURI.lastIndexOf("/")+1);
        }

        artifactPath = parentFolder + systemId;

        InputStream resource  = repository.getTestArtifact(testCaseId, artifactPath);
        if(resource != null) {
            // the "/" prevents the system to add the path where mvn command executed.
            return new Input(publicId, systemId, "/" + artifactPath, resource);
        }

	    return null;
    }

    public class Input implements LSInput {

        private String publicId;

        private String systemId;

        private String baseURI;

        public String getPublicId() {
            return publicId;
        }

        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        public String getBaseURI() {
            return baseURI;
        }

        public InputStream getByteStream() {
            return null;
        }

        public boolean getCertifiedText() {
            return false;
        }

        public Reader getCharacterStream() {
            return null;
        }

        public String getEncoding() {
            return null;
        }

        public String getStringData() {
            synchronized (inputStream) {
                try {
                    byte[] input = new byte[inputStream.available()];
                    inputStream.read(input);
                    String contents = new String(input);
                    return contents;
                } catch (IOException e) {
                    throw new GITBEngineInternalError(e);
                }
            }
        }

        public void setBaseURI(String baseURI) {
            this.baseURI = baseURI;
        }

        public void setByteStream(InputStream byteStream) {
        }

        public void setCertifiedText(boolean certifiedText) {
        }

        public void setCharacterStream(Reader characterStream) {
        }

        public void setEncoding(String encoding) {
        }

        public void setStringData(String stringData) {
        }

        public String getSystemId() {
            return systemId;
        }

        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        public BufferedInputStream getInputStream() {
            return inputStream;
        }

        public void setInputStream(BufferedInputStream inputStream) {
            this.inputStream = inputStream;
        }

        private BufferedInputStream inputStream;

        public Input(String publicId, String sysId, String baseUri, InputStream input) {
            this.publicId = publicId;
            this.systemId = sysId;
            this.baseURI  = baseUri;
            this.inputStream = new BufferedInputStream(input);
        }
    }
}
