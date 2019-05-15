package com.gitb.vs.tdl.rules.testcase;

import com.gitb.tdl.*;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.ContainerTypeInfo;
import com.gitb.vs.tdl.util.Utils;

public class CheckDataTypes extends AbstractTestCaseObserver {

    @Override
    public void handleImport(Object artifactObj) {
        if (artifactObj instanceof TestArtifact) {
            checkDataType(((TestArtifact)artifactObj).getType());
        }
    }

    @Override
    public void handleVariable(Variable var) {
        checkDataType(var.getType());
        if (var.getValue() != null) {
            for (TypedBinding binding: var.getValue()) {
                checkDataType(binding.getType());
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
