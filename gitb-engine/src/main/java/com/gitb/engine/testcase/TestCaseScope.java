package com.gitb.engine.testcase;

import com.gitb.core.ErrorCode;
import com.gitb.engine.utils.ArtifactUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.TestArtifact;
import com.gitb.types.DataType;
import com.gitb.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger logger = LoggerFactory.getLogger(TestCaseScope.class);

	private final TestCaseContext context;

	private TestCaseScope parent;
	private List<TestCaseScope> children;

	private Map<String, DataType> symbols;

	public TestCaseScope(TestCaseContext context) {
		this.context   = context;
		this.symbols   = new ConcurrentHashMap<>();
		this.children  = new CopyOnWriteArrayList<>();
	}

	public TestCaseScope createChildScope() {
		TestCaseScope child = new TestCaseScope(context);

		child.parent = this;

		children.add(child);

		return child;
	}

	public boolean removeChildScope(TestCaseScope childScope) {
		int index = children.indexOf(childScope);

		if(index < 0)  {
			return false;
		} else {
			childScope.destroy();
			children.remove(index);
			return true;
		}
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

	public ScopedVariable getVariable(String name) throws IOException {
		TestArtifact artifact = context.getTestArtifact(name);

		if (artifact != null) {
			DataType data = ArtifactUtils.resolveArtifact(context, this, artifact);

			ScopedVariable variable = createVariable(name);
			variable.setValue(data);

			return variable;
		} else {
			return getVariable(name, true);
		}
	}

	public ScopedVariable getVariable(String name, boolean searchAncestors) {
		TestCaseScope current = this;

		while(current != null) {
			if(current.symbols.containsKey(name)) {
				return new ScopedVariable(current, name);
			} else {
				if (!searchAncestors) {
					break;
				}
				current = current.parent;
			}
		}

		return new ScopedVariable(null, name);
	}

	public ScopedVariable createVariable(String name) {
		return new ScopedVariable(this, name);
	}

	public void destroy() {
		for(TestCaseScope scope : children) {
			scope.destroy();
		}

		children.clear();
		symbols.clear();

		if(parent != null) {
			parent.children.remove(this);
		}

		parent = null;
	}

	public TestCaseContext getContext() {
		return context;
	}

	public class ScopedVariable {
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
