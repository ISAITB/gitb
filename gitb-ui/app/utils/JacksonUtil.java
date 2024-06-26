package utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationModule;
import com.gitb.core.Actor;
import com.gitb.core.StepStatus;
import com.gitb.core.TestCaseType;
import com.gitb.tbs.ConfigurationCompleteRequest;
import com.gitb.tbs.InputRequest;
import com.gitb.tbs.InteractWithUsersRequest;
import com.gitb.tbs.TestStepStatus;
import com.gitb.tpl.*;
import com.gitb.tr.*;
import jakarta.xml.bind.JAXBElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * A Jackson wrapper for converting JAVA objects into JSON and vice versa
 */
public class JacksonUtil {

    private static final ObjectMapper mapper;
    private static final Logger LOG = LoggerFactory.getLogger(JacksonUtil.class);

    static {
        mapper = new ObjectMapper();

        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm"));

        //register JAXBAnnotation module
        mapper.registerModule(new JakartaXmlBindAnnotationModule());

        //register custom module for enum serializations
        SimpleModule customSerializersModule = new SimpleModule("CustomSerializersModule", new Version(1, 0, 0, null, null, null));
        customSerializersModule.addSerializer(StepStatus.class, new StatusSerializer());
        customSerializersModule.addSerializer(TestAssertionGroupReportsType.class, new TestAssertionGroupReportsTypeSerializer());
        customSerializersModule.addSerializer(TestStepReportType.class, new TestStepReportTypeSerializer());
        customSerializersModule.addSerializer(TestStep.class, new TestStepPresentationSerializer());
        customSerializersModule.addSerializer(InteractWithUsersRequest.class, new InteractWithUsersRequestSerializer());
        customSerializersModule.addSerializer(TestCaseType.class, new TestCaseTypeSerializer());
        customSerializersModule.addSerializer(Preliminary.class, new PreliminarySerializer());
        mapper.registerModule(customSerializersModule);
    }

    public static String serializeAny(Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }

    public static String serializeTestCasePresentation(TestCase testCase) throws JsonProcessingException {
        return mapper.writeValueAsString(testCase);
    }

    public static String serializeActorPresentation(Actor actor) throws JsonProcessingException {
        return mapper.writeValueAsString(actor);
    }

    public static String serializeConfigurationCompleteRequest(ConfigurationCompleteRequest request) throws JsonProcessingException {
        // We are calling getConfigs() to ensure that the configurations' array is not null. The goal is to include an empty array in the JSON to detect the type of payload.
        request.getConfigs();
        return mapper.writeValueAsString(request);
    }

    public static String serializeTestStepStatus(TestStepStatus testStepStatus) throws JsonProcessingException {
        return mapper.writeValueAsString(testStepStatus);
    }

    public static String serializeTestReport(TestStepReportType report) throws JsonProcessingException {
        return mapper.writeValueAsString(report);
    }


    public static String serializeInteractionRequest (InteractWithUsersRequest request) throws JsonProcessingException {
        return mapper.writeValueAsString(request);
    }

    private static class StatusSerializer extends JsonSerializer<StepStatus> {
        @Override
        public void serialize(StepStatus status, JsonGenerator json, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            json.writeNumber(status.ordinal());
        }
    }

    private static class TestCaseTypeSerializer extends JsonSerializer<TestCaseType> {
        @Override
        public void serialize(TestCaseType value, JsonGenerator json, SerializerProvider provider) throws IOException, JsonProcessingException {
            json.writeNumber(value.ordinal());
        }
    }

    private static class PreliminarySerializer extends JsonSerializer<Preliminary> {

        @Override
        public void serialize(Preliminary value, JsonGenerator json, SerializerProvider provider) throws IOException, JsonProcessingException {
            json.writeStartObject();
            json.writeArrayFieldStart("interactions");
            for (InstructionOrRequest item: value.getInstructOrRequest()) {
                json.writeStartObject();
                if (item instanceof Instruction) {
                    json.writeStringField("type", "instruction");
                } else {
                    json.writeStringField("type", "request");
                }
                json.writeStringField("desc", item.getDesc());
                json.writeStringField("with", item.getWith());
                json.writeStringField("id", item.getId());
                json.writeEndObject();
            }
            json.writeEndArray();
            json.writeEndObject();
        }
    }

