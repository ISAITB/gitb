package com.gitb.engine.processing.handlers;

import com.gitb.core.ConfigurationParameters;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.StringType;
import com.gitb.utils.XMLUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Objects;

@ProcessingHandler(name="XSLTProcessor")
public class XSLTProcessor extends AbstractProcessingHandler {

    private static final String OPERATION__PROCESS = "process";
    private static final String INPUT__XML = "xml";
    private static final String INPUT__XSLT = "xslt";
    private static final String OUTPUT__OUTPUT = "output";

    @Override
    public ProcessingModule getModuleDefinition() {
        ProcessingModule module = new ProcessingModule();
        module.setId("XSLTProcessor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION__PROCESS,
                List.of(
                        createParameter(INPUT__XML, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The XML content to transform."),
                        createParameter(INPUT__XSLT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The XSLT template to use for the transformation.")
                ),
                List.of(createParameter(OUTPUT__OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The transformation output."))
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        var xmlContent = Objects.requireNonNull(getInputForName(input, INPUT__XML, StringType.class), "You need to provide an input named ["+INPUT__XML+"] with the XML content to transform.");
        var xsltContent = Objects.requireNonNull(getInputForName(input, INPUT__XSLT, StringType.class), "You need to provide an input named ["+INPUT__XSLT+"] with the XSLT to use for the transformation.");
        StringType outputValue;
        try {
            var transformer = XMLUtils.getSecureTransformerFactory().newTransformer(toSource(xsltContent));
            var output = new StringWriter();
            transformer.transform(toSource(xmlContent), new StreamResult(output));
            outputValue = new StringType(output.toString());
        } catch (TransformerConfigurationException e) {
            throw new IllegalArgumentException("An error occurred while preparing the XSLT transformation", e);
        } catch (TransformerException e) {
            throw new IllegalArgumentException("An error occurred during the XSLT transformation", e);
        }
        var data = new ProcessingData();
        data.getData().put(OUTPUT__OUTPUT, outputValue);
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private Source toSource(StringType data) {
        try {
            return new StAXSource(XMLUtils.getSecureXMLInputFactory().createXMLStreamReader(new StringReader((String)data.getValue())));
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Unable to initialise XML stream reader", e);
        }
    }
}
