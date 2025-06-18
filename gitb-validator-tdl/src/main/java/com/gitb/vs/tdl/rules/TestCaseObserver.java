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

package com.gitb.vs.tdl.rules;

import com.gitb.core.Documentation;
import com.gitb.core.TestRole;
import com.gitb.tdl.*;
import com.gitb.vs.tdl.Context;
import com.gitb.vs.tdl.ValidationReport;

public interface TestCaseObserver {

    void initialise(Context context, ValidationReport report);

    void initialiseTestCase(TestCase currentTestCase);

    void initialiseScriptlet(Scriptlet scriptlet);

    void sectionChanged(TestCaseSection section);

    void handleStep(Object step);

    void handleActor(TestRole testRole);

    void handleTestOutput(Output output);

    void handleImport(Object artifactObj);

    void handleDocumentation(Documentation documentation);

    void handleInputParameter(InputParameter param);

    void handleVariable(Variable var);

    void handleOutput(Binding binding);

    void finaliseScriptlet();

    void finaliseTestCase();

    void finalise();

}
