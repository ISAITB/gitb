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
    public Sequence getSteps() {
        return scriptlet.getSteps();
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
