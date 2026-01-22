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

package com.gitb.engine.testcase;

import com.gitb.engine.expr.StaticExpressionHandler;
import com.gitb.tdl.CallStep;
import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import org.apache.commons.lang3.tuple.Pair;

import java.util.LinkedList;
import java.util.Objects;

public record StepTraversalState(String testSuiteContext, TestCaseContext context, LinkedList<Pair<CallStep, Scriptlet>> scriptletCallStack, LinkedList<StaticExpressionHandler> expressionHandlerStack, TestCase testCase) {

    public StaticExpressionHandler getExpressionHandler() {
        return Objects.requireNonNull(expressionHandlerStack.peekLast(), "No available expression handler");
    }

    public void scriptletStart(Scriptlet scriptlet, CallStep callStep) {
        scriptletCallStack.addLast(Pair.of(callStep, scriptlet));
        expressionHandlerStack.addLast(getExpressionHandler().newForScriptlet(scriptlet, callStep));
    }

    public void scriptletEnd() {
        getExpressionHandler().close();
        expressionHandlerStack.removeLast();
        scriptletCallStack.removeLast();
    }

}
