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

package com.gitb.vs.tdl.rules.testcase.expression;

import com.gitb.tdl.Scriptlet;
import com.gitb.tdl.TestCase;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ErrorCode;

import java.util.Map;

public interface VariableResolverProvider {

    Map<String, Boolean> getScope();
    void addReportItem(ErrorCode errorCode, String... parameters);
    Context getContext();
    TestCase getCurrentTestCase();
    Scriptlet getCurrentScriptlet();
    Object getCurrentStep();
    boolean isInternalScriptlet();
    boolean isStandaloneScriptlet();

}
