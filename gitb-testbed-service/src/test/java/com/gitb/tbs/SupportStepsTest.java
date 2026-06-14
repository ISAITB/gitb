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
                "sup-multitype-pipeline"
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
}
