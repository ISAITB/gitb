package utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.gitb.core.Actor;
import com.gitb.core.ActorConfiguration;
import com.gitb.core.StepStatus;
import com.gitb.core.TestCaseType;
import com.gitb.tbs.*;
import com.gitb.tpl.*;
import com.gitb.tpl.Instruction;
import com.gitb.tr.*;

import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * A Jackson wrapper for converting JAVA objects into JSON and vice versa
 */
public class JacksonUtil {

    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();

        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm"));

        //register JAXBAnnotation module
        JaxbAnnotationModule module = new JaxbAnnotationModule();
        mapper.registerModule(module);

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

    public static String serializeConfigureResponse(ConfigureResponse response) throws JsonProcessingException {
        return mapper.writeValueAsString(response);
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

    public static List<ActorConfiguration> parseActorConfigurations(String json) throws IOException {
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, ActorConfiguration.class);
        List<ActorConfiguration> configs = mapper.readValue(json, type);
        return configs;
    }

    @JsonCreator
    public static List<UserInput> parseUserInputs(String json) throws IOException {
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, UserInput.class);
        List<UserInput> inputs = mapper.readValue(json, type);
        return inputs;
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
            if(testStepReport instanceof TAR) {
                TAR tar = (TAR) testStepReport;
                json.writeStringField("name", tar.getName());
                json.writeObjectField("overview", tar.getOverview());
                json.writeObjectField("counters", tar.getCounters());
                json.writeObjectField("context", tar.getContext());
                json.writeObjectField("reports", tar.getReports());
                json.writeStringField("type", "TAR");

            } else if(testStepReport instanceof DR) {
                DR decisionReport = (DR) testStepReport;
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
//			json.writeStartArray();
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

            if(value.getInteraction().getWith() != null) {
                json.writeStringField("with", value.getInteraction().getWith());
            }

            json.writeArrayFieldStart("interactions");
            for(Object ior : value.getInteraction().getInstructionOrRequest()){
                if(ior instanceof InputRequest) {
                    InputRequest inputRequest = (InputRequest) ior;
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
                    json.writeEndObject();
                } else if (ior instanceof com.gitb.tbs.Instruction) {
                    com.gitb.tbs.Instruction instruction = (com.gitb.tbs.Instruction) ior;
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
                    json.writeEndObject();
                }
            }
            json.writeEndArray();
            json.writeEndObject();
        }
    }

    private static class TestStepPresentationSerializer extends JsonSerializer<TestStep> {
        @Override
        public void serialize(TestStep step, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {

            if(step instanceof Sequence && step.getDesc() == null) {
                jsonGenerator.writeStartArray();
                for(TestStep testStep : ((Sequence) step).getSteps()) {
                    jsonGenerator.writeObject(testStep);
                }
                jsonGenerator.writeEndArray();
            } else {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeStringField("id",   step.getId());
                jsonGenerator.writeStringField("desc", step.getDesc());

                if(step instanceof MessagingStep) {
                    jsonGenerator.writeStringField("type", "msg");
                    jsonGenerator.writeStringField("from", ((MessagingStep) step).getFrom());
                    jsonGenerator.writeStringField("to", ((MessagingStep) step).getTo());
                } else if (step instanceof DecisionStep) {
                    jsonGenerator.writeStringField("type", "decision");
                    jsonGenerator.writeObjectField("then", ((DecisionStep) step).getThen());
                    jsonGenerator.writeObjectField("else", ((DecisionStep) step).getElse());
                } else if (step instanceof Sequence) {
                    jsonGenerator.writeStringField("type", "loop");
                    jsonGenerator.writeArrayFieldStart("steps");
                    for(TestStep testStep : ((Sequence) step).getSteps()) {
                        jsonGenerator.writeObject(testStep);
                    }
                    jsonGenerator.writeEndArray();
                } else if (step instanceof FlowStep) {
                    jsonGenerator.writeStringField("type", "flow");
                    jsonGenerator.writeArrayFieldStart("threads");
                    for(Sequence sequence : ((FlowStep) step).getThread()) {
                        jsonGenerator.writeObject(sequence);
                    }
                    jsonGenerator.writeEndArray();
                } else if (step instanceof ExitStep) {
                    jsonGenerator.writeStringField("type", "exit");
                } else if (step instanceof UserInteractionStep) {
                    jsonGenerator.writeStringField("type", "interact");
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
                } else {
                    jsonGenerator.writeStringField("type", "verify");
                }

                jsonGenerator.writeEndObject();
            }
        }
    }

}
