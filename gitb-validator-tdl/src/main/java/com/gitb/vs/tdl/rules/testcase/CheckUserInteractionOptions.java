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

import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.tdl.InstructionOrRequest;
import com.gitb.tdl.UserInteraction;
import com.gitb.tdl.UserRequest;
import com.gitb.vs.tdl.ErrorCode;
import com.gitb.vs.tdl.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

public class CheckUserInteractionOptions extends AbstractTestCaseObserver {

    @Override
    public void handleStep(Object step) {
        super.handleStep(step);
        if (currentStep instanceof UserInteraction interaction) {
            if (interaction.getInstructOrRequest() != null) {
                boolean hasInputRequests = false;
                for (InstructionOrRequest ir : ((UserInteraction) step).getInstructOrRequest()) {
                    if (ir instanceof UserRequest ur) {
                        hasInputRequests = true;
                        if (StringUtils.isBlank(ur.getOptions())) {
                            if (StringUtils.isNotBlank(ur.getOptionLabels())) {
                                addReportItem(ErrorCode.MISSING_INTERACTION_OPTIONS, currentTestCase.getId(), "optionLabels", "optionLabels");
                            }
                            if (StringUtils.isNotBlank(ur.getMultiple())) {
                                addReportItem(ErrorCode.MISSING_INTERACTION_OPTIONS, currentTestCase.getId(), "multiple", "multiple");
                            }
                        } else {
                            if (ur.getContentType() != null && ur.getContentType() != ValueEmbeddingEnumeration.STRING) {
                                addReportItem(ErrorCode.INTERACTION_OPTIONS_FOR_NON_STRING_INPUT, currentTestCase.getId(), ur.getContentType().value());
                            }
                            boolean variableReferenceForOptions = Utils.isVariableExpression(ur.getOptions());
                            if (!variableReferenceForOptions) {
                                int optionCount = StringUtils.countMatches(ur.getOptions(), ',') + 1;
                                if (StringUtils.isNotBlank(ur.getOptionLabels())) {
                                    boolean variableReferenceForLabels = Utils.isVariableExpression(ur.getOptionLabels());
                                    if (!variableReferenceForLabels) {
                                        // Both options and labels are provided as static texts. Check to see that their token counts match.
                                        int labelCount = StringUtils.countMatches(ur.getOptionLabels(), ',') + 1;
                                        if (optionCount != labelCount) {
                                            addReportItem(ErrorCode.INTERACTION_OPTIONS_AND_LABELS_MISMATCH, currentTestCase.getId(), String.valueOf(optionCount), String.valueOf(labelCount));
                                        }
                                    }
                                }
                                if (optionCount == 1) {
                                    addReportItem(ErrorCode.INTERACTION_OPTIONS_SINGLE_OPTION, currentTestCase.getId());
                                }
                            }
                        }
                    }
                }
                if (hasInputRequests && interaction.getBlocking() != null && !interaction.getBlocking().equalsIgnoreCase("true")) {
                    if (Utils.isVariableExpression(interaction.getBlocking())) {
                        addReportItem(ErrorCode.INTERACTION_WITH_INPUTS_MIGHT_BE_NON_BLOCKING, currentTestCase.getId());
                    } else {
                        addReportItem(ErrorCode.INTERACTION_WITH_INPUTS_IS_NON_BLOCKING, currentTestCase.getId());
                    }
                }
            }
            if (Strings.CI.equals("false", interaction.getHandlerEnabled())) {
                // We know we never delegate to a handler.
                if (interaction.getHandler() != null) {
                    addReportItem(ErrorCode.INTERACTION_WITH_UNEXPECTED_HANDLER, currentTestCase.getId());
                }
                if (interaction.getHandlerConfig() != null) {
                    addReportItem(ErrorCode.INTERACTION_WITH_UNEXPECTED_HANDLER_INPUTS, currentTestCase.getId());
                }
            } else {
                // We may delegate to a handler.
                if (interaction.getHandler() == null) {
                    addReportItem(ErrorCode.INTERACTION_WITHOUT_EXPECTED_HANDLER, currentTestCase.getId());
                }
            }
        }
    }

}
