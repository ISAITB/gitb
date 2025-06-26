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
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.ProcessingModule;
import com.gitb.tr.TestResultType;
import com.gitb.types.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@ProcessingHandler(name="CollectionUtils")
public class CollectionUtils extends AbstractProcessingHandler {

    private static final String OPERATION_SIZE = "size";
    private static final String OPERATION_CLEAR = "clear";
    private static final String OPERATION_CONTAINS = "contains";
    private static final String OPERATION_RANDOM_KEY = "randomKey";
    private static final String OPERATION_RANDOM_VALUE = "randomValue";
    private static final String OPERATION_REMOVE = "remove";
    private static final String OPERATION_APPEND = "append";
    private static final String OPERATION_FIND = "find";
    private static final String INPUT_LIST = "list";
    private static final String INPUT_MAP = "map";
    private static final String INPUT_VALUE = "value";
    private static final String INPUT_ITEM = "item";
    private static final String INPUT_TO_LIST = "toList";
    private static final String INPUT_FROM_LIST = "fromList";
    private static final String INPUT_TO_MAP = "toMap";
    private static final String INPUT_FROM_MAP = "fromMap";
    private static final String INPUT_CASE_INSENSITIVE = "ignoreCase";
    private static final String INPUT_ONLY_MISSING = "onlyMissing";
    private static final String OUTPUT_OUTPUT = "output";

