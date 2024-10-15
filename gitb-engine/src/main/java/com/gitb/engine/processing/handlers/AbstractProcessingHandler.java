package com.gitb.engine.processing.handlers;

import com.gitb.core.*;
import com.gitb.engine.testcase.TestCaseScope;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingOperation;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.types.DataType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.GregorianCalendar;
import java.util.List;

public abstract class AbstractProcessingHandler implements IProcessingHandler {

    protected TestCaseScope scope;

    public void setScope(TestCaseScope scope) {
        this.scope = scope;
    }

    @Override
    public String beginTransaction(String stepId, List<Configuration> config) {
        // Transactions not needed.
        return "";
    }

    @Override
    public void endTransaction(String session, String stepId) {
        // Do nothing.
    }

    @Override
    public ProcessingReport process(String session, String stepId, String operation, ProcessingData input) {
        return process(session, operation, input);
    }

    ProcessingOperation createProcessingOperation(String name, List<TypedParameter> input, List<TypedParameter> output) {
        ProcessingOperation operation = new ProcessingOperation();
        operation.setName(name);
        operation.setInputs(new TypedParameters());
        operation.getInputs().getParam().addAll(input);
        operation.setOutputs(new TypedParameters());
        operation.getOutputs().getParam().addAll(output);
        return operation;
    }

    TypedParameter createParameter(String name, String type, UsageEnumeration use, ConfigurationType kind, String description) {
        TypedParameter parameter =  new TypedParameter();
        parameter.setName(name);
        parameter.setType(type);
        parameter.setUse(use);
        parameter.setKind(kind);
        parameter.setDesc(description);
        return parameter;
    }

    TAR createReport(TestResultType result) {
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

    private <T extends DataType> T getDataForType(DataType value, Class<T> type) {
        if (value != null) {
            if (type.isInstance(value)) {
                return (T)value;
            } else {
                try {
                    var returnType = type.getDeclaredConstructor().newInstance();
                    return (T)value.convertTo(returnType.getType());
                } catch (InstantiationException|IllegalAccessException| InvocationTargetException |NoSuchMethodException e) {
                    throw new IllegalArgumentException("Unable to cast provided input ["+value.getClass().getName()+"] to ["+type.getName()+"]", e);
                }
            }
        }
        return null;
    }

    <T extends DataType> T getDefaultInput(ProcessingData data, Class<T> type) {
        if (data.getData() != null && data.getData().size() == 1) {
            return getDataForType(data.getData().values().stream().findFirst().get(), type);
        }
        return null;
    }

    <T extends DataType> T getInputForName(ProcessingData data, String inputName, Class<T> type) {
        if (data.getData() != null) {
            return getDataForType(data.getData().get(inputName), type);
        }
        return null;
    }

    <T extends DataType> T getRequiredInputForName(ProcessingData data, String inputName, Class<T> type) {
        var value = getInputForName(data, inputName, type);
        if (value == null) {
            throw new IllegalArgumentException("Required input [%s] not provided".formatted(inputName));
        }
        return value;
    }

    public abstract ProcessingReport process(String session, String operation, ProcessingData input);

}
