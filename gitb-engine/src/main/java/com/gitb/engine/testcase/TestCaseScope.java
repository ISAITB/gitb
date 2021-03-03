package com.gitb.engine.testcase;

import com.gitb.core.ErrorCode;
import com.gitb.engine.utils.ArtifactUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Imports;
import com.gitb.tdl.TestArtifact;
import com.gitb.types.DataType;
import com.gitb.utils.ErrorUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by serbay on 9/3/14.
 *
 * Class that contains variable bindings contained in an execution block.
 *
 * Works in a similar fashion to Java scoping.
 */
public class TestCaseScope {

	private final TestCaseContext context;

	private TestCaseScope parent;
	private final List<TestCaseScope> children;

	private final Map<String, DataType> symbols;
	private final Imports scopeImports;
	private final String testSuiteContext;

	public TestCaseScope(TestCaseContext context, Imports imports) {
		this(context, imports, null);
	}

	private TestCaseScope(TestCaseContext context, Imports imports, String testSuiteContext) {
		this.context   = context;
		this.scopeImports = imports;
		this.symbols   = new ConcurrentHashMap<>();
		this.children  = new CopyOnWriteArrayList<>();
		this.testSuiteContext = testSuiteContext;
	}

	public String getTestSuiteContext() {
		return testSuiteContext;
	}

	public TestCaseScope createChildScope() {
		return createChildScope(null, null);
	}

	public TestCaseScope createChildScope(Imports imports, String testSuiteContext) {
		if (testSuiteContext == null && this.testSuiteContext != null) {
			testSuiteContext = this.testSuiteContext;
		}
		TestCaseScope child = new TestCaseScope(context, imports, testSuiteContext);
		child.parent = this;
		children.add(child);
		return child;
	}

	private DataType getValue(String name) {
		TestCaseScope current = this;

		while(current != null) {

			if(current.symbols.containsKey(name)) {
				return current.symbols.get(name);
			} else {
				current = current.parent;
			}
		}

		return null;
	}

	private void setValue(String name, DataType value) {
		symbols.put(name, value);
	}

	private TestArtifact getTestArtifact(String name) {
		if (name != null && scopeImports != null) {
			for (var artifact: scopeImports.getArtifactOrModule()) {
				if (artifact instanceof TestArtifact && name.equals(((TestArtifact) artifact).getName())) {
					return (TestArtifact) artifact;
				}
			}
		}
		return null;
	}

	public ScopedVariable getVariable(String name) {
		// When we have a scope from another test suite (i.e. a scriptlet) we should not propagate variable searches to parent scopes.
		boolean searchAncestors = testSuiteContext == null;
		return getVariable(name, searchAncestors);
	}

	public ScopedVariable getVariable(String name, boolean searchAncestors) {
		TestCaseScope current = this;
		while (current != null) {
			// Step 1: Check to see if the variable already exists in the scope.
			if (current.symbols.containsKey(name)) {
				return new ScopedVariable(current, name);
			}
			// Step 2: Check to see if this is an imported artefact.
			TestArtifact artifact = current.getTestArtifact(name);
			if (artifact != null) {
				DataType data;
				try {
					data = ArtifactUtils.resolveArtifact(context, current, artifact);
				} catch (IOException e) {
					throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Artifact linked to name ["+name+"] could not be loaded."), e);
				}
				ScopedVariable variable = new ScopedVariable(current, name);
				variable.setValue(data);
				return variable;
			}
			// Step 3: Repeat for parent scope.
			if (searchAncestors) {
				current = current.parent;
			} else {
				break;
			}
		}
		return new ScopedVariable(null, name);
	}

	public ScopedVariable createVariable(String name) {
		return new ScopedVariable(this, name);
	}

	public TestCaseContext getContext() {
		return context;
	}

	public static class ScopedVariable {
		protected final TestCaseScope scope;
		protected final String name;

		public ScopedVariable(TestCaseScope scope, String name) {
			this.scope = scope;
			this.name = name;
		}

		public TestCaseScope getScope() {
			return scope;
		}

        public boolean isDefined() {
            return scope != null;
        }

		public DataType getValue() {
			return scope.getValue(name);
		}

		/**
		 * This method sets the value of an existing variable.
		 *
		 * @param value variable value to be set
		 * @throws com.gitb.exceptions.GITBEngineInternalError if the variable is not defined, meaning that
		 * it is not created yet.
		 */
		public void setValue(DataType value) {
			if(this.scope == null) {
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Undefined variable ["+name+"]!"));
			}
			this.scope.setValue(this.name, value);
		}

		@Override
		public String toString() {
			return "ScopedVariable{" +
				"name='" + name + '\'' +
				", value=" + scope.getValue(name) +
				'}';
		}
	}

	@Override
	public String toString() {
		return "TestCaseScope{" +
			"symbols=" + symbols +
			", children=" + children +
			'}';
	}
}
