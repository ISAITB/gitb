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

package com.gitb.validation;


import com.gitb.StepHandler;
import com.gitb.core.Configuration;
import com.gitb.core.ValidationModule;
import com.gitb.tr.TestStepReportType;
import com.gitb.types.DataType;

import java.util.List;
import java.util.Map;

/**
 * Created by tuncay on 9/2/14.
 */
public interface IValidationHandler extends StepHandler {

	/**
	 * Returns the validation module definition
	 * @return module definition
	 */
	ValidationModule getModuleDefinition();

    /**
     * Validates the content with given validator
     * @param inputs
     * @return
     */
    TestStepReportType validate(List<Configuration> configurations, Map<String, DataType> inputs, String stepId);

}
