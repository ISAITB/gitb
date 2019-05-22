package com.gitb.processing;

import com.gitb.core.*;
import com.gitb.ps.ProcessingModule;
import com.gitb.ps.ProcessingOperation;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.types.DataType;
import com.gitb.types.NumberType;
import com.gitb.types.StringType;
import com.mifmif.common.regex.Generex;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.MetaInfServices;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@MetaInfServices(IProcessingHandler.class)
public class TokenGenerator implements IProcessingHandler {

    private static final String OPERATION__TIMESTAMP = "timestamp";
    private static final String OPERATION__UUID = "uuid";
    private static final String OPERATION__STRING = "string";

    private static final String INPUT__FORMAT = "format";
    private static final String INPUT__TIME = "time";

    private static final String OUTPUT__VALUE = "value";

    private static final String DEFAULT_DATE_FORMAT = "dd/MM/yyyy'T'HH:mm:ss:SSS";

    @Override
    public ProcessingModule getModuleDefinition() {
        ProcessingModule module = new ProcessingModule();
        module.setId("TokenGenerator");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());

        TypedParameter outputText = createParameter(OUTPUT__VALUE, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The output value.");
        TypedParameter timestampFormat = createParameter(INPUT__FORMAT, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The optional format string to apply (see https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html). Default is '"+DEFAULT_DATE_FORMAT+"'.");
        TypedParameter milliseconds = createParameter(INPUT__TIME, "number", UsageEnumeration.O, ConfigurationType.SIMPLE, "The optional time (in epoch milliseconds) to use as the value (default is the current time).");
        TypedParameter regexpFormat = createParameter(INPUT__FORMAT, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "A regular expression defining the syntax and static parts of the returned string.");

        module.getOperation().add(createProcessingOperation(OPERATION__UUID, Collections.emptyList(), Arrays.asList(outputText)));
        module.getOperation().add(createProcessingOperation(OPERATION__TIMESTAMP, Arrays.asList(timestampFormat, milliseconds), Arrays.asList(outputText)));
        module.getOperation().add(createProcessingOperation(OPERATION__STRING, Arrays.asList(regexpFormat), Arrays.asList(outputText)));
        return module;
    }

    @Override
    public String beginTransaction(List<Configuration> config) {
        // Transactions not needed.
        return "";
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        if (StringUtils.isBlank(operation)) {
            throw new IllegalArgumentException("No operation provided");
        }
        String value;
        if (OPERATION__UUID.equalsIgnoreCase(operation)) {
            value = UUID.randomUUID().toString();
        } else if (OPERATION__TIMESTAMP.equalsIgnoreCase(operation)) {
            StringType format = getInputForName(input, INPUT__FORMAT, StringType.class);
            NumberType time = getInputForName(input, INPUT__TIME, NumberType.class);
            LocalDateTime date;
            if (time == null) {
                date = LocalDateTime.now();
            } else {
                date = Instant.ofEpochMilli(time.longValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            DateTimeFormatter formatter;
            if (format == null) {
                formatter = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT);
            } else {
                formatter = DateTimeFormatter.ofPattern((String)format.getValue());
            }
            value = formatter.format(date);
        } else if (OPERATION__STRING.equalsIgnoreCase(operation)) {
            StringType format = getInputForName(input, INPUT__FORMAT, StringType.class);
            if (format == null) {
                throw new IllegalArgumentException("Format to use for string generation is required");
            }
            try {
                Generex generex = new Generex((String)format.getValue());
                value = generex.random();
            } catch (Exception e) {
                throw new IllegalArgumentException("Generation of string failed for expression ["+format.getValue()+"]", e);
            }
        } else {
            throw new IllegalArgumentException("Unknown operation ["+operation+"]");
        }
        ProcessingData data = new ProcessingData();
        data.getData().put(OUTPUT__VALUE, new StringType(value));
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    @Override
    public void endTransaction(String session) {
        // Do nothing.
    }

    private ProcessingOperation createProcessingOperation(String name, List<TypedParameter> input, List<TypedParameter> output) {
        ProcessingOperation operation = new ProcessingOperation();
        operation.setName(name);
        operation.setInputs(new TypedParameters());
        operation.getInputs().getParam().addAll(input);
        operation.setOutputs(new TypedParameters());
        operation.getOutputs().getParam().addAll(output);
        return operation;
    }

    private TypedParameter createParameter(String name, String type, UsageEnumeration use, ConfigurationType kind, String description) {
        TypedParameter parameter =  new TypedParameter();
        parameter.setName(name);
        parameter.setType(type);
        parameter.setUse(use);
        parameter.setKind(kind);
        parameter.setDesc(description);
        return parameter;
    }

    private TAR createReport(TestResultType result) {
        TAR report = new TAR();
        report.setContext(new AnyContent());
        report.getContext().setType("map");
        report.setResult(result);
        try {
            report.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
        return report;
    }

    private <T extends DataType> T getInputForName(ProcessingData data, String inputName, Class<T> type) {
        if (data.getData() != null) {
            DataType value = data.getData().get(inputName);
            if (type.isInstance(value)) {
                return (T)value;
            } else {
                try {
                    type.getDeclaredConstructor().newInstance();
                } catch (InstantiationException|IllegalAccessException|InvocationTargetException|NoSuchMethodException e) {
                    throw new IllegalArgumentException("Unable to cast provided input ["+value.getClass().getName()+"] to ["+type.getName()+"]", e);
                }
            }
        }
        return null;
    }

}
