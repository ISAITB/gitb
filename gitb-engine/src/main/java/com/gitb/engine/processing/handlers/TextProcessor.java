/*
 * Copyright (C) 2026 European Union
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
import com.gitb.core.TypedParameter;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.BinaryType;
import com.gitb.types.BooleanType;
import com.gitb.types.StringType;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Processing handler offering text encoding and decoding operations (XML, JSON and HTML escaping, URI and
 * form percent-encoding, CSV field escaping, regular expression literal quoting, and hex encoding) that are not
 * otherwise covered by the built-in XPath function library or by another processing handler.
 */
@ProcessingHandler(name="TextProcessor")
public class TextProcessor extends AbstractProcessingHandler {

    private static final String INPUT_INPUT = "input";
    private static final String INPUT_QUOTED = "quoted";
    private static final String OUTPUT_OUTPUT = "output";

    private enum Operation {

        XML_ESCAPE("xmlEscape", "string", "string", "Escape a text so that it can be safely inserted as content into an XML 1.0 document."),
        XML_UNESCAPE("xmlUnescape", "string", "string", "Reverse XML escaping, restoring the original text."),
        JSON_ESCAPE("jsonEscape", "string", "string", "Escape a text so that it can be safely used as (part of) JSON content."),
        JSON_UNESCAPE("jsonUnescape", "string", "string", "Reverse JSON escaping, restoring the original text."),
        HTML_ESCAPE("htmlEscape", "string", "string", "Escape a text using HTML 4 entities."),
        HTML_UNESCAPE("htmlUnescape", "string", "string", "Reverse HTML entity escaping, restoring the original text."),
        URI_ENCODE("uriEncode", "string", "string", "Percent-encode a text for use as a URI component, as defined by RFC 3986."),
        URI_DECODE("uriDecode", "string", "string", "Reverse RFC 3986 percent-encoding, restoring the original text."),
        FORM_ENCODE("formEncode", "string", "string", "Encode a text as \"application/x-www-form-urlencoded\" content (e.g. for a query string or form body)."),
        FORM_DECODE("formDecode", "string", "string", "Reverse \"application/x-www-form-urlencoded\" encoding, restoring the original text."),
        CSV_ESCAPE("csvEscape", "string", "string", "Escape a text so that it can be safely used as a single CSV field."),
        CSV_UNESCAPE("csvUnescape", "string", "string", "Reverse CSV field escaping, restoring the original text."),
        REGEX_ESCAPE("regexEscape", "string", "string", "Quote a text so that it is matched literally when used as (part of) a Java regular expression."),
        HEX_ENCODE("hexEncode", "binary", "string", "Encode binary content as a hexadecimal string."),
        HEX_DECODE("hexDecode", "string", "binary", "Decode a hexadecimal string to its binary content.");

        private static final Map<String, Operation> BY_NAME = new HashMap<>();
        static {
            for (Operation value: values()) {
                BY_NAME.put(value.operationName.toLowerCase(Locale.ROOT), value);
            }
        }

        private final String operationName;
        private final String inputType;
        private final String outputType;
        private final String outputDescription;

        Operation(String operationName, String inputType, String outputType, String outputDescription) {
            this.operationName = operationName;
            this.inputType = inputType;
            this.outputType = outputType;
            this.outputDescription = outputDescription;
        }

        static Operation forName(String name) {
            return BY_NAME.get(name == null ? null : name.toLowerCase(Locale.ROOT));
        }

    }