    private static class TestStepReportTypeSerializer extends JsonSerializer<TestStepReportType> {

        @Override
        public void serialize(TestStepReportType testStepReport, JsonGenerator json, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            json.writeStartObject();
            if(testStepReport instanceof TAR tar) {
                json.writeStringField("name", tar.getName());
                json.writeObjectField("overview", tar.getOverview());
                json.writeObjectField("counters", tar.getCounters());
                json.writeObjectField("context", tar.getContext());
                json.writeObjectField("reports", tar.getReports());
                json.writeStringField("type", "TAR");

            } else if(testStepReport instanceof DR decisionReport) {
                json.writeStringField("type", "DR");
                json.writeBooleanField("decision", decisionReport.isDecision());
            } else {
                json.writeStringField("type", "SR");
            }
            if(testStepReport.getDate() != null) {
                json.writeObjectField("date", testStepReport.getDate());
            }
            if (testStepReport.getResult() != null) {
                json.writeStringField("result", testStepReport.getResult().value());
            }
            json.writeStringField("id", testStepReport.getId());

            json.writeEndObject();
        }
    }

    private static class TestAssertionGroupReportsTypeSerializer extends JsonSerializer<TestAssertionGroupReportsType> {

        @Override
        public void serialize(TestAssertionGroupReportsType testAssertionGroupReportsType, JsonGenerator json, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            json.writeStartObject();
            json.writeArrayFieldStart("reports");
            for(TAR tar : testAssertionGroupReportsType.getReports()) {
                json.writeObject(tar);
            }
            json.writeEndArray();
            json.writeArrayFieldStart("assertionReports");
            for (JAXBElement<TestAssertionReportType> element: testAssertionGroupReportsType.getInfoOrWarningOrError()) {
                json.writeStartObject();
                json.writeStringField("type", element.getName().getLocalPart());
                json.writeObjectField("value", element.getValue());
                json.writeEndObject();
            }
            json.writeEndArray();
            json.writeEndObject();
        }
    }

    private static class InteractWithUsersRequestSerializer extends JsonSerializer<InteractWithUsersRequest> {

