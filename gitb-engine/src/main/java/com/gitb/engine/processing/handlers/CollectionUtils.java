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
import com.gitb.types.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

@ProcessingHandler(name="CollectionUtils")
public class CollectionUtils extends AbstractProcessingHandler {

    private static final String OPERATION__SIZE = "size";
    private static final String OPERATION__CLEAR = "clear";
    private static final String OPERATION__CONTAINS = "contains";
    private static final String OPERATION__RANDOM_KEY = "randomKey";
    private static final String OPERATION__RANDOM_VALUE = "randomValue";
    private static final String OPERATION__REMOVE = "remove";
    private static final String OPERATION__APPEND = "append";
    private static final String INPUT__LIST = "list";
    private static final String INPUT__MAP = "map";
    private static final String INPUT__VALUE = "value";
    private static final String INPUT__ITEM = "item";
    private static final String INPUT__TO_LIST = "toList";
    private static final String INPUT__FROM_LIST = "fromList";
    private static final String INPUT__TO_MAP = "toMap";
    private static final String INPUT__FROM_MAP = "fromMap";
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
        module.getOperation().add(createProcessingOperation(OPERATION__CONTAINS,
                List.of(
                        createParameter(INPUT__LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to consider (if the collection is expected to be a list)."),
                        createParameter(INPUT__MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider (if the collection is expected to be a map)."),
                        createParameter(INPUT__VALUE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The value to look for (as an item for a list or as a key for a map).")
                ),
                Collections.emptyList()
        ));
        module.getOperation().add(createProcessingOperation(OPERATION__RANDOM_KEY,
                List.of(
                        createParameter(INPUT__MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider.")
                ),
                List.of(
                        createParameter(OUTPUT__OUTPUT, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The selected entry.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION__RANDOM_VALUE,
                List.of(
                        createParameter(INPUT__LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to consider (if the collection is expected to be a list)."),
                        createParameter(INPUT__MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider (if the collection is expected to be a map).")
                ),
                List.of(
                        createParameter(OUTPUT__OUTPUT, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The selected entry.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION__REMOVE,
                List.of(
                        createParameter(INPUT__LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to consider (if the collection is expected to be a list)."),
                        createParameter(INPUT__MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider (if the collection is expected to be a map)."),
                        createParameter(INPUT__ITEM, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The item to remove (index if list or key if map).")
                ),
                Collections.emptyList()

        ));
        module.getOperation().add(createProcessingOperation(OPERATION__APPEND,
                List.of(
                        createParameter(INPUT__TO_LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to add to."),
                        createParameter(INPUT__TO_MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to add to."),
                        createParameter(INPUT__FROM_LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to read the entries from."),
                        createParameter(INPUT__FROM_MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to read the entries from.")
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
        ProcessingData data = new ProcessingData();
        if (OPERATION__SIZE.equalsIgnoreCase(operation)) {
            int size;
            DataType inputCollection = getInputCollection(input);
            if (inputCollection instanceof MapType) {
                size = ((MapType) inputCollection).getSize();
            } else {
                size = ((ListType) inputCollection).getSize();
            }
            NumberType sizeType = new NumberType();
            sizeType.setValue((double) size);
            data.getData().put(OUTPUT__OUTPUT, sizeType);
        } else if (OPERATION__CLEAR.equalsIgnoreCase(operation)) {
            DataType inputCollection = getInputCollection(input);
            if (inputCollection instanceof MapType) {
                ((MapType) inputCollection).clear();
            } else {
                ((ListType) inputCollection).clear();
            }
        } else if (OPERATION__CONTAINS.equalsIgnoreCase(operation)) {
            if (!input.getData().containsKey(INPUT__VALUE)) {
                throw new IllegalArgumentException("The value to check for must be provided");
            }
            var value = input.getData().get(INPUT__VALUE);
            var contains = false;
            DataType inputCollection = getInputCollection(input);
            if (inputCollection instanceof MapType) {
                var valueToCheck = value.convertTo(DataType.STRING_DATA_TYPE);
                var locatedItem = ((MapType) inputCollection).getItem((String) valueToCheck.getValue());
                contains = locatedItem != null;
            } else {
                var iterator = ((ListType) inputCollection).iterator();
                while (iterator.hasNext() && !contains) {
                    var item = iterator.next();
                    var valueToCheck = value.convertTo(item.getType());
                    if (Objects.equals(item.getValue(), valueToCheck.getValue())) {
                        contains = true;
                    }
                }
            }
            data.getData().put(OUTPUT__OUTPUT, new BooleanType(contains));
        } else if (OPERATION__RANDOM_KEY.equalsIgnoreCase(operation) || OPERATION__RANDOM_VALUE.equalsIgnoreCase(operation)) {
            List<DataType> valueList;
            DataType inputCollection = getInputCollection(input);
            if (inputCollection instanceof MapType) {
                if (OPERATION__RANDOM_KEY.equalsIgnoreCase(operation)) {
                    valueList = ((MapType) inputCollection).getItems().keySet().stream().map(StringType::new).collect(Collectors.toList());
                } else {
                    valueList = new ArrayList<>(((MapType) inputCollection).getItems().values());
                }
            } else {
                valueList = (List<DataType>) inputCollection.getValue();
            }
            if (!valueList.isEmpty()) {
                data.getData().put(OUTPUT__OUTPUT, valueList.get(getRandomNumberInRange(0, valueList.size())));
            }
        } else if (OPERATION__REMOVE.equalsIgnoreCase(operation)) {
            if (!input.getData().containsKey(INPUT__ITEM)) {
                throw new IllegalArgumentException("The item to remove must be provided");
            }
            var item = input.getData().get(INPUT__ITEM);
            DataType inputCollection = getInputCollection(input);
            if (inputCollection instanceof MapType) {
                ((MapType) inputCollection).getItems().remove(item.convertTo(DataType.STRING_DATA_TYPE).getValue());
            } else {
                var items = (List<DataType>) inputCollection.getValue();
                var itemToRemove = ((Double) item.convertTo(DataType.NUMBER_DATA_TYPE).getValue()).intValue();
                if (itemToRemove >= items.size()) {
                    itemToRemove = items.size() - 1;
                } else if (itemToRemove < 0) {
                    itemToRemove = 0;
                }
                items.remove(itemToRemove);
            }
        } else if (OPERATION__APPEND.equalsIgnoreCase(operation)) {
            Pair<ListType, ListType> listPair = null;
            Pair<MapType, MapType> mapPair = null;
            ListType fromList = getInputForName(input, INPUT__FROM_LIST, ListType.class);
            if (fromList != null) {
                ListType toList = getRequiredInputForName(input, INPUT__TO_LIST, ListType.class);
                listPair = Pair.of(fromList, toList);
            }
            if (listPair == null) {
                MapType fromMap = getInputForName(input, INPUT__FROM_MAP, MapType.class);
                if (fromMap != null) {
                    MapType toMap = getRequiredInputForName(input, INPUT__TO_MAP, MapType.class);
                    mapPair = Pair.of(fromMap, toMap);
                }
            }
            if (listPair == null && mapPair == null) {
                throw new IllegalArgumentException("You must provide either [%s] and [%s] inputs, or [%s] and [%s] inputs for the [%s] operation".formatted(INPUT__FROM_LIST, INPUT__TO_LIST, INPUT__FROM_MAP, INPUT__TO_MAP, operation));
            }
            if (listPair != null) {
                ((List<DataType>)listPair.getRight().getValue()).addAll(((List<DataType>)listPair.getLeft().getValue()));
            } else {
                ((Map<String, DataType>)mapPair.getRight().getValue()).putAll(((Map<String, DataType>)mapPair.getLeft().getValue()));
            }
        } else {
            throw new IllegalArgumentException("Unknown operation [%s]".formatted(operation));
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private DataType getInputCollection(ProcessingData input) {
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
        return inputCollection;
    }

    private int getRandomNumberInRange(int min, int max) {
        var random = new Random();
        return random.nextInt(max - min) + min;
    }
}