    @Override
    protected ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("TextProcessor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        for (Operation op: Operation.values()) {
            List<TypedParameter> inputs;
            if (op == Operation.JSON_ESCAPE || op == Operation.JSON_UNESCAPE) {
                inputs = List.of(
                        createParameter(INPUT_INPUT, op.inputType, UsageEnumeration.R, ConfigurationType.SIMPLE, "The text to process."),
                        createParameter(INPUT_QUOTED, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether the value is (for jsonEscape) or should be (for jsonUnescape) wrapped in double quotes as a complete JSON string literal (default is false).")
                );
            } else {
                inputs = List.of(
                        createParameter(INPUT_INPUT, op.inputType, UsageEnumeration.R, ConfigurationType.SIMPLE, "The text to process.")
                );
            }
            module.getOperation().add(createProcessingOperation(op.operationName,
                    inputs,
                    List.of(createParameter(OUTPUT_OUTPUT, op.outputType, UsageEnumeration.R, ConfigurationType.SIMPLE, op.outputDescription))
            ));
        }
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        if (StringUtils.isBlank(operation)) {
            throw new IllegalArgumentException("No operation provided");
        }
        Operation op = Operation.forName(operation);
        if (op == null) {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        ProcessingData data = new ProcessingData();
        switch (op) {
            case XML_ESCAPE -> data.getData().put(OUTPUT_OUTPUT, new StringType(StringEscapeUtils.escapeXml10(textInput(input))));
            case XML_UNESCAPE -> data.getData().put(OUTPUT_OUTPUT, new StringType(StringEscapeUtils.unescapeXml(textInput(input))));
            case JSON_ESCAPE -> data.getData().put(OUTPUT_OUTPUT, new StringType(jsonEscape(input)));
            case JSON_UNESCAPE -> data.getData().put(OUTPUT_OUTPUT, new StringType(jsonUnescape(input)));
            case HTML_ESCAPE -> data.getData().put(OUTPUT_OUTPUT, new StringType(StringEscapeUtils.escapeHtml4(textInput(input))));
            case HTML_UNESCAPE -> data.getData().put(OUTPUT_OUTPUT, new StringType(StringEscapeUtils.unescapeHtml4(textInput(input))));
            case URI_ENCODE -> data.getData().put(OUTPUT_OUTPUT, new StringType(uriEncode(textInput(input))));
            case URI_DECODE -> data.getData().put(OUTPUT_OUTPUT, new StringType(uriDecode(textInput(input))));
            case FORM_ENCODE -> data.getData().put(OUTPUT_OUTPUT, new StringType(URLEncoder.encode(textInput(input), StandardCharsets.UTF_8)));
            case FORM_DECODE -> data.getData().put(OUTPUT_OUTPUT, new StringType(URLDecoder.decode(textInput(input), StandardCharsets.UTF_8)));
            case CSV_ESCAPE -> data.getData().put(OUTPUT_OUTPUT, new StringType(StringEscapeUtils.escapeCsv(textInput(input))));
            case CSV_UNESCAPE -> data.getData().put(OUTPUT_OUTPUT, new StringType(StringEscapeUtils.unescapeCsv(textInput(input))));
            case REGEX_ESCAPE -> data.getData().put(OUTPUT_OUTPUT, new StringType(Pattern.quote(textInput(input))));
            case HEX_ENCODE -> data.getData().put(OUTPUT_OUTPUT, new StringType(Hex.encodeHexString(binaryInput(input))));
            case HEX_DECODE -> data.getData().put(OUTPUT_OUTPUT, hexDecode(input));
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private String textInput(ProcessingData input) {
        return getRequiredInputForName(input, INPUT_INPUT, StringType.class).getValue();
    }

    private byte[] binaryInput(ProcessingData input) {
        return getRequiredInputForName(input, INPUT_INPUT, BinaryType.class).serializeByDefaultEncoding();
    }

    private boolean quotedInput(ProcessingData input) {
        return Optional.ofNullable(getInputForName(input, INPUT_QUOTED, BooleanType.class)).map(BooleanType::getValue).orElse(false);
    }

    private String jsonEscape(ProcessingData input) {
        String escaped = StringEscapeUtils.escapeJson(textInput(input));
        return quotedInput(input) ? "\"" + escaped + "\"" : escaped;
    }

    private String jsonUnescape(ProcessingData input) {
        String text = textInput(input);
        if (quotedInput(input)) {
            if (text.length() < 2 || !text.startsWith("\"") || !text.endsWith("\"")) {
                throw new IllegalArgumentException("The provided input was expected to be a quoted JSON string literal");
            }
            text = text.substring(1, text.length() - 1);
        }
        return StringEscapeUtils.unescapeJson(text);
    }

    /**
     * Percent-encodes the provided text as a URI component, matching RFC 3986 (and XPath's fn:encode-for-uri)
     * semantics. URLEncoder implements the older application/x-www-form-urlencoded rules, so its output is
     * adjusted here: a space is encoded to "%20" rather than "+" (a literal '+' is already encoded to "%2B" by
     * URLEncoder so this is unambiguous), '*' is percent-encoded (it is not RFC 3986 "unreserved"), and '~' is
     * left unescaped (it is RFC 3986 "unreserved" but URLEncoder escapes it).
     */
    private String uriEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }

    private String uriDecode(String value) {
        // A literal '+' is percent-encoded by uriEncode, so any remaining '+' came from a caller mixing in
        // form-encoded content. Escape it before delegating so URLDecoder does not turn it into a space.
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private BinaryType hexDecode(ProcessingData input) {
        try {
            BinaryType output = new BinaryType();
            output.setValue(Hex.decodeHex(textInput(input)));
            return output;
        } catch (DecoderException e) {
            throw new IllegalArgumentException("The provided input is not a valid hexadecimal string", e);
        }
    }

}
