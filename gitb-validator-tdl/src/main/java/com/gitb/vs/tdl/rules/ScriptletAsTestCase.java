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

import com.gitb.core.Metadata;
import com.gitb.tdl.*;

import java.nio.file.Path;

public class ScriptletAsTestCase extends TestCase {

    private final Scriptlet scriptlet;
    private final Path scriptletPath;

    public ScriptletAsTestCase(Scriptlet scriptlet, Path scriptletPath) {
        this.scriptlet = scriptlet;
        this.scriptletPath = scriptletPath;
    }

    public Path getScriptletPath() {
        return scriptletPath;
    }

    @Override
    public Metadata getMetadata() {
        return scriptlet.getMetadata();
    }

    @Override
    public Namespaces getNamespaces() {
        return scriptlet.getNamespaces();
    }

    @Override
    public Imports getImports() {
        return scriptlet.getImports();
    }

    @Override
    public Variables getVariables() {
        return scriptlet.getVariables();
    }

    @Override
    public TestCaseSteps getSteps() {
        var steps = new TestCaseSteps();
        steps.getSteps().addAll(scriptlet.getSteps().getSteps());
        return steps;
    }

    public Scriptlet getWrappedScriptlet() {
        return scriptlet;
    }

    @Override
    public String getId() {
        return scriptlet.getId();
    }

    //        getPreliminary
    //        getActors
    //        getOutput
    //        getScriptlets

}
