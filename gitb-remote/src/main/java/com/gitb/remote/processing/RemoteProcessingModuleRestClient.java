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

package com.gitb.remote.processing;

import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.model.ps.BeginTransactionRequest;
import com.gitb.model.ps.ProcessRequest;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.BeginTransactionResponse;
import com.gitb.ps.ProcessingModule;
import com.gitb.remote.ClientConfiguration;
import com.gitb.remote.RemoteServiceRestClient;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ModelUtils;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;

public class RemoteProcessingModuleRestClient extends RemoteServiceRestClient implements IProcessingHandler {

    private ProcessingModule serviceModule;

    public RemoteProcessingModuleRestClient(URI serviceURL, Properties callProperties, String sessionId, String testCaseIdentifier, Supplier<ClientConfiguration> clientConfiguration) {
        super(serviceURL, callProperties, sessionId, testCaseIdentifier, clientConfiguration);
    }

    public RemoteProcessingModuleRestClient(URI serviceURL, Properties callProperties) {
        this(serviceURL, callProperties, null, null, null);
    }

    @Override
    protected String getServiceLocation() {
        if (serviceModule != null) {
            return serviceModule.getServiceLocation();
        }
        return null;
    }

    @Override
    public ProcessingModule getModuleDefinition() {
        return getModuleDefinition(true);
    }

    public ProcessingModule getModuleDefinition(boolean cacheResult) {
        if (serviceModule != null) {
            return serviceModule;
        } else {
            com.gitb.ps.GetModuleDefinitionResponse response = call("GET", "getModuleDefinition", Optional.empty(), Optional.of(com.gitb.model.ps.GetModuleDefinitionResponse.class))
                    .map(ModelUtils::fromModel)
                    .orElseThrow(() -> new IllegalStateException("Remote service did not return a valid response"));
            ProcessingModule result = response.getModule();
            if (cacheResult) {
                if (result == null) result = new ProcessingModule();
                serviceModule = result;
            }
            return result;
        }
    }

    @Override
    public String beginTransaction(String stepId, List<Configuration> config) {
        var transactionRequest = new BeginTransactionRequest();
        if (config != null) {
            transactionRequest.getConfig().addAll(config.stream().map(ModelUtils::toModel).toList());
        }
        return call("POST", "beginTransaction", Optional.of(transactionRequest), Optional.of(BeginTransactionResponse.class), stepIdMap(stepId))
                .map(BeginTransactionResponse::getSessionId)
                .orElseThrow(() -> new IllegalStateException("Remote service did not return a valid response"));
    }

    @Override
    public ProcessingReport process(String session, String stepId, String operation, ProcessingData data) {
        ProcessRequest request = ProcessRequest.builder()
                .withSessionId(session)
                .withOperation(operation)
                .withInput(getInput(data).toArray(com.gitb.model.core.AnyContent[]::new))
                .build();
        return call("POST", "process", Optional.of(request), Optional.of(com.gitb.model.ps.ProcessResponse.class), stepIdMap(stepId))
                .map(response -> new ProcessingReport(ModelUtils.fromModel(response.getReport()), getOutput(response.getOutput())))
                .orElseThrow(() -> new IllegalStateException("Remote service did not return a valid response"));
    }

    @Override
    public void endTransaction(String session, String stepId) {
        com.gitb.model.core.BasicRequest basicRequest = com.gitb.model.core.BasicRequest.builderForBasicRequest()
                .withSessionId(session)
                .build();
        call("POST", "endTransaction", Optional.of(basicRequest), Optional.empty(), stepIdMap(stepId));
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    private List<com.gitb.model.core.AnyContent> getInput(ProcessingData data) {
        List<com.gitb.model.core.AnyContent> result = new ArrayList<>();
        for (Map.Entry<String, DataType> inputEntry : data.getData().entrySet()) {
            AnyContent anyContent = DataTypeUtils.convertDataTypeToAnyContent(inputEntry.getKey(), inputEntry.getValue());
            result.add(ModelUtils.toModel(anyContent));
        }
        return result;
    }

    private ProcessingData getOutput(List<com.gitb.model.core.AnyContent> output) {
        ProcessingData data = new ProcessingData();
        for (com.gitb.model.core.AnyContent content : output) {
            if (content.getName() != null) {
                var value = DataTypeFactory.getInstance().create(ModelUtils.fromModel(content));
                if (value != null) {
                    data.getData().put(content.getName(), value);
                }
            }
        }
        return data;
    }

}
