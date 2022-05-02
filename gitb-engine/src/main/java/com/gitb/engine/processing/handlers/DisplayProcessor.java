package com.gitb.engine.processing.handlers;

import com.gitb.core.ConfigurationParameters;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.MapType;
import com.gitb.utils.DataTypeUtils;

import java.util.Collections;
import java.util.List;

@ProcessingHandler(name="DisplayProcessor")
public class DisplayProcessor extends AbstractProcessingHandler {

    private static final String OPERATION__DISPLAY = "display";
    private static final String INPUT__PARAMETERS = "parameters";

    @Override
    public ProcessingModule getModuleDefinition() {
        ProcessingModule module = new ProcessingModule();
        module.setId("DisplayProcessor");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION__DISPLAY,
                List.of(createParameter(INPUT__PARAMETERS, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map of input parameters to display.")),
                Collections.emptyList()
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        var parameters = getInputForName(input, INPUT__PARAMETERS, MapType.class);
        if (parameters == null) {
            parameters = getDefaultInput(input, MapType.class);
        }
        var report = createReport(TestResultType.SUCCESS);
        if (parameters != null) {
            parameters.getItems().forEach((key, value) -> {
                var item = DataTypeUtils.convertDataTypeToAnyContent(key, value);
                item.setForDisplay(true);
                item.setForContext(false);
                report.getContext().getItem().add(item);
            });
        }
        return new ProcessingReport(report, new ProcessingData());
    }
}
