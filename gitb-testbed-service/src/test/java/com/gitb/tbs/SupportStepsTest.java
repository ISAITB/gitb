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

package com.gitb.tbs;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.gitb.tbs.TdlTestHelper.*;

class SupportStepsTest extends BaseIntegrationTest {

    @BeforeAll
    static void stubAll() {
        for (String id : new String[]{
                "sup-assign-string",
                "sup-assign-number",
                "sup-assign-boolean",
                "sup-assign-expression",
                "sup-assign-type-convert",
                "sup-log-message",
                "sup-group-steps",
                "sup-group-hidden",
                "sup-call-basic",
                "sup-call-outputs",
                "sup-call-optional-param",
                "sup-call-nested",
                "sup-level-warning",
                "sup-mixed-levels",
                "sup-complex-scenario",
                "sup-multitype-pipeline",
                "sup-interact-dropdown-multiple",
                "sup-interact-text-multiple",
                "sup-interact-dependency-multiple",
                "sup-interact-multiple-values",
                "sup-interact-dependency-values",
                "sup-interact-required-values"
        }) {
            stubTdl(id, "tdl/sup/" + id + ".xml");
        }
    }

    @Test
    void assignString() throws Exception {
        assertSuccess(run("sup-assign-string"));
    }

    @Test
    void assignNumber() throws Exception {
        assertSuccess(run("sup-assign-number"));
    }

    @Test
    void assignBoolean() throws Exception {
        assertSuccess(run("sup-assign-boolean"));
    }

    @Test
    void assignExpression() throws Exception {
        assertSuccess(run("sup-assign-expression"));
    }

    @Test
    void assignTypeConvert() throws Exception {
        assertSuccess(run("sup-assign-type-convert"));
    }

    @Test
    void logMessage() throws Exception {
        assertSuccess(run("sup-log-message"));
    }

    @Test
    void groupSteps() throws Exception {
        assertSuccess(run("sup-group-steps"));
    }

    @Test
    void groupHidden() throws Exception {
        assertSuccess(run("sup-group-hidden"));
    }

    @Test
    void callBasic() throws Exception {
        assertSuccess(run("sup-call-basic"));
    }

    @Test
    void callOutputs() throws Exception {
        assertSuccess(run("sup-call-outputs"));
    }

    @Test
    void callOptionalParam() throws Exception {
        assertSuccess(run("sup-call-optional-param"));
    }

    @Test
    void callNested() throws Exception {
        assertSuccess(run("sup-call-nested"));
    }

    @Test
    void levelWarning() throws Exception {
        assertSuccess(run("sup-level-warning"));
    }

    @Test
    void mixedLevels() throws Exception {
        assertFailed(run("sup-mixed-levels"));
    }

    @Test
    void complexScenario() throws Exception {
        assertSuccess(run("sup-complex-scenario"));
    }

    @Test
    void multitypePipeline() throws Exception {
        assertSuccess(run("sup-multitype-pipeline"));
    }

    @Test
    void interactDropdownMultipleIsCommaSeparatedString() throws Exception {
        assertSuccess(runWithInteractions("sup-interact-dropdown-multiple", request -> List.of(
                userInput(1, "implicit", "a,b"),
                userInput(2, "explicit", "a,c"),
                userInput(3, "bound", "b,c"),
                userInput(4, "boundExplicit", "a,b,c")
        )));
    }

    @Test
    void interactTextMultipleIsList() throws Exception {
        assertSuccess(runWithInteractions("sup-interact-text-multiple", request -> List.of(
                userInput(1, "text", "x"),
                userInput(2, "boundString", "y"),
                userInput(3, "boundList", "z")
        )));
    }

    @Test
    void interactDependencyOnMultipleSelectionMatchesAnySelectedValue() throws Exception {
        // "b" is among the selected values so the dependent (required) inputs apply and are provided.
        assertSuccess(runWithInteractions("sup-interact-dependency-multiple", request -> List.of(
                userInput(1, "implicit", "a,b"),
                userInput(2, "implicitDependent", "x"),
                userInput(3, "explicit", "b,c"),
                userInput(4, "explicitDependent", "y")
        )));
    }

