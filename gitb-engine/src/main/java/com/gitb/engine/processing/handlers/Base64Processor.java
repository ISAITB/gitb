/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
import com.gitb.types.BinaryType;
import com.gitb.types.BooleanType;
import com.gitb.types.StringType;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;

import java.util.Arrays;
import java.util.Collections;

@ProcessingHandler(name="Base64Processor")
public class Base64Processor extends AbstractProcessingHandler {

    private static final Tika TIKA = new Tika();
    private static final String OPERATION_ENCODE = "encode";
    private static final String OPERATION_DECODE = "decode";
    private static final String INPUT_INPUT = "input";
    private static final String INPUT_DATA_URL = "dataUrl";
    private static final String OUTPUT_OUTPUT = "output";

    @Override
    protected ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("Base64Processor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION_ENCODE,
                Arrays.asList(
                        createParameter(INPUT_INPUT, "binary", UsageEnumeration.R, ConfigurationType.SIMPLE, "The bytes to encode."),
                        createParameter(INPUT_DATA_URL, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether or not to produce output as a data URL (default is false).")
                ),
                Collections.singletonList(
                        createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The Base-64 encoded string.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_DECODE,
                Collections.singletonList(
                        createParameter(INPUT_INPUT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The Base-64 encoded string (can also be provided as a data URL).")
                ),
                Collections.singletonList(
                        createParameter(OUTPUT_OUTPUT, "binary", UsageEnumeration.R, ConfigurationType.SIMPLE, "The decoded bytes.")
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
        if (OPERATION_ENCODE.equalsIgnoreCase(operation)) {
            byte[] binaryContent = getInputForName(input, INPUT_INPUT, BinaryType.class).serializeByDefaultEncoding();
            BooleanType asDataUrl = getInputForName(input, INPUT_DATA_URL, BooleanType.class);
            boolean dataUrl = (asDataUrl != null)? asDataUrl.getValue() :false;
            String outputValue = org.apache.commons.codec.binary.Base64.encodeBase64String(binaryContent);
            if (dataUrl) {
                String mimeType = getMimeType(binaryContent);
                outputValue = "data:"+mimeType+";base64," + outputValue;
            }
            data.getData().put(OUTPUT_OUTPUT, new StringType(outputValue));
        } else if (OPERATION_DECODE.equalsIgnoreCase(operation)) {
            String stringContent = getInputForName(input, INPUT_INPUT, StringType.class).toString();
            if (isDataURL(stringContent)) {
                stringContent = getBase64FromDataURL(stringContent);
            }
            BinaryType output = new BinaryType();
            output.setValue(org.apache.commons.codec.binary.Base64.decodeBase64(stringContent));
            data.getData().put(OUTPUT_OUTPUT, output);
        } else {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private String getMimeType(byte[] bytes) {
        return TIKA.detect(bytes);
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
