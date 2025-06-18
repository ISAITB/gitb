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

import com.gitb.core.ConfigurationParameters;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.processing.DeferredProcessingReport;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.NumberType;
import com.gitb.utils.DataTypeUtils;

import java.util.Collections;
import java.util.List;

@ProcessingHandler(name="DelayProcessor")
public class DelayProcessor extends AbstractProcessingHandler {

    private static final String OPERATION__DELAY = "delay";
    private static final String INPUT__DURATION = "duration";

    @Override
    public ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("DelayProcessor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION__DELAY,
                List.of(createParameter(INPUT__DURATION, "number", UsageEnumeration.R, ConfigurationType.SIMPLE, "A duration in milliseconds to delay for.")),
                Collections.emptyList()
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        var duration = getInputForName(input, INPUT__DURATION, NumberType.class);
        if (duration == null) {
            throw new IllegalArgumentException("The duration to delay for is required");
        }
        return new DeferredProcessingReport(createReport(TestResultType.SUCCESS), new ProcessingData(), duration.longValue());
    }
}
