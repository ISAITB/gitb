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

package utils;

import com.gitb.core.StepStatus;
import com.gitb.core.TestCaseType;
import com.gitb.tbs.ConfigurationCompleteRequest;
import com.gitb.tbs.InputRequest;
import com.gitb.tbs.InteractWithUsersRequest;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tpl.*;
import com.gitb.tr.*;
import config.Configurations;
import jakarta.xml.bind.JAXBElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.Version;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * A Jackson wrapper for converting JAVA objects into JSON and vice versa
 */
public class JacksonUtil {

    private static final ObjectMapper mapper;
    private static final Logger LOG = LoggerFactory.getLogger(JacksonUtil.class);

    static {
        SimpleModule customSerializersModule = new SimpleModule("CustomSerializersModule", new Version(1, 0, 0, null, null, null));
        customSerializersModule.addSerializer(StepStatus.class, new StatusSerializer());
        customSerializersModule.addSerializer(TestAssertionGroupReportsType.class, new TestAssertionGroupReportsTypeSerializer());
        customSerializersModule.addSerializer(TestStepReportType.class, new TestStepReportTypeSerializer());
        customSerializersModule.addSerializer(TestStep.class, new TestStepPresentationSerializer());
        customSerializersModule.addSerializer(InteractWithUsersRequest.class, new InteractWithUsersRequestSerializer());
        customSerializersModule.addSerializer(TestCaseType.class, new TestCaseTypeSerializer());
        customSerializersModule.addSerializer(Preliminary.class, new PreliminarySerializer());
        mapper = JsonMapper.builder()
                .addModule(new JakartaXmlBindAnnotationModule())
                .addModule(customSerializersModule)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .build();
    }

    private static ObjectWriter getWriter() {
        // Jackson's ObjectWriter.with(DateFormat) requires a java.text.DateFormat (it clones it per
        // serialization, so a fresh, unshared instance built here is safe) - this is the one place a
        // SimpleDateFormat is still built directly rather than going through the cached
        // Configurations.DATE_TIME_FORMATTER (a java.time.format.DateTimeFormatter).
        SimpleDateFormat dateFormat = new SimpleDateFormat(Configurations.DATE_FORMAT_DATETIME());
        dateFormat.setTimeZone(TimeZone.getTimeZone(Configurations.TIME_ZONE()));
        return mapper.writer(dateFormat);
    }

    public static String serializeTestCasePresentation(TestCase testCase) {
        return getWriter().writeValueAsString(testCase);
    }

    public static String serializeConfigurationCompleteRequest(ConfigurationCompleteRequest request) {
        // We are calling getConfigs() to ensure that the configurations' array is not null. The goal is to include an empty array in the JSON to detect the type of payload.
        request.getConfigs();
        return getWriter().writeValueAsString(request);
    }

    public static String serializeTestStepStatus(TestStepStatus testStepStatus) {
        return getWriter().writeValueAsString(testStepStatus);
    }

    public static String serializeTestReport(TestStepReportType report) {
        return getWriter().writeValueAsString(report);
    }


    public static String serializeInteractionRequest (InteractWithUsersRequest request) {
        return getWriter().writeValueAsString(request);
    }

    private static class StatusSerializer extends ValueSerializer<StepStatus> {
        @Override
        public void serialize(StepStatus status, JsonGenerator json, SerializationContext SerializationContext) {
            json.writeNumber(status.ordinal());
        }
    }

    private static class TestCaseTypeSerializer extends ValueSerializer<TestCaseType> {
        @Override
        public void serialize(TestCaseType value, JsonGenerator json, SerializationContext provider) {
            json.writeNumber(value.ordinal());
        }
    }

    private static class PreliminarySerializer extends ValueSerializer<Preliminary> {

        @Override
        public void serialize(Preliminary value, JsonGenerator json, SerializationContext provider) {
            json.writeStartObject();
            json.writeArrayPropertyStart("interactions");
            for (InstructionOrRequest item: value.getInstructOrRequest()) {
                json.writeStartObject();
                if (item instanceof Instruction) {
                    json.writeStringProperty("type", "instruction");
                } else {
                    json.writeStringProperty("type", "request");
                }
                json.writeStringProperty("desc", item.getDesc());
                json.writeStringProperty("with", item.getWith());
                json.writeStringProperty("id", item.getId());
                json.writeEndObject();
            }
            json.writeEndArray();
            json.writeEndObject();
        }
    }

    private static class TestStepReportTypeSerializer extends ValueSerializer<TestStepReportType> {

