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

package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.*;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.ContainerTypeInfo;
import com.gitb.vs.tdl.util.Utils;

public class CheckDataTypes extends AbstractTestCaseObserver {

    @Override
    public void handleImport(Object artifactObj) {
        super.handleImport(artifactObj);
        if (artifactObj instanceof TestArtifact) {
            checkDataType(((TestArtifact)artifactObj).getType());
        }
    }

    @Override
    public void handleInputParameter(InputParameter param) {
        handleVariable(param);
    }

    @Override
    public void handleVariable(Variable var) {
        checkDataType(var.getType());
        if (var.getValue() != null && var.getType() != null) {
            if ("map".equals(var.getType())) {
                for (NamedTypedString binding: var.getValue()) {
                    if (binding.getName() == null || binding.getType() == null) {
                        addReportItem(ErrorCode.VALUE_OF_MAP_VARIABLE_WITHOUT_NAME_OR_TYPE, currentTestCase.getId(), var.getName());
                    }
                    checkDataType(binding.getType());
                }
            } else if (var.getType().startsWith("list")) {
                for (NamedTypedString binding: var.getValue()) {
                    if (binding.getName() != null || binding.getType() != null) {
                        addReportItem(ErrorCode.VALUE_OF_NON_MAP_VARIABLE_WITH_NAME_OR_TYPE, currentTestCase.getId(), var.getName());
                    }
                }
            } else {
                if (var.getValue().size() > 1) {
                    addReportItem(ErrorCode.MULTIPLE_VALUES_FOR_PRIMITIVE_VARIABLE, currentTestCase.getId(), var.getName());
                } else if (var.getValue().size() == 1) {
                    if (var.getValue().get(0).getName() != null || var.getValue().get(0).getType() != null) {
                        addReportItem(ErrorCode.VALUE_OF_NON_MAP_VARIABLE_WITH_NAME_OR_TYPE, currentTestCase.getId(), var.getName());
                    }
                }
            }
        }
    }

    @Override
    public void handleStep(Object step) {
        super.handleStep(step);
        if (step instanceof UserInteraction) {
            if (((UserInteraction) step).getInstructOrRequest() != null) {
                for (InstructionOrRequest ir : ((UserInteraction) step).getInstructOrRequest()) {
                    checkDataType(ir.getType());
                }
            }
        } else if (step instanceof Assign) {
            checkDataType(((Assign)step).getType());
        }
    }

    @Override
    public void handleOutput(Binding binding) {
        if (binding instanceof TypedBinding) {
            checkDataType(((TypedBinding)binding).getType());
        }
    }

    private void checkDataType(String dataType) {
        if (dataType != null) {
            boolean isDataType = context.getExternalConfiguration().getDataTypes().contains(dataType);
            if (!isDataType) {
                boolean isContainer = Utils.isContainerType(dataType, context.getExternalConfiguration().getContainerDataTypes(), context.getExternalConfiguration().getContainedDataTypes());
                if (!isContainer) {
                    addReportItem(ErrorCode.INVALID_DATA_TYPE_REFERENCE, currentTestCase.getId(), dataType);
                } else {
                    // Check for a list that has no subtype.
                    ContainerTypeInfo typeInfo = Utils.getContainerTypeParts(dataType);
                    if ("list".equals(typeInfo.getContainerType()) && typeInfo.getContainedType() == null) {
                        addReportItem(ErrorCode.MISSING_LIST_CONTAINED_TYPE, currentTestCase.getId());
                    }
                }
            }
        }
    }
}
