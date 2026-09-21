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

class ControlFlowTest extends BaseIntegrationTest {

    @BeforeAll
    static void stubAll() {
        for (String id : new String[]{
                "cf-if-true",
                "cf-if-false",
                "cf-if-no-else",
                "cf-while-basic",
                "cf-while-zero",
                "cf-repuntil-basic",
                "cf-repuntil-first",
                "cf-foreach-list",
                "cf-foreach-empty",
                "cf-flow-parallel",
                "cf-exit-success",
                "cf-exit-failure",
                "cf-exit-from-loop",
                "cf-nested-if",
                "cf-nested-loops",
                "cf-stop-on-error",
                "cf-continue-after-error",
                "cf-group-basic"
        }) {
            stubTdl(id, "tdl/cf/" + id + ".xml");
        }
    }

    @Test
    void ifTrue() throws Exception {
        run("cf-if-true").assertSuccess().assertLogs(List.of("then-branch executed"));
    }

    @Test
    void ifFalse() throws Exception {
        run("cf-if-false").assertSuccess().assertLogs(List.of("else-branch executed"));
    }

    @Test
    void ifNoElse() throws Exception {
        assertSuccess(run("cf-if-no-else"));
    }

    @Test
    void whileBasic() throws Exception {
        run("cf-while-basic").assertSuccess().assertLogs(List.of("iteration 1", "iteration 2", "iteration 3"));
    }

    @Test
    void whileZero() throws Exception {
        assertSuccess(run("cf-while-zero"));
    }

    @Test
    void repuntilBasic() throws Exception {
        assertSuccess(run("cf-repuntil-basic"));
    }

    @Test
    void repuntilFirst() throws Exception {
        assertSuccess(run("cf-repuntil-first"));
    }

    @Test
    void foreachList() throws Exception {
        run("cf-foreach-list").assertSuccess().assertLogs(List.of("visiting apple", "visiting banana", "visiting cherry"));
    }

    @Test
    void foreachEmpty() throws Exception {
        assertSuccess(run("cf-foreach-empty"));
    }

    @Test
    void flowParallel() throws Exception {
        assertSuccess(run("cf-flow-parallel"));
    }

    @Test
    void exitSuccess() throws Exception {
        assertSuccess(run("cf-exit-success"));
    }

    @Test
    void exitFailure() throws Exception {
        assertFailed(run("cf-exit-failure"));
    }

    @Test
    void exitFromLoop() throws Exception {
        run("cf-exit-from-loop").assertSuccess().assertLogs(List.of("iteration 1", "iteration 2"));
    }

    @Test
    void nestedIf() throws Exception {
        assertSuccess(run("cf-nested-if"));
    }

    @Test
    void nestedLoops() throws Exception {
        assertSuccess(run("cf-nested-loops"));
    }

    @Test
    void stopOnError() throws Exception {
        assertFailed(run("cf-stop-on-error"));
    }

    @Test
    void continueAfterError() throws Exception {
        assertFailed(run("cf-continue-after-error"));
    }

    @Test
    void groupBasic() throws Exception {
        assertSuccess(run("cf-group-basic"));
    }
}