        @Override
        public void serialize(TestStepReportType testStepReport, JsonGenerator json, SerializationContext SerializationContext) {
            json.writeStartObject();
            if(testStepReport instanceof TAR tar) {
                json.writeStringProperty("name", tar.getName());
                json.writePOJOProperty("overview", tar.getOverview());
                json.writePOJOProperty("counters", tar.getCounters());
                json.writePOJOProperty("context", tar.getContext());
                json.writePOJOProperty("reports", tar.getReports());
                json.writeStringProperty("type", "TAR");

            } else if(testStepReport instanceof DR decisionReport) {
                json.writeStringProperty("type", "DR");
                json.writeBooleanProperty("decision", decisionReport.isDecision());
            } else {
                json.writeStringProperty("type", "SR");
            }
            if (testStepReport.getDate() != null) {
                String dateString = Configurations.DATE_TIME_FORMATTER().format(
                        testStepReport.getDate().toGregorianCalendar().toInstant());
                json.writePOJOProperty("date", dateString);
            }
            if (testStepReport.getResult() != null) {
                json.writeStringProperty("result", testStepReport.getResult().value());
            }
            json.writeStringProperty("id", testStepReport.getId());

            json.writeEndObject();
        }
    }

    private static class TestAssertionGroupReportsTypeSerializer extends ValueSerializer<TestAssertionGroupReportsType> {

        @Override
        public void serialize(TestAssertionGroupReportsType testAssertionGroupReportsType, JsonGenerator json, SerializationContext SerializationContext) {
            json.writeStartObject();
            json.writeArrayPropertyStart("reports");
            for(TAR tar : testAssertionGroupReportsType.getReports()) {
                json.writePOJO(tar);
            }
            json.writeEndArray();
            json.writeArrayPropertyStart("assertionReports");
            for (JAXBElement<TestAssertionReportType> element: testAssertionGroupReportsType.getInfoOrWarningOrError()) {
                json.writeStartObject();
                json.writeStringProperty("type", element.getName().getLocalPart());
                json.writePOJOProperty("value", element.getValue());
                json.writeEndObject();
            }
            json.writeEndArray();
            json.writeEndObject();
        }
    }

    private static class InteractWithUsersRequestSerializer extends ValueSerializer<InteractWithUsersRequest> {

        @Override
        public void serialize(InteractWithUsersRequest value, JsonGenerator json, SerializationContext provider) {
            json.writeStartObject();
            json.writeStringProperty("stepId", value.getStepId());
            json.writeStringProperty("tcInstanceId", value.getTcInstanceid());

            if (value.getInteraction().getWith() != null) {
                json.writeStringProperty("with", value.getInteraction().getWith());
            }
            if (value.getInteraction().getInputTitle() != null) {
                json.writeStringProperty("inputTitle", value.getInteraction().getInputTitle());
            }
            if (value.getInteraction().isAdmin()) {
                json.writeBooleanProperty("admin", true);
            }
            if (value.getInteraction().getDesc() != null) {
                json.writeStringProperty("desc", value.getInteraction().getDesc());
            }

            json.writeArrayPropertyStart("interactions");
            for (Object ior : value.getInteraction().getInstructionOrRequest()){
                if (ior instanceof InputRequest inputRequest) {
                    json.writeStartObject();
                    json.writeStringProperty("type", "request");
                    if(inputRequest.getId() != null) {
                        json.writeStringProperty("id", inputRequest.getId());
                    }
                    if(inputRequest.getDesc() != null) {
                        json.writeStringProperty("desc", inputRequest.getDesc());
                    }
                    if(inputRequest.getName() != null) {
                        json.writeStringProperty("name", inputRequest.getName());
                    }
                    if(inputRequest.getWith() != null) {
                        json.writeStringProperty("with", inputRequest.getWith());
                    }
                    if(inputRequest.getEncoding() != null) {
                        json.writeStringProperty("encoding", inputRequest.getEncoding());
                    }
                    if(inputRequest.getType() != null) {
                        json.writeStringProperty("variableType", inputRequest.getType());
                    }
                    if(inputRequest.getContentType() != null) {
                        json.writeStringProperty("contentType", inputRequest.getContentType().value());
                    }
                    if (inputRequest.getOptions() != null) {
                        json.writeStringProperty("options", inputRequest.getOptions());
                    }
                    if (inputRequest.getOptionLabels() != null) {
                        json.writeStringProperty("optionLabels", inputRequest.getOptionLabels());
                    }
                    if (inputRequest.isMultiple() != null) {
                        json.writeBooleanProperty("multiple", inputRequest.isMultiple());
                    }
                    if (inputRequest.getInputType() != null) {
                        json.writeStringProperty("inputType", inputRequest.getInputType().value());
                    }
                    if (inputRequest.getMimeType() != null) {
                        json.writeStringProperty("mimeType", inputRequest.getMimeType());
                    }
                    if (inputRequest.isRequired()) {
                        json.writeBooleanProperty("required", inputRequest.isRequired());
                    }
                    if (inputRequest.getSize() != null) {
                        json.writeNumberProperty("size", inputRequest.getSize());
                    }
                    if (inputRequest.getDefault() != null && !inputRequest.getDefault().isEmpty()) {
                        json.writeStringProperty("default", inputRequest.getDefault());
                    }
                    if (inputRequest.getAccept() != null && !inputRequest.getAccept().isEmpty()) {
                        json.writeStringProperty("accept", inputRequest.getAccept());
                    }
                    json.writeEndObject();
                } else if (ior instanceof com.gitb.tbs.Instruction instruction) {
                    json.writeStartObject();
                    json.writeStringProperty("type", "instruction");
                    if (instruction.getId() != null) {
                        json.writeStringProperty("id", instruction.getId());
                    }
                    if (instruction.getId() != null) {
                        json.writeStringProperty("desc", instruction.getDesc());
                    }
                    if (instruction.getWith() != null) {
                        json.writeStringProperty("with", instruction.getWith());
                    }
                    if (instruction.getName() != null) {
                        json.writeStringProperty("name", instruction.getName());
                    }
                    if (instruction.getValue() != null) {
                        json.writeStringProperty("value", instruction.getValue());
                    }
                    if (instruction.getType() != null) {
                        json.writeStringProperty("variableType", instruction.getType());
                    }
                    if (instruction.getEncoding() != null) {
                        json.writeStringProperty("encoding", instruction.getEncoding());
                    }
                    if (instruction.getEmbeddingMethod() != null) {
                        json.writeStringProperty("contentType", instruction.getEmbeddingMethod().value());
                    }
                    if (instruction.getMimeType() != null) {
                        json.writeStringProperty("mimeType", instruction.getMimeType());
                    }
                    if (instruction.getMetadata() != null) {
                        json.writeStringProperty("metadata", instruction.getMetadata());
                    }
                    if (instruction.isForceDisplay()) {
                        json.writeBooleanProperty("forceDisplay", instruction.isForceDisplay());
                    }
                    if (!instruction.isShowControls()) {
                        // true is the default
                        json.writeBooleanProperty("showControls", instruction.isShowControls());
                    }
                    if (instruction.getLevel() != null) {
                        json.writeStringProperty("level", instruction.getLevel().value());
                    }
                    json.writeEndObject();
                }
            }
            json.writeEndArray();
            json.writeEndObject();
        }
    }

