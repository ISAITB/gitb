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

package com.gitb.engine.testcase;

import com.gitb.core.ActorConfiguration;
import com.gitb.tbs.SUTConfiguration;
import com.gitb.tdl.TestCase;

import java.util.ArrayList;
import java.util.List;

public class StaticTestCaseContext extends TestCaseContext {

    public StaticTestCaseContext(TestCase testCase) {
        super(testCase, testCase.getId(), "");
    }

    @Override
    protected List<SUTConfiguration> configureDynamicActorProperties(TestCase testCase, List<ActorConfiguration> configurations) {
        /*
         * We skip contacting remote services to define dynamic actor configurations. Instead, we simply return an empty list.
         */
        return new ArrayList<>();
    }
}
