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

package com.gitb.engine.remote.processing;

import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.engine.remote.ClientConfiguration;
import com.gitb.engine.remote.RemoteServiceClient;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.Void;
import com.gitb.ps.*;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.utils.DataTypeUtils;
import jakarta.xml.ws.soap.AddressingFeature;
import org.apache.cxf.endpoint.Client;

import java.net.URL;
import java.util.*;

public class RemoteProcessingModuleClient extends RemoteServiceClient implements IProcessingHandler {

    private ProcessingModule serviceModule;

    public RemoteProcessingModuleClient(URL serviceURL, Properties callProperties, String sessionId, ClientConfiguration clientConfiguration) {
        super(serviceURL, callProperties, sessionId, clientConfiguration);
    }

    @Override
    protected String getServiceLocation() {
        if (serviceModule != null) {
            return serviceModule.getServiceLocation();
        }
        return null;
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public ProcessingModule getModuleDefinition() {
        if (serviceModule == null) {
            serviceModule = call(() -> Optional.ofNullable(getServiceClient().getModuleDefinition(new Void()).getModule()).orElseGet(ProcessingModule::new));
        }
        return serviceModule;
    }

    @Override
    public String beginTransaction(String stepId, List<Configuration> config) {
        BeginTransactionRequest transactionRequest = new BeginTransactionRequest();
        transactionRequest.getConfig().addAll(config);
        return call(() -> getServiceClient().beginTransaction(transactionRequest).getSessionId(), stepIdMap(stepId));
    }

    private String sessionIdToUse(String processingSessionId) {
        if (processingSessionId == null) {
            return testSessionId;
        } else {
            return processingSessionId;
        }
    }

    @Override
    public ProcessingReport process(String processingSessionId, String stepId, String operation, ProcessingData data) {
        ProcessRequest processRequest = new ProcessRequest();
        processRequest.setSessionId(sessionIdToUse(processingSessionId));
        processRequest.setOperation(operation);
        processRequest.getInput().addAll(getInput(data));
        ProcessResponse processResponse = call(() -> getServiceClient().process(processRequest), stepIdMap(stepId));
        return new ProcessingReport(processResponse.getReport(), getOutput(processResponse.getOutput()));
    }

    private List<AnyContent> getInput(ProcessingData data) {
        List<AnyContent> result = new ArrayList<>();
        for (Map.Entry<String, DataType> inputEntry : data.getData().entrySet()) {
            AnyContent anyContent = DataTypeUtils.convertDataTypeToAnyContent(inputEntry.getKey(), inputEntry.getValue());
            result.add(anyContent);
        }
        return result;
    }

    private ProcessingData getOutput(List<AnyContent> output) {
        ProcessingData data = new ProcessingData();
        for (AnyContent content : output) {
            if (content.getName() != null) {
                var value = DataTypeFactory.getInstance().create(content);
                if (value != null) {
                    data.getData().put(content.getName(), value);
                }
            }
        }
        return data;
    }

    @Override
    public void endTransaction(String processingSessionId, String stepId) {
        BasicRequest basicRequest = new BasicRequest();
        basicRequest.setSessionId(sessionIdToUse(processingSessionId));
        call(() -> getServiceClient().endTransaction(basicRequest), stepIdMap(stepId));
    }

    private ProcessingService getServiceClient() {
        TestCaseUtils.prepareRemoteServiceLookup(getCallProperties());
        var client = new ProcessingServiceClient(getServiceURL()).getProcessingServicePort(new AddressingFeature(true));
        prepareClient((Client)client);
        return client;
    }

}