        @Override
        public void serialize(InteractWithUsersRequest value, JsonGenerator json, SerializerProvider provider) throws IOException, JsonProcessingException {
            json.writeStartObject();
            json.writeStringField("stepId", value.getStepId());
            json.writeStringField("tcInstanceId", value.getTcInstanceid());

            if (value.getInteraction().getWith() != null) {
                json.writeStringField("with", value.getInteraction().getWith());
            }
            if (value.getInteraction().getInputTitle() != null) {
                json.writeStringField("inputTitle", value.getInteraction().getInputTitle());
            }
            if (value.getInteraction().isAdmin()) {
                json.writeBooleanField("admin", true);
            }
            if (value.getInteraction().getDesc() != null) {
                json.writeStringField("desc", value.getInteraction().getDesc());
            }

            json.writeArrayFieldStart("interactions");
            for(Object ior : value.getInteraction().getInstructionOrRequest()){
                if(ior instanceof InputRequest inputRequest) {
                    json.writeStartObject();
                    json.writeStringField("type", "request");
                    if(inputRequest.getId() != null) {
                        json.writeStringField("id", inputRequest.getId());
                    }
                    if(inputRequest.getDesc() != null) {
                        json.writeStringField("desc", inputRequest.getDesc());
                    }
                    if(inputRequest.getName() != null) {
                        json.writeStringField("name", inputRequest.getName());
                    }
                    if(inputRequest.getWith() != null) {
                        json.writeStringField("with", inputRequest.getWith());
                    }
                    if(inputRequest.getEncoding() != null) {
                        json.writeStringField("encoding", inputRequest.getEncoding());
                    }
                    if(inputRequest.getType() != null) {
                        json.writeStringField("variableType", inputRequest.getType());
                    }
                    if(inputRequest.getContentType() != null) {
                        json.writeStringField("contentType", inputRequest.getContentType().value());
                    }
                    if (inputRequest.getOptions() != null) {
                        json.writeStringField("options", inputRequest.getOptions());
                    }
                    if (inputRequest.getOptionLabels() != null) {
                        json.writeStringField("optionLabels", inputRequest.getOptionLabels());
                    }
                    if (inputRequest.isMultiple() != null) {
                        json.writeBooleanField("multiple", inputRequest.isMultiple());
                    }
                    if (inputRequest.getInputType() != null) {
                        json.writeStringField("inputType", inputRequest.getInputType().value());
                    }
                    if (inputRequest.getMimeType() != null) {
                        json.writeStringField("mimeType", inputRequest.getMimeType());
                    }
                    json.writeEndObject();
                } else if (ior instanceof com.gitb.tbs.Instruction instruction) {
                    json.writeStartObject();
                    json.writeStringField("type", "instruction");
                    if(instruction.getId() != null) {
                        json.writeStringField("id", instruction.getId());
                    }
                    if(instruction.getId() != null) {
                        json.writeStringField("desc", instruction.getDesc());
                    }
                    if(instruction.getWith() != null) {
                        json.writeStringField("with", instruction.getWith());
                    }
                    if(instruction.getName() != null) {
                        json.writeStringField("name", instruction.getName());
                    }
                    if(instruction.getValue() != null) {
                        json.writeStringField("value", instruction.getValue());
                    }
                    if(instruction.getType() != null) {
                        json.writeStringField("variableType", instruction.getType());
                    }
                    if(instruction.getEncoding() != null) {
                        json.writeStringField("encoding", instruction.getEncoding());
                    }
                    if(instruction.getEmbeddingMethod() != null) {
                        json.writeStringField("contentType", instruction.getEmbeddingMethod().value());
                    }
                    if (instruction.getMimeType() != null) {
                        json.writeStringField("mimeType", instruction.getMimeType());
                    }
                    if (instruction.isForceDisplay()) {
                        json.writeBooleanField("forceDisplay", instruction.isForceDisplay());
                    }
                    json.writeEndObject();
                }
            }
            json.writeEndArray();
            json.writeEndObject();
        }
    }

    private static class TestStepPresentationSerializer extends JsonSerializer<TestStep> {

        @FunctionalInterface
        private interface SerializerFn {
            void apply() throws IOException;
        }

        private void writeStep(JsonGenerator jsonGenerator, TestStep step, SerializerFn fn) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("id",   step.getId());
            jsonGenerator.writeStringField("desc", step.getDesc());
            if (step.getDocumentation() != null) {
                jsonGenerator.writeStringField("documentation", HtmlUtil.sanitizeEditorContent(step.getDocumentation()));
            }
            fn.apply();
            jsonGenerator.writeEndObject();
        }

