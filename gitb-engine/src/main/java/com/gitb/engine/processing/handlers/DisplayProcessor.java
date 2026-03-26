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

package com.gitb.engine.processing.handlers;

import com.gitb.core.ConfigurationParameters;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.UsageEnumeration;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.engine.utils.HandlerUtils;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.*;
import com.gitb.utils.DataTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.gitb.engine.processing.handlers.DisplayProcessor.HANDLER_NAME;

@ProcessingHandler(name=HANDLER_NAME)
public class DisplayProcessor extends AbstractProcessingHandler {

    public static final String HANDLER_NAME = "DisplayProcessor";
    private static final Logger LOG = LoggerFactory.getLogger(DisplayProcessor.class);
    private static final String OPERATION_DISPLAY = "display";
    private static final String INPUT_PARAMETERS = "parameters";
    private static final String INPUT_CONTENT_TYPES = "contentTypes";
    private static final String INPUT_REPORT_ITEMS = "reportItems";
    public static final String INPUT_REPORT_STEPS = "reportSteps";
    private static final String INPUT_RESULT = "result";
    private static final String INPUT_SORT_REPORT_BY_SEVERITY = "sortReportBySeverity";

    @Override
    public ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId(HANDLER_NAME);
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION_DISPLAY,
                List.of(
                        createParameter(INPUT_RESULT, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, String.format("The result of the step. On of '%s', '%s' or '%s'. If not specified the default considered is '%s'.", TestResultType.SUCCESS, TestResultType.WARNING, TestResultType.WARNING, TestResultType.SUCCESS)),
                        createParameter(INPUT_PARAMETERS, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map of input parameters to display."),
                        createParameter(INPUT_CONTENT_TYPES, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map of content types to apply for the display of matching input parameters."),
                        createParameter(INPUT_REPORT_ITEMS, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map of report items to display as a detailed validation report."),
                        createParameter(INPUT_REPORT_STEPS, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list of step identifiers from which to source the current step's detailed validation report."),
                        createParameter(INPUT_SORT_REPORT_BY_SEVERITY, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether report items should be sorted based on severity first and then location (as opposed to location first).")
                ),
                Collections.emptyList()
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        var result = TestResultType.SUCCESS;
        var resultInput = getInputForName(input, INPUT_RESULT, StringType.class);
        var hasResult = resultInput != null;
        boolean sortReportBySeverity = Optional.ofNullable(getAndConvert(input.getData(), INPUT_SORT_REPORT_BY_SEVERITY, DataType.BOOLEAN_DATA_TYPE, BooleanType.class)).map(BooleanType::getValue).orElse(false);
        if (hasResult) {
            try {
                result = TestResultType.valueOf((String)(resultInput.convertTo(DataType.STRING_DATA_TYPE).getValue()));
            } catch (IllegalArgumentException | NullPointerException e) {
                LOG.warn(MarkerFactory.getDetachedMarker(session), String.format("Invalid value for input '%s'. Considering '%s' by default.", INPUT_RESULT, TestResultType.SUCCESS));
            }
        }
        var parameters = getInputForName(input, INPUT_PARAMETERS, MapType.class);
        var report = createReport(result);
        if (parameters != null) {
            parameters.getItems().forEach((key, value) -> {
                var item = DataTypeUtils.convertDataTypeToAnyContent(key, value);
                item.setForDisplay(true);
                item.setForContext(false);
                report.getContext().getItem().add(item);
            });
        }
        TestCaseUtils.applyContentTypes(input.getData().get(INPUT_CONTENT_TYPES), report.getContext());
        HandlerUtils.addReportStepMapToReport(getInputForName(input, INPUT_REPORT_STEPS, ListType.class), report, !hasResult,  getScope(session), session);
        HandlerUtils.addReportItemMapToReport(getInputForName(input, INPUT_REPORT_ITEMS, MapType.class), report, !hasResult, Optional.of(objectFactory));
        sortReport(report, !sortReportBySeverity);
        return new ProcessingReport(report, new ProcessingData());
    }
}
