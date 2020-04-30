package com.gitb.processing;

import com.gitb.core.*;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.BinaryType;
import com.gitb.types.BooleanType;
import com.gitb.types.StringType;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.kohsuke.MetaInfServices;

import java.util.Arrays;
import java.util.Collections;

@MetaInfServices(IProcessingHandler.class)
public class Base64Processor extends AbstractProcessingHandler {

    private Tika tika = new Tika();

    private static final String OPERATION__ENCODE = "encode";
    private static final String OPERATION__DECODE = "decode";

    private static final String INPUT__INPUT = "input";
    private static final String INPUT__DATA_URL = "dataUrl";
    private static final String OUTPUT__OUTPUT = "output";

    @Override
    public ProcessingModule getModuleDefinition() {
        ProcessingModule module = new ProcessingModule();
        module.setId("Base64Processor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION__ENCODE,
            Arrays.asList(
                createParameter(INPUT__INPUT, "binary", UsageEnumeration.R, ConfigurationType.SIMPLE, "The bytes to encode."),
                createParameter(INPUT__DATA_URL, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether or not to produce output as a data URL (default is false).")
            ),
            Collections.singletonList(
                createParameter(OUTPUT__OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The Base-64 encoded string.")
            )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION__DECODE,
            Collections.singletonList(
                createParameter(INPUT__INPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The Base-64 encoded string (can also be provided as a data URL).")
            ),
            Collections.singletonList(
                createParameter(OUTPUT__OUTPUT, "binary", UsageEnumeration.R, ConfigurationType.SIMPLE, "The decoded bytes.")
            )
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        if (StringUtils.isBlank(operation)) {
            throw new IllegalArgumentException("No operation provided");
        }
        ProcessingData data = new ProcessingData();
        if (OPERATION__ENCODE.equalsIgnoreCase(operation)) {
            byte[] binaryContent = getInputForName(input, INPUT__INPUT, BinaryType.class).serializeByDefaultEncoding();
            BooleanType asDataUrl = getInputForName(input, INPUT__DATA_URL, BooleanType.class);
            boolean dataUrl = (asDataUrl != null)?((Boolean)asDataUrl.getValue()):false;
            String outputValue = org.apache.commons.codec.binary.Base64.encodeBase64String(binaryContent);
            if (dataUrl) {
                String mimeType = getMimeType(binaryContent);
                outputValue = "data:"+mimeType+";base64," + outputValue;
            }
            data.getData().put(OUTPUT__OUTPUT, new StringType(outputValue));
        } else if (OPERATION__DECODE.equalsIgnoreCase(operation)) {
            String stringContent = getInputForName(input, INPUT__INPUT, StringType.class).toString();
            if (isDataURL(stringContent)) {
                stringContent = getBase64FromDataURL(stringContent);
            }
            BinaryType output = new BinaryType();
            output.setValue(org.apache.commons.codec.binary.Base64.decodeBase64(stringContent));
            data.getData().put(OUTPUT__OUTPUT, output);
        } else {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private String getMimeType(byte[] bytes) {
        return tika.detect(bytes);
    }

    private boolean isDataURL(String value) {
        return value != null && value.startsWith("data:") && value.contains(";base64,");
    }

    private String getBase64FromDataURL(String dataURL) {
        String result = null;
        if (dataURL != null) {
            result = dataURL.substring(dataURL.indexOf(",")+1);
        }
        return result;
    }

}
