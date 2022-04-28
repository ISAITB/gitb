package com.gitb.engine.processing.handlers;

import com.gitb.core.ConfigurationParameters;
import com.gitb.core.ConfigurationType;
import com.gitb.core.Metadata;
import com.gitb.core.UsageEnumeration;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.DataType;
import com.gitb.types.ListType;
import com.gitb.types.MapType;
import com.gitb.types.NumberType;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;

import java.util.Collections;
import java.util.List;

@MetaInfServices(IProcessingHandler.class)
public class CollectionUtils extends AbstractProcessingHandler {

    private static final String OPERATION__SIZE = "size";
    private static final String OPERATION__CLEAR = "clear";
    private static final String INPUT__LIST = "list";
    private static final String INPUT__MAP = "map";
    private static final String OUTPUT__OUTPUT = "output";

    @Override
    public ProcessingModule getModuleDefinition() {
        ProcessingModule module = new ProcessingModule();
        module.setId("CollectionUtils");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION__SIZE,
            List.of(
                    createParameter(INPUT__LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to consider (if the collection is expected to be a list)."),
                    createParameter(INPUT__MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider (if the collection is expected to be a map).")
            ),
            List.of(
                createParameter(OUTPUT__OUTPUT, "number", UsageEnumeration.R, ConfigurationType.SIMPLE, "The number of entries in the collection.")
            )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION__CLEAR,
                List.of(
                        createParameter(INPUT__LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to consider (if the collection is expected to be a list)."),
                        createParameter(INPUT__MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider (if the collection is expected to be a map).")
                ),
                Collections.emptyList()
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        if (StringUtils.isBlank(operation)) {
            throw new IllegalArgumentException("No operation provided");
        }
        DataType inputCollection = null;
        if (input.getData() != null) {
            if (input.getData().containsKey(INPUT__LIST) && input.getData().containsKey(INPUT__MAP)) {
                throw new IllegalArgumentException("Either a list or map should be provided but not both");
            } else if (input.getData().containsKey(INPUT__LIST)) {
                inputCollection = input.getData().get(INPUT__LIST);
                if (!(inputCollection instanceof ListType)) {
                    throw new IllegalArgumentException("A list was provided as a map input");
                }
            } else if (input.getData().containsKey(INPUT__MAP)) {
                inputCollection = input.getData().get(INPUT__MAP);
                if (!(inputCollection instanceof MapType)) {
                    throw new IllegalArgumentException("A map was provided as a list input");
                }
            }
        }
        if (inputCollection == null) {
            throw new IllegalArgumentException("Either a list or map should be provided as input");
        }
        ProcessingData data = new ProcessingData();
        if (OPERATION__SIZE.equalsIgnoreCase(operation)) {
            int size;
            if (inputCollection instanceof MapType) {
                size = ((MapType) inputCollection).getSize();
            } else {
                size = ((ListType) inputCollection).getSize();
            }
            NumberType sizeType = new NumberType();
            sizeType.setValue((double) size);
            data.getData().put(OUTPUT__OUTPUT, sizeType);
        } else if (OPERATION__CLEAR.equalsIgnoreCase(operation)) {
            if (inputCollection instanceof MapType) {
                ((MapType) inputCollection).clear();
            } else {
                ((ListType) inputCollection).clear();
            }
        } else {
            throw new IllegalArgumentException("Unknown operation [" + operation + "]");
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

}
