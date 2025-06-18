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

package com.gitb.processing;

import com.gitb.StepHandler;
import com.gitb.core.Configuration;
import com.gitb.ps.ProcessingModule;

import java.util.List;

public interface IProcessingHandler extends StepHandler {

    ProcessingModule getModuleDefinition();

    String beginTransaction(String stepId, List<Configuration> config);

    ProcessingReport process(String session, String stepId, String operation, ProcessingData input);

    void endTransaction(String session, String stepId);

}
