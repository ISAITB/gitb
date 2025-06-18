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

import com.gitb.core.*;
import com.gitb.engine.AbstractHandler;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.ps.ProcessingOperation;
import com.gitb.tr.TAR;
import com.gitb.tr.TestResultType;
import com.gitb.types.DataType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.GregorianCalendar;
import java.util.List;

public abstract class AbstractProcessingHandler extends AbstractHandler implements IProcessingHandler {

    private final ProcessingModule moduleDefinition;

    public AbstractProcessingHandler() {
        this.moduleDefinition = createProcessingModule();
    }

    @Override
    public ProcessingModule getModuleDefinition() {
        return moduleDefinition;
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

    protected abstract ProcessingModule createProcessingModule();

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
