package com.gitb.processing;

import com.gitb.core.*;
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

    @Override
    public String beginTransaction(List<Configuration> config) {
        // Transactions not needed.
        return "";
    }

    @Override
    public void endTransaction(String session) {
        // Do nothing.
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

    <T extends DataType> T getInputForName(ProcessingData data, String inputName, Class<T> type) {
        if (data.getData() != null) {
            DataType value = data.getData().get(inputName);
            if (type.isInstance(value)) {
                return (T)value;
            } else {
                try {
                    type.getDeclaredConstructor().newInstance();
                } catch (InstantiationException|IllegalAccessException| InvocationTargetException |NoSuchMethodException e) {
                    throw new IllegalArgumentException("Unable to cast provided input ["+value.getClass().getName()+"] to ["+type.getName()+"]", e);
                }
            }
        }
        return null;
    }

}