        @Override
        public void serialize(TestStep step, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            if (step instanceof MessagingStep) {
                writeStep(jsonGenerator, step, () -> {
                    jsonGenerator.writeStringField("type", "msg");
                    jsonGenerator.writeStringField("from", ((MessagingStep) step).getFrom());
                    jsonGenerator.writeStringField("to", ((MessagingStep) step).getTo());
                    jsonGenerator.writeBooleanField("reply", ((MessagingStep) step).isReply());
                });
            } else if (step instanceof DecisionStep) {
                writeStep(jsonGenerator, step, () -> {
                    jsonGenerator.writeStringField("type", "decision");
                    jsonGenerator.writeStringField("title", ((DecisionStep) step).getTitle());
                    jsonGenerator.writeObjectField("then", ((DecisionStep) step).getThen());
                    jsonGenerator.writeObjectField("else", ((DecisionStep) step).getElse());
                    jsonGenerator.writeBooleanField("collapsed", ((DecisionStep) step).isCollapsed());
                });
            } else if (step instanceof LoopStep) {
                writeStep(jsonGenerator, step, () -> {
                    jsonGenerator.writeStringField("type", "loop");
                    jsonGenerator.writeStringField("title", ((LoopStep) step).getTitle());
                    jsonGenerator.writeBooleanField("collapsed", ((LoopStep) step).isCollapsed());
                    jsonGenerator.writeArrayFieldStart("steps");
                    for(TestStep testStep : ((Sequence) step).getSteps()) {
                        jsonGenerator.writeObject(testStep);
                    }
                    jsonGenerator.writeEndArray();
                });
            } else if (step instanceof GroupStep) {
                writeStep(jsonGenerator, step, () -> {
                    jsonGenerator.writeStringField("type", "group");
                    jsonGenerator.writeStringField("title", ((GroupStep) step).getTitle());
                    jsonGenerator.writeBooleanField("collapsed", ((GroupStep) step).isCollapsed());
                    jsonGenerator.writeArrayFieldStart("steps");
                    for(TestStep testStep : ((Sequence) step).getSteps()) {
                        jsonGenerator.writeObject(testStep);
                    }
                    jsonGenerator.writeEndArray();
                });
            } else if (step instanceof FlowStep) {
                writeStep(jsonGenerator, step, () -> {
                    jsonGenerator.writeStringField("type", "flow");
                    jsonGenerator.writeStringField("title", ((FlowStep) step).getTitle());
                    jsonGenerator.writeBooleanField("collapsed", ((FlowStep) step).isCollapsed());
                    jsonGenerator.writeArrayFieldStart("threads");
                    for(Sequence sequence : ((FlowStep) step).getThread()) {
                        jsonGenerator.writeObject(sequence);
                    }
                    jsonGenerator.writeEndArray();
                });
            } else if (step instanceof ExitStep) {
                writeStep(jsonGenerator, step, () -> {
                    jsonGenerator.writeStringField("type", "exit");
                });
            } else if (step instanceof UserInteractionStep) {
                writeStep(jsonGenerator, step, () -> {
                    jsonGenerator.writeStringField("type", "interact");
                    jsonGenerator.writeStringField("title", ((UserInteractionStep) step).getTitle());
                    jsonGenerator.writeBooleanField("collapsed", ((UserInteractionStep) step).isCollapsed());
                    jsonGenerator.writeBooleanField("admin", ((UserInteractionStep) step).isAdmin());
                    jsonGenerator.writeArrayFieldStart("interactions");
                    for(InstructionOrRequest ior : ((UserInteractionStep) step).getInstructOrRequest()){
                        jsonGenerator.writeStartObject();
                        if(ior instanceof Instruction) {
                            jsonGenerator.writeStringField("type", "instruction");
                        } else if (ior instanceof UserRequest){
                            jsonGenerator.writeStringField("type", "request");
                        }
                        jsonGenerator.writeStringField("id",   ior.getId());
                        if(ior.getDesc() != null) {
                            jsonGenerator.writeStringField("desc", ior.getDesc());
                        }
                        if(ior.getWith() != null) {
                            jsonGenerator.writeStringField("with", ior.getWith());
                        }
                        jsonGenerator.writeEndObject();
                    }
                    jsonGenerator.writeEndArray();
                    jsonGenerator.writeStringField("with", ((UserInteractionStep) step).getWith());
                });
            } else if (step instanceof VerifyStep) {
                writeStep(jsonGenerator, step, () -> {
                    jsonGenerator.writeStringField("type", "verify");
                });
            } else if (step instanceof ProcessStep) {
                writeStep(jsonGenerator, step, () -> {
                    jsonGenerator.writeStringField("type", "process");
                });
            } else if (step instanceof Sequence) {
                jsonGenerator.writeStartArray();
                for(TestStep testStep : ((Sequence) step).getSteps()) {
                    jsonGenerator.writeObject(testStep);
                }
                jsonGenerator.writeEndArray();
            } else {
                LOG.warn("Encountered unknown step type ["+step.getClass().getName()+"] to serialize - ignoring.");
            }
        }
    }

}
