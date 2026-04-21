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

package com.gitb.remote.validation;

import com.gitb.core.Configuration;
import com.gitb.core.ValidationModule;
import com.gitb.model.vs.ValidateRequest;
import com.gitb.model.vs.ValidationResponse;
import com.gitb.remote.ClientConfiguration;
import com.gitb.remote.RemoteServiceRestClient;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ModelUtils;
import com.gitb.validation.IValidationHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;

public class RemoteValidationModuleRestClient extends RemoteServiceRestClient implements IValidationHandler {

    private ValidationModule serviceModule;

    public RemoteValidationModuleRestClient(URI serviceURL, Properties callProperties, String sessionId, String testCaseIdentifier, Supplier<ClientConfiguration> clientConfigurationProvider) {
        super(serviceURL, callProperties, sessionId, testCaseIdentifier, clientConfigurationProvider);
    }

    public RemoteValidationModuleRestClient(URI serviceURL, Properties callProperties) {
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
    public ValidationModule getModuleDefinition() {
        return getModuleDefinition(true);
    }

    public ValidationModule getModuleDefinition(boolean cacheResult) {
        if (serviceModule != null) {
            return serviceModule;
        } else {
            com.gitb.vs.GetModuleDefinitionResponse response = call("GET", "getModuleDefinition", Optional.empty(), Optional.of(com.gitb.model.vs.GetModuleDefinitionResponse.class))
                    .map(ModelUtils::fromModel)
                    .orElseThrow(() -> new IllegalStateException("Remote service did not return a valid response"));
            ValidationModule result = response.getModule();
            if (cacheResult) {
                if (result == null) result = new ValidationModule();
                serviceModule = result;
            }
            return result;
        }
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs, String stepId) {
        var validateRequest = ValidateRequest.builder()
                .withConfig(configurations.stream().map(ModelUtils::toModel).toArray(com.gitb.model.core.Configuration[]::new))
                .withSessionId(testSessionId)
                .withInput(inputs.entrySet().stream()
                        .map(entry -> ModelUtils.toModel(DataTypeUtils.convertDataTypeToAnyContent(entry.getKey(), entry.getValue())))
                        .toArray(com.gitb.model.core.AnyContent[]::new))
                .build();
        return call("POST", "validate", Optional.of(validateRequest), Optional.of(ValidationResponse.class), stepIdMap(stepId))
                .map(ValidationResponse::getReport)
                .map(ModelUtils::fromModel)
                .orElseThrow(() -> new IllegalStateException("Remote service did not return a valid response"));
    }

    @Override
    public boolean isRemote() {
        return true;
    }
}
