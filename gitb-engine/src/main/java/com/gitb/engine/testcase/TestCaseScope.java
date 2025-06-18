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

import com.gitb.core.ErrorCode;
import com.gitb.engine.TestEngineConfiguration;
import com.gitb.engine.utils.ArtifactUtils;
import com.gitb.engine.utils.TemplateUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tdl.Imports;
import com.gitb.tdl.Namespaces;
import com.gitb.tdl.TestArtifact;
import com.gitb.types.*;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.gitb.engine.PropertyConstants.isBuiltInProperty;

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
	private final Map<String, DataType> resolvedArtifacts;
	private final Imports scopeImports;
	private final Namespaces scopeNamespaces;
	private Map<String, String> namespaceDefinitions;
	private final String testSuiteContext;
	private final String scopeId;
	private String qualifiedScopeId;
	private final boolean isolated;

	public TestCaseScope(TestCaseContext context, Imports imports, Namespaces namespaces) {
		this(null, context, imports, namespaces, null, false);
	}

	private TestCaseScope(String scopeId, TestCaseContext context, Imports imports, Namespaces namespaces, String testSuiteContext, boolean isolated) {
		this.scopeId = scopeId;
		this.context   = context;
		this.scopeImports = imports;
		this.scopeNamespaces = namespaces;
		this.symbols   = new ConcurrentHashMap<>();
		this.resolvedArtifacts   = new ConcurrentHashMap<>();
		this.children  = new CopyOnWriteArrayList<>();
		this.testSuiteContext = testSuiteContext;
		this.isolated = isolated;
	}

	public TestCaseScope getParent() {
		return parent;
	}

	public String getScopeId() {
		return scopeId;
	}

	public String getQualifiedScopeId() {
		if (qualifiedScopeId == null) {
			var id = new StringBuilder();
			var currentScope = this;
			while (currentScope != null) {
				if (StringUtils.isNotBlank(currentScope.getScopeId())) {
					id.insert(0, currentScope.getScopeId());
				}
				if (currentScope.getParent() != null && StringUtils.isNotBlank(currentScope.getParent().getScopeId())) {
					id.insert(0, '_');
				}
				currentScope = currentScope.getParent();
			}
			qualifiedScopeId = id.toString();
		}
		return qualifiedScopeId;
	}

	public String getTestSuiteContext() {
		return testSuiteContext;
	}

	public TestCaseScope createChildScope() {
		return createChildScope(this.scopeId, this.scopeImports, this.scopeNamespaces, this.testSuiteContext, false);
	}

	public TestCaseScope createChildScope(String scopeId, Imports imports, Namespaces namespaces, String testSuiteContext, boolean isolated) {
		if (testSuiteContext == null && this.testSuiteContext != null) {
			testSuiteContext = this.testSuiteContext;
		}
		TestCaseScope child = new TestCaseScope(scopeId, context, imports, namespaces, testSuiteContext, isolated);
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

	private DataType valueToStore(DataType value) {
		if (TestEngineConfiguration.TEMP_STORAGE_ENABLED && value != null && value.getValue() != null) {
			if (value instanceof BinaryType && TestEngineConfiguration.TEMP_STORAGE_BINARY_ENABLED && ((TestEngineConfiguration.TEMP_STORAGE_BINARY_THRESHOLD_BYTES <= 0) || (((byte[]) value.getValue()).length > TestEngineConfiguration.TEMP_STORAGE_BINARY_THRESHOLD_BYTES))) {
				return new StoredBinaryType(context.getDataFolder(), (BinaryType) value);
			} else if (value instanceof StringType && TestEngineConfiguration.TEMP_STORAGE_STRING_ENABLED && ((TestEngineConfiguration.TEMP_STORAGE_STRING_THRESHOLD_CHARS <= 0) || (((String) value.getValue()).length() > TestEngineConfiguration.TEMP_STORAGE_STRING_THRESHOLD_CHARS))) {
				return new StoredStringType(context.getDataFolder(), (StringType) value);
			} else if (value instanceof SchemaType && TestEngineConfiguration.TEMP_STORAGE_XML_ENABLED && ((TestEngineConfiguration.TEMP_STORAGE_XML_THRESHOLD_BYTES <= 0) || (((SchemaType) value).getSize() == null) || (((SchemaType) value).getSize() > TestEngineConfiguration.TEMP_STORAGE_XML_THRESHOLD_BYTES))) {
				return new StoredSchemaType(context.getDataFolder(), (SchemaType) value);
			} else if (value instanceof ObjectType && TestEngineConfiguration.TEMP_STORAGE_XML_ENABLED && ((TestEngineConfiguration.TEMP_STORAGE_XML_THRESHOLD_BYTES <= 0) || (((ObjectType) value).getSize() == null) || (((ObjectType) value).getSize() > TestEngineConfiguration.TEMP_STORAGE_XML_THRESHOLD_BYTES))) {
				return new StoredObjectType(context.getDataFolder(), (ObjectType) value);
			} else if (value instanceof MapType) {
				for (var key: ((MapType) value).getItems().keySet()) {
					((MapType) value).addItem(key, valueToStore(((MapType) value).getItem(key)));
				}
			} else if (value instanceof ListType) {
				for (int i=0; i < ((ListType) value).getSize(); i++) {
					((ListType) value).replaceItem(i, valueToStore(((ListType) value).getItem(i)));
				}
			}
		}
		return value;
	}

	private DataType setValue(String name, DataType value) {
		var valueToStore = valueToStore(value);
		symbols.put(name, valueToStore);
		return valueToStore;
	}

	public ScopedVariable getVariable(String name) {
		return getVariable(name, true);
	}

	public ScopedVariable getVariableWhileRespectingIsolation(String name) {
		/*
		 * If this scope is isolated, variable lookups that would lead to assignments should not look into parent scopes.
		 * The purpose of this is to avoid mutating unwillingly the state of a parent scope if an assignment is made to
		 * a variable with the same name.
		 */
		return getVariable(name, !isolated);
	}

	public Map<String, String> getNamespaceDefinitions() {
		if (namespaceDefinitions == null) {
			var namespaces = new HashMap<String, String>();
			TestCaseScope current = this;
			do {
				if (current.scopeNamespaces != null) {
					for (var ns: current.scopeNamespaces.getNs()) {
						// Ignore NS with a prefix that is already recorded (use only latest scope).
						if (!namespaces.containsKey(ns.getPrefix())) {
							namespaces.put(ns.getPrefix(), ns.getValue());
						}
					}
				}
				// Now process parent scope.
				current = current.parent;
			} while (current != null);
			namespaceDefinitions = namespaces;
		}
		return namespaceDefinitions;
	}

	public ScopedVariable getVariable(String name, boolean searchAncestors) {
		TestCaseScope current = this;
		while (current != null) {
			// Step 1: Check to see if the variable already exists in the scope.
			if (current.symbols.containsKey(name)) {
				return new ScopedVariable(current, name);
			}
			// Step 2: Check to see if this is an imported artifact (artifact values do not get recorded in the scope).
			if (current.scopeImports != null) {
				DataType artifactData = null;
				for (var artifact: current.scopeImports.getArtifactOrModule()) {
					if (artifact instanceof TestArtifact && name.equals(((TestArtifact) artifact).getName())) {
						if (current.resolvedArtifacts.containsKey(name)) {
							artifactData = current.resolvedArtifacts.get(name);
							break;
						} else {
							try {
								var artifactLookup = ArtifactUtils.resolveArtifact(context, current, (TestArtifact) artifact);
								artifactData = artifactLookup.getValue();
								var valueToStore = valueToStore(artifactData);
								if (valueToStore == null) {
									throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Artifact linked to name [%s] could not be loaded from path [%s].".formatted(name, artifactLookup.getKey())));
								}
								current.resolvedArtifacts.put(name, valueToStore);
								break;
							} catch (IOException e) {
								throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Artifact linked to name ["+name+"] could not be loaded."), e);
							}
						}
					}
				}
				if (artifactData != null) {
					/* The loaded data may be a template. We need to process it here to make replacements (the processed
					   template is however not stored in the scope to allow its reuse with different values). */
					var processedArtifactData = TemplateUtils.generateDataTypeFromTemplate(current, artifactData, artifactData.getType(), false);
					ScopedArtifact scopedArtifact = new ScopedArtifact(current, name);
					scopedArtifact.setValue(processedArtifactData);
					return scopedArtifact;
				}
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
		public DataType setValue(DataType value) {
			if(this.scope == null) {
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INVALID_TEST_CASE, "Undefined variable ["+name+"]!"));
			}
			return this.scope.setValue(this.name, value);
		}

		@Override
		public String toString() {
			return "ScopedVariable{name='" + name + '\'' + ", value=" + getValue() + "}";
		}
	}

	public static class ScopedArtifact extends ScopedVariable {

		private DataType artifact;

		public ScopedArtifact(TestCaseScope scope, String name) {
			super(scope, name);
		}

		@Override
		public DataType getValue() {
			return artifact;
		}

		@Override
		public DataType setValue(DataType value) {
			this.artifact = value;
			return this.artifact;
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
