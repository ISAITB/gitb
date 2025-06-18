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

package com.gitb.engine.validation.handlers.common;

import com.gitb.core.Configuration;
import com.gitb.core.ValidationModule;
import com.gitb.engine.AbstractHandler;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;
import com.gitb.utils.XMLUtils;
import com.gitb.validation.IValidationHandler;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by senan on 24.10.2014.
 */
public abstract class AbstractValidator extends AbstractHandler implements IValidationHandler {

    public static final String TEST_CASE_ID_INPUT = "com.gitb.TestCaseID";

    protected ValidationModule validatorDefinition;

    @Override
    public ValidationModule getModuleDefinition() {
        return this.validatorDefinition;
    }

    @Override
    public TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs, String stepId) {
        // Ignore the step ID for embedded validators.
        return validate(configurations, inputs);
    }

    protected static ValidationModule readModuleDefinition(String fileName) {
        try {
            ValidationModule module = null;
            InputStream resource = AbstractValidator.class.getResourceAsStream(fileName);
            if (resource != null) {
                module = XMLUtils.unmarshal(ValidationModule.class, new StreamSource(resource));
            }
            return module;
        } catch (Exception e) {
            throw new GITBEngineInternalError(e);
        }
    }

    protected String getTestCaseId(Map<String, DataType> inputs) {
        return (String) inputs.get(TEST_CASE_ID_INPUT).getValue();
    }

    public abstract TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs);

}