    private static class TestStepPresentationSerializer extends ValueSerializer<TestStep> {

        @FunctionalInterface
        private interface SerializerFn {
            void apply() throws IOException;
        }

        private void writeStep(JsonGenerator jsonGenerator, TestStep step, SerializerFn fn) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringProperty("id",   step.getId());
            jsonGenerator.writeStringProperty("desc", step.getDesc());
            if (step.getDocumentation() != null) {
                jsonGenerator.writeStringProperty("documentation", HtmlUtil.sanitizeEditorContent(step.getDocumentation()));
            }
            fn.apply();
            jsonGenerator.writeEndObject();
        }

        @Override
        public void serialize(TestStep step, JsonGenerator jsonGenerator, SerializationContext SerializationContext) {
            try {
                switch (step) {
                    case MessagingStep messagingStep -> {
                        writeStep(jsonGenerator, messagingStep, () -> {
                            jsonGenerator.writeStringProperty("type", "msg");
                            jsonGenerator.writeStringProperty("from", messagingStep.getFrom());
                            jsonGenerator.writeStringProperty("to", messagingStep.getTo());
                            jsonGenerator.writeBooleanProperty("reply", messagingStep.isReply());
                        });
                    }
                    case DecisionStep decisionStep -> writeStep(jsonGenerator, decisionStep, () -> {
                        jsonGenerator.writeStringProperty("type", "decision");
                        jsonGenerator.writeStringProperty("title", decisionStep.getTitle());
                        jsonGenerator.writePOJOProperty("then", decisionStep.getThen());
                        jsonGenerator.writePOJOProperty("else", decisionStep.getElse());
                        jsonGenerator.writeBooleanProperty("collapsed", decisionStep.isCollapsed());
                    });
                    case LoopStep loopStep -> writeStep(jsonGenerator, loopStep, () -> {
                        jsonGenerator.writeStringProperty("type", "loop");
                        jsonGenerator.writeStringProperty("title", loopStep.getTitle());
                        jsonGenerator.writeBooleanProperty("collapsed", loopStep.isCollapsed());
                        jsonGenerator.writeArrayPropertyStart("steps");
                        for (TestStep testStep : loopStep.getSteps()) {
                            jsonGenerator.writePOJO(testStep);
                        }
                        jsonGenerator.writeEndArray();
                    });
                    case GroupStep groupStep -> writeStep(jsonGenerator, groupStep, () -> {
                        jsonGenerator.writeStringProperty("type", "group");
                        jsonGenerator.writeStringProperty("title", groupStep.getTitle());
                        jsonGenerator.writeBooleanProperty("collapsed", groupStep.isCollapsed());
                        jsonGenerator.writeArrayPropertyStart("steps");
                        for (TestStep testStep : groupStep.getSteps()) {
                            jsonGenerator.writePOJO(testStep);
                        }
                        jsonGenerator.writeEndArray();
                    });
                    case FlowStep flowStep -> writeStep(jsonGenerator, step, () -> {
                        jsonGenerator.writeStringProperty("type", "flow");
                        jsonGenerator.writeStringProperty("title", flowStep.getTitle());
                        jsonGenerator.writeBooleanProperty("collapsed", flowStep.isCollapsed());
                        jsonGenerator.writeArrayPropertyStart("threads");
                        for (Sequence sequence : flowStep.getThread()) {
                            jsonGenerator.writePOJO(sequence);
                        }
                        jsonGenerator.writeEndArray();
                    });
                    case ExitStep exitStep -> writeStep(jsonGenerator, step, () -> {
                        if (exitStep.getActor() != null && !exitStep.getActor().isEmpty()) {
                            jsonGenerator.writeStringProperty("from", exitStep.getActor());
                            jsonGenerator.writeStringProperty("to", exitStep.getActor());
                        }
                        jsonGenerator.writeStringProperty("type", "exit");
                    });
                    case UserInteractionStep userInteractionStep -> writeStep(jsonGenerator, step, () -> {
                        jsonGenerator.writeStringProperty("type", "interact");
                        jsonGenerator.writeStringProperty("title", userInteractionStep.getTitle());
                        jsonGenerator.writeBooleanProperty("collapsed", userInteractionStep.isCollapsed());
                        jsonGenerator.writeBooleanProperty("admin", userInteractionStep.isAdmin());
                        jsonGenerator.writeArrayPropertyStart("interactions");
                        for (InstructionOrRequest ior : userInteractionStep.getInstructOrRequest()) {
                            jsonGenerator.writeStartObject();
                            if (ior instanceof Instruction) {
                                jsonGenerator.writeStringProperty("type", "instruction");
                            } else if (ior instanceof UserRequest) {
                                jsonGenerator.writeStringProperty("type", "request");
                            }
                            jsonGenerator.writeStringProperty("id", ior.getId());
                            if (ior.getDesc() != null) {
                                jsonGenerator.writeStringProperty("desc", ior.getDesc());
                            }
                            if (ior.getWith() != null) {
                                jsonGenerator.writeStringProperty("with", ior.getWith());
                            }
                            jsonGenerator.writeEndObject();
                        }
                        jsonGenerator.writeEndArray();
                        jsonGenerator.writeStringProperty("with", userInteractionStep.getWith());
                        if (userInteractionStep.getActor() != null && !userInteractionStep.getActor().isEmpty()) {
                            jsonGenerator.writeStringProperty("from", userInteractionStep.getActor());
                            jsonGenerator.writeStringProperty("to", userInteractionStep.getActor());
                        }
                    });
                    case VerifyStep verifyStep -> writeStep(jsonGenerator, step, () -> {
                        if (verifyStep.getActor() != null && !verifyStep.getActor().isEmpty()) {
                            jsonGenerator.writeStringProperty("from", verifyStep.getActor());
                            jsonGenerator.writeStringProperty("to", verifyStep.getActor());
                        }
                        jsonGenerator.writeStringProperty("type", "verify");
                    });
                    case ProcessStep processStep -> writeStep(jsonGenerator, step, () -> {
                        if (processStep.getActor() != null && !processStep.getActor().isEmpty()) {
                            jsonGenerator.writeStringProperty("from", processStep.getActor());
                            jsonGenerator.writeStringProperty("to", processStep.getActor());
                        }
                        jsonGenerator.writeStringProperty("type", "process");
                    });
                    case Sequence sequence -> {
                        jsonGenerator.writeStartArray();
                        for (TestStep testStep : sequence.getSteps()) {
                            jsonGenerator.writePOJO(testStep);
                        }
                        jsonGenerator.writeEndArray();
                    }
                    case null ->
                            LOG.warn("Encountered unknown step type to serialize - ignoring.");
                    default ->
                            LOG.warn("Encountered unknown step type [{}] to serialize - ignoring.", step.getClass().getName());
                }
            } catch (IOException e) {
                throw new IllegalStateException("Error while serializing JSON", e);
            }
        }
    }

}
