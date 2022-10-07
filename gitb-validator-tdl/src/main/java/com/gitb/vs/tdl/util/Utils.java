package com.gitb.vs.tdl.util;

import com.gitb.tdl.Process;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.Context;
import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {

    public static final String DOMAIN_MAP = "DOMAIN";
    public static final String ORGANISATION_MAP = "ORGANISATION";
    public static final String ORGANISATION_MAP__SHORT_NAME = "shortName";
    public static final String ORGANISATION_MAP__FULL_NAME = "fullName";
    public static final String SYSTEM_MAP = "SYSTEM";
    public static final String SYSTEM_MAP__SHORT_NAME = "shortName";
    public static final String SYSTEM_MAP__FULL_NAME = "fullName";
    public static final String SYSTEM_MAP__VERSION = "version";
    public static final String SESSION_MAP = "SESSION";
    public static final String STEP_SUCCESS = "STEP_SUCCESS";
    public static final String STEP_STATUS = "STEP_STATUS";
    public static final String TEST_SUCCESS = "TEST_SUCCESS";
    public static final String VARIABLE_EXPRESSION = "\\$([a-zA-Z][a-zA-Z\\-_0-9]*)(?:\\{(?:[\\$\\{\\}a-zA-Z\\-\\._0-9]*)\\})*";
    public static final Pattern VARIABLE_EXPRESSION_PATTERN = Pattern.compile(VARIABLE_EXPRESSION);

    public static boolean unzip(InputStream stream, File targetFolder) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(stream);
        ZipEntry zipEntry = zis.getNextEntry();
        boolean ok = false;
        while (zipEntry != null) {
            ok = true;
            File newFile = newFile(targetFolder, zipEntry);
            if (!zipEntry.isDirectory()) {
                newFile.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        return ok;
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IllegalStateException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    public static DocumentBuilderFactory getSecureDocumentBuilderFactory() throws ParserConfigurationException {
        // Use Xerces implementation for its advanced security features.
        DocumentBuilderFactoryImpl docBuilderFactory = new DocumentBuilderFactoryImpl();
        docBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        docBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        docBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        docBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        docBuilderFactory.setXIncludeAware(false);
        docBuilderFactory.setExpandEntityReferences(false);
        return docBuilderFactory;
    }

    public static Document readAsXML(InputStream is) {
        Document document = null;
        DocumentBuilderFactory dbFactory = null;
        try {
            dbFactory = getSecureDocumentBuilderFactory();
            dbFactory.setNamespaceAware(true);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
        DocumentBuilder builder;
        try {
            builder = dbFactory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) {
                }
                @Override
                public void error(SAXParseException exception) {
                }
                @Override
                public void fatalError(SAXParseException exception) {
                }
            });
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
        try {
            document = builder.parse(is);
        } catch (Exception e) {
            // Treat as non-XML.
        }
        return document;
    }

    public static <T> JAXBElement<T> unmarshal(InputStream is, Class<T> rootElement, JAXBContext jaxbContext, Schema schema, ValidationEventHandler eventHandler) throws JAXBException {
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        if (schema != null) {
            unmarshaller.setSchema(schema);
        }
        if (eventHandler != null) {
            unmarshaller.setEventHandler(eventHandler);
        }
        // Use a factory that disables XML External Entity (XXE) attacks.
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLStreamReader xsr;
        try {
            xsr = xif.createXMLStreamReader(new StreamSource(is));
        } catch (XMLStreamException e) {
            throw new IllegalStateException(e);
        }
        return unmarshaller.unmarshal(xsr, rootElement);
    }

    public static boolean isVariableExpression(String expression){
        return StringUtils.isNotBlank(expression) && VARIABLE_EXPRESSION_PATTERN.matcher(expression).matches();
    }

    public static boolean isURL(String handler) {
        try {
            new URI(handler).toURL();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static ContainerTypeInfo getContainerTypeParts(String dataType) {
        String containerTypePart;
        String containedTypePart = null;
        int containerStart = dataType.indexOf('[');
        if (containerStart != -1) {
            containerTypePart = dataType.substring(0, containerStart);
            int containerEnd = dataType.indexOf(']');
            if (containerEnd != -1 && containerEnd > containerStart && dataType.length() > containerStart+1) {
                containedTypePart = dataType.substring(containerStart+1, containerEnd);
            }
        } else {
            containerTypePart = dataType;
        }
        return new ContainerTypeInfo(containerTypePart, containedTypePart);
    }

    public static boolean isContainerType(String dataType, Set<String> containerDataTypes, Set<String> containedDataTypes) {
        boolean isContainer = containerDataTypes.contains(dataType);
        if (!isContainer) {
            ContainerTypeInfo typeInfo = getContainerTypeParts(dataType);
            if (typeInfo.getContainerType() != null && typeInfo.getContainedType() != null) {
                isContainer = containedDataTypes.contains(typeInfo.getContainedType()) && containerDataTypes.contains(typeInfo.getContainerType());
            }
        }
        return isContainer;
    }

    private static String getStepName(Object stepObj) {
        String step;
        if (stepObj instanceof Send) {
            step = "send";
        } else if (stepObj instanceof Receive) {
            step = "receive";
        } else if (stepObj instanceof Listen) {
            step = "listen";
        } else if (stepObj instanceof Process) {
            step = "process";
        } else if (stepObj instanceof Verify) {
            step = "verify";
        } else if (stepObj instanceof IfStep) {
            step = "if";
        } else if (stepObj instanceof WhileStep) {
            step = "while";
        } else if (stepObj instanceof RepeatUntilStep) {
            step = "repuntil";
        } else if (stepObj instanceof ForEachStep) {
            step = "foreach";
        } else if (stepObj instanceof FlowStep) {
            step = "flow";
        } else if (stepObj instanceof BeginTransaction) {
            step = "btxn";
        } else if (stepObj instanceof EndTransaction) {
            step = "etxn";
        } else if (stepObj instanceof BeginProcessingTransaction) {
            step = "bptxn";
        } else if (stepObj instanceof EndProcessingTransaction) {
            step = "eptxn";
        } else if (stepObj instanceof ExitStep) {
            step = "exit";
        } else if (stepObj instanceof Assign) {
            step = "assign";
        } else if (stepObj instanceof Log) {
            step = "log";
        } else if (stepObj instanceof Group) {
            step = "group";
        } else if (stepObj instanceof CallStep) {
            step = "call";
        } else if (stepObj instanceof UserInteraction) {
            step = "interact";
        } else if (stepObj instanceof TestArtifact) {
            step = "imports";
        } else if (stepObj instanceof Output) {
            step = "output";
        } else if (stepObj instanceof Variable) {
            step = "variables";
        } else if (stepObj instanceof ScriptletOutputsMarker) {
            step = "outputs";
        } else {
            step = "";
        }
        return step;
    }

    public static String stepNameWithScriptlet(Object currentStep, Scriptlet currentScriptlet) {
        if (currentScriptlet == null) {
            return Utils.getStepName(currentStep);
        } else {
            return String.format("%s of scriptlet [%s]", Utils.getStepName(currentStep), currentScriptlet.getId());
        }
    }

    public static String getScriptletLocation(Path scriptletPath, Context context) {
        return context.getTestSuiteRootPath().relativize(scriptletPath).toString();
    }

    public static String getTestCaseLocation(String testCaseId, Context context) {
        List<Path> paths = context.getTestCasePaths().get(testCaseId);
        if (paths.size() == 1) {
            return context.getTestSuiteRootPath().relativize(paths.get(0)).toString();
        }
        return "";
    }

    public static String standardisePath(String path) {
        if (path != null && !path.isBlank()) {
            path = StringUtils.replace(path, "\\", "/");
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
        } else {
            path = null;
        }
        return path;
    }

    public static class ScriptletOutputsMarker {}
}