    @Test
    void interactDependencyOnMultipleSelectionRequiresDependentInput() throws Exception {
        // "b" is among the selected values so the dependent inputs are required, but are not provided.
        assertFailed(runWithInteractions("sup-interact-dependency-multiple", request -> List.of(
                userInput(1, "implicit", "a,b"),
                userInput(3, "explicit", "b,c")
        )));
    }

    @Test
    void interactDependencyOnMultipleSelectionIgnoredWhenValueNotSelected() throws Exception {
        // "b" is not selected so the dependent (required) inputs do not apply and can be omitted.
        assertSuccess(runWithInteractions("sup-interact-dependency-multiple", request -> List.of(
                userInput(1, "implicit", "a,c"),
                userInput(3, "explicit", "c")
        )));
    }

    @Test
    void interactMultipleValuesForTextBasedInputs() throws Exception {
        assertSuccess(runWithInteractions("sup-interact-multiple-values", request -> List.of(
                userInput(1, "text", "t1"), userInput(1, "text", "t2"),
                userInput(2, "secret", "s1"), userInput(2, "secret", "s2"),
                userInput(3, "multiline", "m1"), userInput(3, "multiline", "m2"),
                userInput(4, "code", "c1"), userInput(4, "code", "c2"),
                userInput(5, "rich", "<p>r1</p>"), userInput(5, "rich", "<p>r2</p>"),
                userInput(6, "dependent", "d")
        )));
    }

    @Test
    void interactDependencyOnTextWithMultipleValuesMatchesAnyValue() throws Exception {
        // "t2" is one of the text values so the (required) dependent input applies, but is not provided.
        assertFailed(runWithInteractions("sup-interact-dependency-values", request -> List.of(
                userInput(1, "text", "t1"), userInput(1, "text", "t2")
        )));
    }

    @Test
    void interactDependencyOnTextWithMultipleValuesIgnoredWhenNoValueMatches() throws Exception {
        assertSuccess(runWithInteractions("sup-interact-dependency-values", request -> List.of(
                userInput(1, "text", "t1"), userInput(1, "text", "t3")
        )));
    }

    @Test
    void interactDependencyOnRichTextMatchesSanitizedValue() throws Exception {
        // The second value only equals the expected one once sanitized, so the (required) dependent input applies, but is not provided.
        assertFailed(runWithInteractions("sup-interact-dependency-values", request -> List.of(
                userInput(3, "rich", "<p>r1</p>"), userInput(3, "rich", "<p>r2</p><script>alert(1)</script>")
        )));
    }

    @Test
    void interactDependencyOnRichTextWithMultipleValuesIgnoredWhenNoValueMatches() throws Exception {
        assertSuccess(runWithInteractions("sup-interact-dependency-values", request -> List.of(
                userInput(3, "rich", "<p>r1</p>"), userInput(3, "rich", "<p>r3</p>")
        )));
    }

    @Test
    void interactRequiredInputWithValuesIsSatisfied() throws Exception {
        assertSuccess(runWithInteractions("sup-interact-required-values", request -> List.of(
                userInput(1, "single", " x "), userInput(2, "multiple", "y")
        )));
    }

    @Test
    void interactRequiredInputIsNotSatisfiedByWhitespaceOnlyValue() throws Exception {
        assertFailed(runWithInteractions("sup-interact-required-values", request -> List.of(
                userInput(1, "single", "   "), userInput(2, "multiple", "y")
        )));
    }

    @Test
    void interactRequiredInputWithMultipleValuesIsNotSatisfiedByWhitespaceOnlyValues() throws Exception {
        assertFailed(runWithInteractions("sup-interact-required-values", request -> List.of(
                userInput(1, "single", "x"), userInput(2, "multiple", " "), userInput(2, "multiple", "	")
        )));
    }
}
