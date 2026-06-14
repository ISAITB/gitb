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

import com.gitb.tr.TestResultType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestRunResult {

    private final TestStepStatus finalStatus;
    private final List<String> logMessages;

    TestRunResult(TestStepStatus finalStatus, List<String> logMessages) {
        this.finalStatus = finalStatus;
        this.logMessages = List.copyOf(logMessages);
    }

    public TestStepStatus status() {
        return finalStatus;
    }

    public List<String> logMessages() {
        return logMessages;
    }

    public TestRunResult assertSuccess() {
        assertEquals(TestResultType.SUCCESS, finalStatus.getReport().getResult(),
                "Expected session SUCCESS but got: " + finalStatus.getReport().getResult());
        return this;
    }

    public TestRunResult assertFailed() {
        assertEquals(TestResultType.FAILURE, finalStatus.getReport().getResult(),
                "Expected session FAILURE but got: " + finalStatus.getReport().getResult());
        return this;
    }

    public TestRunResult assertWarning() {
        assertEquals(TestResultType.WARNING, finalStatus.getReport().getResult(),
                "Expected session WARNING but got: " + finalStatus.getReport().getResult());
        return this;
    }

    /**
     * Asserts that every string in {@code expected} appears as a substring of at least one
     * captured log message. By default the match is ordered: each expected string must be
     * found in a log message that comes after the log message matched by the previous expected
     * string. Pass {@code ordered = false} to skip the ordering constraint.
     */
    public TestRunResult assertLogs(List<String> expected, boolean ordered) {
        if (ordered) {
            int pointer = 0;
            for (String exp : expected) {
                boolean found = false;
                while (pointer < logMessages.size()) {
                    if (logMessages.get(pointer).contains(exp)) {
                        found = true;
                        pointer++;
                        break;
                    }
                    pointer++;
                }
                if (!found) {
                    fail("Expected log message containing [" + exp + "] not found in order. Captured logs: " + logMessages);
                }
            }
        } else {
            for (String exp : expected) {
                boolean found = logMessages.stream().anyMatch(msg -> msg.contains(exp));
                if (!found) {
                    fail("Expected log message containing [" + exp + "] not found. Captured logs: " + logMessages);
                }
            }
        }
        return this;
    }

    public TestRunResult assertLogs(List<String> expected) {
        return assertLogs(expected, true);
    }
}