    @Override
    public ProcessingModule createProcessingModule() {
        ProcessingModule module = new ProcessingModule();
        module.setId("CollectionUtils");
        module.setMetadata(new Metadata());
        module.getMetadata().setName(module.getId());
        module.getMetadata().setVersion("1.0");
        module.setConfigs(new ConfigurationParameters());
        module.getOperation().add(createProcessingOperation(OPERATION_SIZE,
            List.of(
                    createParameter(INPUT_LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to consider (if the collection is expected to be a list)."),
                    createParameter(INPUT_MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider (if the collection is expected to be a map).")
            ),
            List.of(
                createParameter(OUTPUT_OUTPUT, "number", UsageEnumeration.R, ConfigurationType.SIMPLE, "The number of entries in the collection.")
            )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_CLEAR,
                List.of(
                        createParameter(INPUT_LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to consider (if the collection is expected to be a list)."),
                        createParameter(INPUT_MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider (if the collection is expected to be a map).")
                ),
                Collections.emptyList()
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_CONTAINS,
                List.of(
                        createParameter(INPUT_LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to consider (if the collection is expected to be a list)."),
                        createParameter(INPUT_MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider (if the collection is expected to be a map)."),
                        createParameter(INPUT_VALUE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The value to look for (as an item for a list or as a key for a map)."),
                        createParameter(INPUT_CASE_INSENSITIVE, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether or not the lookup should ignore casing (default is false).")
                ),
                Collections.emptyList()
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_RANDOM_KEY,
                List.of(
                        createParameter(INPUT_MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider.")
                ),
                List.of(
                        createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The selected entry.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_RANDOM_VALUE,
                List.of(
                        createParameter(INPUT_LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to consider (if the collection is expected to be a list)."),
                        createParameter(INPUT_MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider (if the collection is expected to be a map).")
                ),
                List.of(
                        createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The selected entry.")
                )
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_REMOVE,
                List.of(
                        createParameter(INPUT_LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to consider (if the collection is expected to be a list)."),
                        createParameter(INPUT_MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider (if the collection is expected to be a map)."),
                        createParameter(INPUT_ITEM, "string", UsageEnumeration.R, ConfigurationType.SIMPLE, "The item to remove (index if list or key if map).")
                ),
                Collections.emptyList()

        ));
        module.getOperation().add(createProcessingOperation(OPERATION_APPEND,
                List.of(
                        createParameter(INPUT_TO_LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to add to."),
                        createParameter(INPUT_TO_MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to add to."),
                        createParameter(INPUT_FROM_LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to read the entries from."),
                        createParameter(INPUT_FROM_MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to read the entries from."),
                        createParameter(INPUT_ONLY_MISSING, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether or not only missing items should be appended (default is false)."),
                        createParameter(INPUT_CASE_INSENSITIVE, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether or not the missing item matching should ignore casing (default is false).")
                ),
                Collections.emptyList()
        ));
        module.getOperation().add(createProcessingOperation(OPERATION_FIND,
                List.of(
                        createParameter(INPUT_LIST, "list", UsageEnumeration.O, ConfigurationType.SIMPLE, "The list to consider (if the collection is expected to be a list)."),
                        createParameter(INPUT_MAP, "map", UsageEnumeration.O, ConfigurationType.SIMPLE, "The map to consider (if the collection is expected to be a map)."),
                        createParameter(INPUT_VALUE, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The value to look for (as an item for a list or as a key for a map)."),
                        createParameter(INPUT_CASE_INSENSITIVE, "boolean", UsageEnumeration.O, ConfigurationType.SIMPLE, "Whether or not the lookup should ignore casing (default is false).")
                ),
                List.of(
                        createParameter(OUTPUT_OUTPUT, "string", UsageEnumeration.O, ConfigurationType.SIMPLE, "The located entry (if found).")
                )
        ));
        return module;
    }

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData input) {
        if (StringUtils.isBlank(operation)) {
            throw new IllegalArgumentException("No operation provided");
        }
        ProcessingData data = new ProcessingData();
        if (OPERATION_SIZE.equalsIgnoreCase(operation)) {
            int size;
            DataType inputCollection = getInputCollection(input);
            if (inputCollection instanceof MapType) {
                size = ((MapType) inputCollection).getSize();
            } else {
                size = ((ListType) inputCollection).getSize();
            }
            NumberType sizeType = new NumberType();
            sizeType.setValue((double) size);
            data.getData().put(OUTPUT_OUTPUT, sizeType);
        } else if (OPERATION_CLEAR.equalsIgnoreCase(operation)) {
            DataType inputCollection = getInputCollection(input);
            if (inputCollection instanceof MapType) {
                ((MapType) inputCollection).clear();
            } else {
                ((ListType) inputCollection).clear();
            }
        } else if (OPERATION_CONTAINS.equalsIgnoreCase(operation) || OPERATION_FIND.equalsIgnoreCase(operation)) {
            if (!input.getData().containsKey(INPUT_VALUE)) {
                throw new IllegalArgumentException("The value to check for must be provided");
            }
            var value = input.getData().get(INPUT_VALUE);
            var ignoreCaseInput = input.getData().get(INPUT_CASE_INSENSITIVE);
            boolean ignoreCase = false;
            if (ignoreCaseInput != null) {
                ignoreCase = (Boolean) ignoreCaseInput.convertTo(DataType.BOOLEAN_DATA_TYPE).getValue();
            }
            Optional<DataType> locatedValue = Optional.empty();
            DataType inputCollection = getInputCollection(input);
            if (inputCollection instanceof MapType) {
                var valueToCheck = (String) value.convertTo(DataType.STRING_DATA_TYPE).getValue();
                locatedValue = Optional.ofNullable(((MapType) inputCollection).getItem(valueToCheck));
                if (locatedValue.isEmpty() && ignoreCase) {
                    // We do a case-insensitive scan after the direct lookup so that we can find it fast if possible.
                    var items = ((MapType) inputCollection).getItems();
                    locatedValue = items.keySet().stream()
                            .filter(key -> key != null && key.equalsIgnoreCase(valueToCheck))
                            .findFirst()
                            .map(items::get);
                }
            } else {
                var iterator = ((ListType) inputCollection).iterator();
                String convertedLookupValue = null;
                while (iterator.hasNext() && locatedValue.isEmpty()) {
                    var item = iterator.next();
                    var valueToCheck = value.convertTo(item.getType());
                    if (Objects.equals(item.getValue(), valueToCheck.getValue())) {
                        locatedValue = Optional.of(item);
                    } else if (ignoreCase) {
                        var currentItemAsString = (String) item.convertTo(DataType.STRING_DATA_TYPE).getValue();
                        if (convertedLookupValue == null) {
                            convertedLookupValue = (String)valueToCheck.convertTo(DataType.STRING_DATA_TYPE).getValue();
                        }
                        if (currentItemAsString.equalsIgnoreCase(convertedLookupValue)) {
                            locatedValue = Optional.of(item);
                        }
                    }
                }
            }
            var valueFound = locatedValue.isPresent();
            if (OPERATION_CONTAINS.equalsIgnoreCase(operation)) {
                data.getData().put(OUTPUT_OUTPUT, new BooleanType(valueFound));
            } else if (valueFound) {
                data.getData().put(OUTPUT_OUTPUT, locatedValue.get());
            }
        } else if (OPERATION_RANDOM_KEY.equalsIgnoreCase(operation) || OPERATION_RANDOM_VALUE.equalsIgnoreCase(operation)) {
            List<DataType> valueList;
            DataType inputCollection = getInputCollection(input);
            if (inputCollection instanceof MapType) {
                if (OPERATION_RANDOM_KEY.equalsIgnoreCase(operation)) {
                    valueList = ((MapType) inputCollection).getItems().keySet().stream().map(StringType::new).collect(Collectors.toList());
                } else {
                    valueList = new ArrayList<>(((MapType) inputCollection).getItems().values());
                }
            } else {
                valueList = (List<DataType>) inputCollection.getValue();
            }
            if (!valueList.isEmpty()) {
                data.getData().put(OUTPUT_OUTPUT, valueList.get(getRandomNumberInRange(0, valueList.size())));
            }
        } else if (OPERATION_REMOVE.equalsIgnoreCase(operation)) {
            if (!input.getData().containsKey(INPUT_ITEM)) {
                throw new IllegalArgumentException("The item to remove must be provided");
            }
            var item = input.getData().get(INPUT_ITEM);
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
        } else if (OPERATION_APPEND.equalsIgnoreCase(operation)) {
            Pair<ListType, ListType> listPair = null;
            Pair<MapType, MapType> mapPair = null;
            ListType fromList = getInputForName(input, INPUT_FROM_LIST, ListType.class);
            if (fromList != null) {
                ListType toList = getRequiredInputForName(input, INPUT_TO_LIST, ListType.class);
                listPair = Pair.of(fromList, toList);
            }
            if (listPair == null) {
                MapType fromMap = getInputForName(input, INPUT_FROM_MAP, MapType.class);
                if (fromMap != null) {
                    MapType toMap = getRequiredInputForName(input, INPUT_TO_MAP, MapType.class);
                    mapPair = Pair.of(fromMap, toMap);
                }
            }
            if (listPair == null && mapPair == null) {
                throw new IllegalArgumentException("You must provide either [%s] and [%s] inputs, or [%s] and [%s] inputs for the [%s] operation".formatted(INPUT_FROM_LIST, INPUT_TO_LIST, INPUT_FROM_MAP, INPUT_TO_MAP, operation));
            }
            boolean onlyMissing = Optional.ofNullable(getInputForName(input, INPUT_ONLY_MISSING, BooleanType.class))
                    .map(flag -> (Boolean) flag.getValue())
                    .orElse(false);
            boolean caseInsensitive = Optional.ofNullable(getInputForName(input, INPUT_CASE_INSENSITIVE, BooleanType.class))
                    .map(flag -> (Boolean) flag.getValue())
                    .orElse(false);
            if (listPair != null) {
                List<DataType> toList = (List<DataType>) listPair.getRight().getValue();
                Function<DataType, Boolean> addCheck;
                if (onlyMissing) {
                    if (caseInsensitive) {
                        Set<String> toListValues = toList.stream().map(value -> ((String) value.convertTo(DataType.STRING_DATA_TYPE).getValue()).toLowerCase()).collect(Collectors.toSet());
                        addCheck = (item) -> {
                            String itemValue = ((String) item.convertTo(DataType.STRING_DATA_TYPE).getValue()).toLowerCase();
                            boolean shouldAdd = !toListValues.contains(itemValue);
                            if (shouldAdd) {
                                toListValues.add(itemValue); // Make sure we don't add duplicates coming from the fromList.
                            }
                            return shouldAdd;
                        };
                    } else {
                        Set<String> toListValues = toList.stream().map(value -> (String) value.convertTo(DataType.STRING_DATA_TYPE).getValue()).collect(Collectors.toSet());
                        addCheck = (item) -> {
                            String itemValue = (String) item.convertTo(DataType.STRING_DATA_TYPE).getValue();
                            boolean shouldAdd = !toListValues.contains(itemValue);
                            if (shouldAdd) {
                                toListValues.add(itemValue); // Make sure we don't add duplicates coming from the fromList.
                            }
                            return shouldAdd;
                        };
                    }
                } else {
                    addCheck = (item) -> true;
                }
                for (DataType fromItem: (List<DataType>) listPair.getLeft().getValue()) {
                    if (addCheck.apply(fromItem)) {
                        toList.add(fromItem);
                    }
                }
            } else {
                Map<String, DataType> toMap = (Map<String, DataType>) mapPair.getRight().getValue();
                Function<String, Boolean> addCheck;
                if (onlyMissing) {
                    if (caseInsensitive) {
                        // Create string representation of toMap's keys
                        Set<String> toKeys = toMap.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
                        addCheck = (itemKey) -> {
                            String itemKeyValue = itemKey.toLowerCase();
                            boolean shouldAdd = !toKeys.contains(itemKeyValue);
                            if (shouldAdd) {
                                toKeys.add(itemKeyValue);
                            }
                            return shouldAdd;
                        };
                    } else {
                        Set<String> toKeys = new HashSet<>(toMap.keySet());
                        addCheck = (itemKey) -> {
                            boolean shouldAdd = !toKeys.contains(itemKey);
                            if (shouldAdd) {
                                toKeys.add(itemKey);
                            }
                            return shouldAdd;
                        };
                    }
                } else {
                    addCheck = (itemKey) -> true;
                }
                for (Map.Entry<String, DataType> fromEntry: ((Map<String, DataType>) mapPair.getLeft().getValue()).entrySet()) {
                    if (addCheck.apply(fromEntry.getKey())) {
                        toMap.put(fromEntry.getKey(), fromEntry.getValue());
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown operation [%s]".formatted(operation));
        }
        return new ProcessingReport(createReport(TestResultType.SUCCESS), data);
    }

    private DataType getInputCollection(ProcessingData input) {
        DataType inputCollection = null;
        if (input.getData() != null) {
            if (input.getData().containsKey(INPUT_LIST) && input.getData().containsKey(INPUT_MAP)) {
                throw new IllegalArgumentException("Either a list or map should be provided but not both");
            } else if (input.getData().containsKey(INPUT_LIST)) {
                inputCollection = input.getData().get(INPUT_LIST);
                if (!(inputCollection instanceof ListType)) {
                    throw new IllegalArgumentException("A list was provided as a map input");
                }
            } else if (input.getData().containsKey(INPUT_MAP)) {
                inputCollection = input.getData().get(INPUT_MAP);
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
        return ThreadLocalRandom.current().nextInt(max - min) + min;
    }
}
