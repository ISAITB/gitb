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
    public ProcessingModule getModuleDefinition() {
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
        var report = createReport(TestResultType.SUCCESS);
        report.getContext().getItem().add(DataTypeUtils.convertDataTypeToAnyContent("duration", duration));
        return new DeferredProcessingReport(report, new ProcessingData(), duration.longValue());
    }
}
