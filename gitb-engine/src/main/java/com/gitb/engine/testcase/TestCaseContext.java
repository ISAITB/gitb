package com.gitb.engine.testcase;

import com.gitb.core.*;
import com.gitb.engine.ModuleManager;
import com.gitb.engine.SessionManager;
import com.gitb.engine.TestEngineConfiguration;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.messaging.handlers.layer.AbstractMessagingHandler;
import com.gitb.engine.processing.ProcessingContext;
import com.gitb.engine.remote.messaging.RemoteMessagingModuleClient;
import com.gitb.engine.utils.ScriptletCache;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.ms.InitiateResponse;
import com.gitb.tbs.SUTConfiguration;
import com.gitb.tdl.*;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.utils.ActorUtils;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.map.Tuple;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.gitb.engine.PropertyConstants.*;
import static com.gitb.engine.utils.TestCaseUtils.TEST_ENGINE_VERSION;

/**
 * Created by serbay on 9/3/14.
 *
 * Class that encapsulates all the necessary information for a test case execution session
 */
public class TestCaseContext {

	private static final Logger logger = LoggerFactory.getLogger(TestCaseContext.class);

	/**
	 * The map containing the success flags for each executed step.
	 */
	public static final String STEP_SUCCESS_MAP = "STEP_SUCCESS";

	/**
	 * The map containing the status values for all steps.
	 */
	public static final String STEP_STATUS_MAP = "STEP_STATUS";

	/**
	 * The scope variable holding the overall result of the test session.
	 */
	public static final String TEST_SUCCESS = "TEST_SUCCESS";

    /**
     * Test case to be executed
     */
    private final TestCase testCase;

    /**
     * Test session id given to the testbed service client to control the execution
     */
    private final String sessionId;
	/**
	 * Test case identifier as stated in its TDL definition
	 */
	private final String testCaseIdentifier;

	/**
     * Test case scope where all the variables, artifacts are stored and accessed.
     */
    private TestCaseScope scope;

    /**
     * Configurations of SUTs which are provided by the users. Map key is set to (actorId, endpointName) tuples.
     * Note that in the case of an actor having just one endpoint, an empty endpoint value is possible. In this
     * case, the key part will be in the (actorId, null) form.
     */
    private final Map<Tuple<String>, ActorConfiguration> sutConfigurations;

    /**
     * MessagingContext for each handler used in the TestCase
     */
    private final Map<String, MessagingContext> messagingContexts;

	/**
	 * ProcessingContext for the processing transactions (for each transaction)
	 */
	private final Map<String, ProcessingContext> processingContexts;

    /**
     * Roles defined in the TestCase
     */
    private final Map<String, TestRole> actorRoles;

    /**
     * Corresponding simulated actor configurations for each SUT (key: actor name & actor endpoint of SUT)
     * Map key is set to (actorId, endpointName) tuples whereas value is set to simulated actor configurations
     * for the (actorId, endpointName) tuple. Note that in the case of an actor having just one endpoint,
     * the key part will be in the (actorId, null) form.
     */
    private final Map<Tuple<String>, List<ActorConfiguration>> sutHandlerConfigurations;

    private final ScriptletCache scriptletCache = new ScriptletCache();
	private final VariableResolver variableResolver;

    /**
     * Current state of the test case execution
     */
    private TestCaseStateEnum currentState;

	/**
	 * Test session state enumeration
	 */
    public enum TestCaseStateEnum {
		/**
		 * Just initialized, waiting for configuration
		 */
        IDLE,
		/**
		 * Configured, waiting for preliminary initiation
		 */
        CONFIGURATION,
		/**
		 * Preliminary requirements are sent to SUTs, waiting for inputs
		 */
        PRELIMINARY,
		/**
		 * Preliminary phase completed, waiting for execution
		 */
        READY,
		/**
		 * Execution is in progress
		 */
        EXECUTION,
		/**
		 * Output calculation is in progress
		 */
		OUTPUT,
		/**
		 * Execution is completed or stopped by user or exit with some error
		 */
        STOPPED,
		/**
		 * Execution is in the process of stopping
		 */
		STOPPING
    }

	private com.gitb.core.LogLevel logLevelToSignal = com.gitb.core.LogLevel.DEBUG;
	private boolean logLevelIsExpression = false;
	private boolean reportedInvalidLogLevel = false;
	private Path dataFolder;

    public TestCaseContext(TestCase testCase, String testCaseIdentifier, String sessionId) {
        this.currentState = TestCaseStateEnum.IDLE;
        this.testCase = testCase;
		this.testCaseIdentifier = testCaseIdentifier;
        this.sessionId = sessionId;
        this.sutConfigurations = new ConcurrentHashMap<>();
        this.sutHandlerConfigurations = new ConcurrentHashMap<>();
        this.messagingContexts = new ConcurrentHashMap<>();
		this.processingContexts = new ConcurrentHashMap<>();
        this.scope = new TestCaseScope(this, testCase.getImports(), testCase.getNamespaces());
		this.variableResolver = new VariableResolver(scope);
		if (testCase.getSteps() != null) {
			this.logLevelIsExpression = VariableResolver.isVariableReference(testCase.getSteps().getLogLevel());
			if (!this.logLevelIsExpression) {
				this.logLevelToSignal = com.gitb.core.LogLevel.fromValue(testCase.getSteps().getLogLevel());
			}
		}
		if (TestEngineConfiguration.TEMP_STORAGE_ENABLED) {
			// Initialise storage folder
			try {
				dataFolder = Path.of(TestEngineConfiguration.TEMP_STORAGE_LOCATION, sessionId);
				Files.createDirectories(dataFolder);
			} catch (IOException e) {
				throw new IllegalStateException("Unable to create session data storage folder", e);
			}
		}
        addStepStatus();
        processVariables();

        actorRoles = new HashMap<>();

        // Initialize configuration lists for SutHandlerConfigurations
        for(TestRole role : this.testCase.getActors().getActor()) {
			actorRoles.put(role.getId(), role);
		}
	}

	public Path getDataFolder() {
		return dataFolder;
	}

	public ScriptletCache getScriptletCache() {
    	return this.scriptletCache;
	}

	private void addStepStatus() {
		var successMap = new StepStatusMapType();
		var statusMap = new StepStatusMapType();
		scope.createVariable(STEP_SUCCESS_MAP).setValue(successMap);
		scope.createVariable(STEP_STATUS_MAP).setValue(statusMap);
		TestCaseUtils.initialiseStepStatusMaps(successMap, statusMap, testCase.getSteps(), scope);
	}

	private void processVariables() {
        //process variables to create test case scope symbols
        if (this.testCase.getVariables() != null) {
            for (Variable variable : this.testCase.getVariables().getVar()) {
                DataType value;
                try {
                    value = DataTypeFactory.getInstance().create(variable);
                } catch (Exception e) {
                    value = DataTypeFactory.getInstance().create(variable.getType());
                }
                this.scope.createVariable(variable.getName()).setValue(value);

            }
        }
    }

    /**
     * Configure simulated actors for the given SUT configurations
     * @param configurations SUT configurations
     * @return Simulated actor configurations for each (actorId, endpointName) tuple
     */
    public List<SUTConfiguration> configure(List<ActorConfiguration> configurations, ActorConfiguration domainConfiguration, ActorConfiguration organisationConfiguration, ActorConfiguration systemConfiguration){

		addSpecialConfiguration(DOMAIN_MAP, domainConfiguration);
		addSpecialConfiguration(ORGANISATION_MAP, organisationConfiguration);
		addSpecialConfiguration(SYSTEM_MAP, systemConfiguration);
		addSessionMetadata();

		for(ActorConfiguration actorConfiguration : configurations) {
		    Tuple<String> actorIdEndpointTupleKey = new Tuple<>(new String[] {actorConfiguration.getActor(), actorConfiguration.getEndpoint()});
		    sutConfigurations.put(actorIdEndpointTupleKey, actorConfiguration);
		    sutHandlerConfigurations.put(actorIdEndpointTupleKey, new CopyOnWriteArrayList<>());
	    }

	    List<SUTConfiguration> sutConfigurations = configureSimulatedActorsForSUTs(configurations);

	    // set the configuration parameters given in the test case definition if they are not set already
	    for(TestRole role : testCase.getActors().getActor()) {
		    for(Endpoint endpoint : role.getEndpoint()) {
			    for(Parameter parameter : endpoint.getConfig()) {
				    setSUTConfigurationParameter(sutConfigurations, role.getId(), endpoint.getName(), parameter);
			    }
		    }
	    }

	    bindActorConfigurationsToScope();

	    return sutConfigurations;
    }

	private void addSessionMetadata() {
		DataTypeFactory factory = DataTypeFactory.getInstance();
		TestCaseScope.ScopedVariable variable = scope.createVariable(SESSION_MAP);
		var map = (MapType) factory.create(DataType.MAP_DATA_TYPE);
		var sessionId = factory.create(DataType.STRING_DATA_TYPE);
		sessionId.setValue(getSessionId());
		map.addItem(SESSION_MAP__TEST_SESSION_ID, sessionId);
		var testCaseId = factory.create(DataType.STRING_DATA_TYPE);
		testCaseId.setValue(getTestCaseIdentifier());
		map.addItem(SESSION_MAP__TEST_CASE_ID, testCaseId);
		var testEngineVersion = factory.create(DataType.STRING_DATA_TYPE);
		testEngineVersion.setValue(TEST_ENGINE_VERSION);
		map.addItem(SESSION_MAP__TEST_ENGINE_VERSION, testEngineVersion);
		variable.setValue(map);
	}

	private void addSpecialConfiguration(String mapVariableName, ActorConfiguration domainConfiguration) {
		if (domainConfiguration != null) {
			DataTypeFactory factory = DataTypeFactory.getInstance();
			TestCaseScope.ScopedVariable variable = scope.createVariable(mapVariableName);
			MapType map = (MapType) factory.create(DataType.MAP_DATA_TYPE);
			for (Configuration configuration : domainConfiguration.getConfig()) {
				DataType configurationValue;
				if (configuration.getValue() != null && configuration.getValue().startsWith("data:") && configuration.getValue().contains(";base64,")) {
					// Data URL
					String base64 = configuration.getValue().substring(configuration.getValue().indexOf(",")+1);
					configurationValue = factory.create(DataType.BINARY_DATA_TYPE);
					configurationValue.setValue(Base64.decodeBase64(base64));
				} else {
					// String
					configurationValue = factory.create(DataType.STRING_DATA_TYPE);
					configurationValue.setValue(configuration.getValue());
				}
				map.addItem(configuration.getName(), configurationValue);
			}
			variable.setValue(map);
		}
	}

	private void setSUTConfigurationParameter(List<SUTConfiguration> sutConfigurations, String id, String endpoint, Parameter parameter) {
		for(SUTConfiguration sutConfiguration : sutConfigurations) {
			for(ActorConfiguration actorConfiguration : sutConfiguration.getConfigs()) {
				if(id.equals(actorConfiguration.getActor()) &&
                        ((endpoint != null && actorConfiguration.getEndpoint() != null && endpoint.equals(actorConfiguration.getEndpoint()))
                        || actorConfiguration.getEndpoint() == null)) {

					boolean configurationExists = false;
					for(Configuration configuration : actorConfiguration.getConfig()) {
						if(configuration.getName().equals(parameter.getName())) {
							configurationExists = true;
							break;
						}
					}

					if(!configurationExists) {
						Configuration configuration = new Configuration();
						configuration.setName(parameter.getName());
						configuration.setValue(parameter.getValue());

						actorConfiguration.getConfig().add(configuration);
					}

					break;
				}
			}
		}
	}

	private void bindActorConfigurationsToScope() {

		DataTypeFactory factory = DataTypeFactory.getInstance();

		for(ActorConfiguration actorConfiguration : sutConfigurations.values()) {
			TestCaseScope.ScopedVariable variable = scope.createVariable(actorConfiguration.getActor());

			MapType map = (MapType) factory.create(DataType.MAP_DATA_TYPE);

			for(Configuration configuration : actorConfiguration.getConfig()) {
				DataType configurationValue;
				if (configuration.getValue() != null && configuration.getValue().startsWith("data:") && configuration.getValue().contains(";base64,")) {
					// Data URL
					String base64 = configuration.getValue().substring(configuration.getValue().indexOf(",")+1);
					configurationValue = factory.create(DataType.BINARY_DATA_TYPE);
					configurationValue.setValue(Base64.decodeBase64(base64));
				} else {
					// String
					configurationValue = factory.create(DataType.STRING_DATA_TYPE);
					configurationValue.setValue(configuration.getValue());
				}
				map.addItem(configuration.getName(), configurationValue);
			}

			List<ActorConfiguration> actorSUTConfigurations = sutHandlerConfigurations.get(new Tuple<>(new String[] {actorConfiguration.getActor(), actorConfiguration.getEndpoint()}));

			if (actorSUTConfigurations != null) {
				for(ActorConfiguration sutConfiguration : actorSUTConfigurations) {
					if (sutConfiguration != null) {
						MapType sutConfigurationMap = (MapType) factory.create(DataType.MAP_DATA_TYPE);

						for(Configuration configuration : sutConfiguration.getConfig()) {
							DataType configurationValue;
							if (configuration.getValue() != null && configuration.getValue().startsWith("data:") && configuration.getValue().contains(";base64,")) {
								// Data URL
								String base64 = configuration.getValue().substring(configuration.getValue().indexOf(",")+1);
								configurationValue = factory.create(DataType.BINARY_DATA_TYPE);
								configurationValue.setValue(Base64.decodeBase64(base64));
							} else {
								// String
								configurationValue = factory.create(DataType.STRING_DATA_TYPE);
								configurationValue.setValue(configuration.getValue());
							}
							sutConfigurationMap.addItem(configuration.getName(), configurationValue);
						}

						map.addItem(sutConfiguration.getActor(), sutConfigurationMap);
					}
				}
			}

			variable.setValue(map);
		}
	}

	private TestRoleEnumeration actorRole(TestRole role) {
		if (role != null && role.getRole() != null) {
			return role.getRole();
		}
		return TestRoleEnumeration.SIMULATED;
	}

	private List<SUTConfiguration> configureSimulatedActorsForSUTs(List<ActorConfiguration> configurations) {
		List<TransactionInfo> testCaseTransactions = createTransactionInfo(testCase.getSteps(), null, new VariableResolver(scope), new LinkedList<>());
		Map<String, MessagingContextBuilder> messagingContextBuilders = new HashMap<>();

        // collect all transactions needed to configure the messaging handlers
		for(TransactionInfo transactionInfo : testCaseTransactions) {
			var fromRole = actorRole(actorRoles.get(transactionInfo.fromActorId));
			var toRole = actorRole(actorRoles.get(transactionInfo.toActorId));

			if(!messagingContextBuilders.containsKey(transactionInfo.handler)) {
				messagingContextBuilders.put(transactionInfo.handler, new MessagingContextBuilder(transactionInfo));
			}

			MessagingContextBuilder builder = messagingContextBuilders.get(transactionInfo.handler);

			if(fromRole == TestRoleEnumeration.SUT
				&& toRole == TestRoleEnumeration.SUT) {
				// both of them are SUTs, messaging handler acts as a proxy between them
				builder.addActorConfiguration(transactionInfo.toActorId, transactionInfo.toEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.fromActorId, transactionInfo.fromEndpointName))
					   .addActorConfiguration(transactionInfo.fromActorId, transactionInfo.fromEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.toActorId, transactionInfo.toEndpointName));
			} else if(fromRole == TestRoleEnumeration.SUT && toRole == TestRoleEnumeration.SIMULATED) {
				// just one of them is SUT, messaging handler acts as the simulated actor
				builder.addActorConfiguration(transactionInfo.toActorId, transactionInfo.toEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.fromActorId, transactionInfo.fromEndpointName));
			} else if(fromRole == TestRoleEnumeration.SIMULATED && toRole == TestRoleEnumeration.SUT) {
				// just one of them is SUT, messaging handler acts as the simulated actor
				builder.addActorConfiguration(transactionInfo.fromActorId, transactionInfo.fromEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.toActorId, transactionInfo.toEndpointName));
			} else {
				// both of them are simulated actors
				builder.addActorConfiguration(transactionInfo.toActorId, transactionInfo.toEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.fromActorId, transactionInfo.fromEndpointName))
						.addActorConfiguration(transactionInfo.fromActorId, transactionInfo.fromEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.toActorId, transactionInfo.toEndpointName));
			}
		}

		List<SUTConfiguration> handlerConfigurations = new ArrayList<>();

		for (MessagingContextBuilder builder : messagingContextBuilders.values()) {
			MessagingContext messagingContext = builder.build(sessionId);
			handlerConfigurations.addAll(messagingContext.getSutHandlerConfigurations());
			messagingContexts.put(builder.getHandler(), messagingContext);
			SessionManager.getInstance().mapMessagingSessionToTestSession(messagingContext.getSessionId(), sessionId);
		}

        List<SUTConfiguration> configurationsBySUTActor = groupGeneratedSUTConfigurationsBySUTActor(handlerConfigurations);

        for (SUTConfiguration sutConfiguration : configurationsBySUTActor) {
            List<ActorConfiguration> actorConfigurations = sutHandlerConfigurations.get(new Tuple<>(new String[] {sutConfiguration.getActor(), sutConfiguration.getEndpoint()}));

            actorConfigurations.addAll(sutConfiguration.getConfigs());
        }

        return configurationsBySUTActor;

	}

	private List<SUTConfiguration> groupGeneratedSUTConfigurationsBySUTActor(List<SUTConfiguration> sutHandlerConfigurations) {
		Map<Tuple<String>, SUTConfiguration> groups = new HashMap<>();

		for(SUTConfiguration sutConfiguration : sutHandlerConfigurations) {
			Tuple<String> actorTuple = new Tuple<>(new String[]{sutConfiguration.getActor(), sutConfiguration.getEndpoint()});
			if(!groups.containsKey(actorTuple)) {
				SUTConfiguration groupedSUTConfiguration = new SUTConfiguration();
				groupedSUTConfiguration.setActor(actorTuple.getContents()[0]);
				groupedSUTConfiguration.setEndpoint(actorTuple.getContents()[1]);
				groups.put(actorTuple, groupedSUTConfiguration);
			}

			SUTConfiguration groupedSUTConfiguration = groups.get(actorTuple);
			groupedSUTConfiguration.getConfigs().addAll(sutConfiguration.getConfigs());
		}

		return new ArrayList<>(groups.values());
	}

	private TransactionInfo buildTransactionInfo(String from, String to, String handler, List<Configuration> properties, VariableResolver resolver, LinkedList<Pair<CallStep, Scriptlet>> scriptletCallStack) {
		return new TransactionInfo(
				TestCaseUtils.fixedOrVariableValue(ActorUtils.extractActorId(from), String.class, scriptletCallStack),
				TestCaseUtils.fixedOrVariableValue(ActorUtils.extractEndpointName(from), String.class, scriptletCallStack),
				TestCaseUtils.fixedOrVariableValue(ActorUtils.extractActorId(to), String.class, scriptletCallStack),
				TestCaseUtils.fixedOrVariableValue(ActorUtils.extractEndpointName(to), String.class, scriptletCallStack),
				VariableResolver.isVariableReference(handler)?resolver.resolveVariableAsString(handler).toString():handler,
				TestCaseUtils.getStepProperties(properties, resolver)
		);
	}

    /**
     * Traverses test case steps to generate transaction information containing (from, handler, to) information
     * @param sequence test step sequence
     * @return transactions occurring in the given sequence
     */
    private List<TransactionInfo> createTransactionInfo(Sequence sequence, String testSuiteContext, VariableResolver resolver, LinkedList<Pair<CallStep, Scriptlet>> scriptletCallStack) {
        List<TransactionInfo> transactions = new ArrayList<>();
        for(Object step : sequence.getSteps()) {
            if(step instanceof Sequence) {
                transactions.addAll(createTransactionInfo((Sequence) step, testSuiteContext, resolver, scriptletCallStack));
            } else if(step instanceof BeginTransaction) {
				var beginTransactionStep = (BeginTransaction) step;
				transactions.add(buildTransactionInfo(beginTransactionStep.getFrom(), beginTransactionStep.getTo(), beginTransactionStep.getHandler(), beginTransactionStep.getProperty(), resolver, scriptletCallStack));
			} else if (step instanceof MessagingStep) {
				var messagingStep = (MessagingStep) step;
				if (StringUtils.isBlank(messagingStep.getTxnId()) && StringUtils.isNotBlank(messagingStep.getHandler())) {
					transactions.add(buildTransactionInfo(messagingStep.getFrom(), messagingStep.getTo(), messagingStep.getHandler(), messagingStep.getProperty(), resolver, scriptletCallStack));
				}
            } else if (step instanceof IfStep) {
	            transactions.addAll(createTransactionInfo(((IfStep) step).getThen(), testSuiteContext, resolver, scriptletCallStack));
	            if (((IfStep) step).getElse() != null) {
					transactions.addAll(createTransactionInfo(((IfStep) step).getElse(), testSuiteContext, resolver, scriptletCallStack));
				}
            } else if(step instanceof WhileStep) {
	            transactions.addAll(createTransactionInfo(((WhileStep) step).getDo(), testSuiteContext, resolver, scriptletCallStack));
            } else if(step instanceof ForEachStep) {
	            transactions.addAll(createTransactionInfo(((ForEachStep) step).getDo(), testSuiteContext, resolver, scriptletCallStack));
            } else if(step instanceof RepeatUntilStep) {
	            transactions.addAll(createTransactionInfo(((RepeatUntilStep) step).getDo(), testSuiteContext, resolver, scriptletCallStack));
            } else if(step instanceof FlowStep) {
	            for(Sequence thread : ((FlowStep) step).getThread()) {
		            transactions.addAll(createTransactionInfo(thread, testSuiteContext, resolver, scriptletCallStack));
	            }
            } else if(step instanceof CallStep) {
            	String testSuiteContextToUse = ((CallStep) step).getFrom();
            	if (testSuiteContextToUse == null && testSuiteContext != null) {
					testSuiteContextToUse = testSuiteContext;
				}
	            Scriptlet scriptlet = getScriptlet(testSuiteContextToUse, ((CallStep) step).getPath(), true);
				scriptletCallStack.addLast(Pair.of((CallStep) step, scriptlet));
				transactions.addAll(createTransactionInfo(scriptlet.getSteps(), testSuiteContextToUse, resolver, scriptletCallStack));
				scriptletCallStack.removeLast();
            }
        }
        return transactions;
    }

    public Scriptlet getScriptlet(String testSuiteContext, String path, boolean required) {
    	return scriptletCache.getScriptlet(testSuiteContext, path, testCase, required);
	}

    public TestCase getTestCase() {
        return testCase;
    }

    public TestCaseScope getScope() {
        return scope;
    }

    public void setScope(TestCaseScope scope) {
        this.scope = scope;
    }

    public String getSessionId() {
        return sessionId;
    }

	public String getTestCaseIdentifier() {
		return testCaseIdentifier;
	}

	public com.gitb.core.LogLevel getLogLevelToSignal() {
		if (logLevelIsExpression) {
			com.gitb.core.LogLevel resolvedLevel;
			String resolvedValue = (String) variableResolver.resolveVariable(testCase.getSteps().getLogLevel()).convertTo(DataType.STRING_DATA_TYPE).getValue();
			String warningMessage = null;
			if (resolvedValue == null || resolvedValue.isBlank()) {
				resolvedLevel = com.gitb.core.LogLevel.DEBUG;
				warningMessage = String.format("Unable to resolve test case log level using expression [%s]. Considering %s as default.", testCase.getSteps().getLogLevel(), resolvedLevel);
			} else {
				try {
					resolvedLevel = com.gitb.core.LogLevel.fromValue(resolvedValue);
					if (reportedInvalidLogLevel) {
						reportedInvalidLogLevel = false;
					}
				} catch (Exception e) {
					resolvedLevel = com.gitb.core.LogLevel.DEBUG;
					warningMessage = String.format("Invalid test case log level [%s]. Considering %s as default.", resolvedValue, resolvedLevel);
				}
			}
			if (warningMessage != null && !reportedInvalidLogLevel) {
				reportedInvalidLogLevel = true;
				logger.warn(MarkerFactory.getDetachedMarker(getSessionId()), warningMessage);
			}
			return resolvedLevel;
		} else {
			return logLevelToSignal;
		}
	}

	public MessagingContext getMessagingContext(String handler) {
        return messagingContexts.get(handler);
    }

	public TestCaseStateEnum getCurrentState() {
		return currentState;
	}

	public void setCurrentState(TestCaseStateEnum currentState) {
		this.currentState = currentState;
	}

    public void destroy() {
		if (dataFolder != null && Files.exists(dataFolder)) {
			try {
				FileUtils.deleteDirectory(dataFolder.toFile());
			} catch (IOException e) {
				logger.warn(String.format("Unable to delete data folder for session [%s]", sessionId), e);
			}
		}
        destroyMessagingContexts();
    }

	private void destroyMessagingContexts() {
		for(MessagingContext messagingContext:messagingContexts.values()){
			messagingContext.getHandler().endSession(messagingContext.getSessionId());
            messagingContext.cleanup();
			SessionManager.getInstance().removeMessagingSession(messagingContext.getSessionId());
		}
		messagingContexts.clear();
	}

    public Collection<MessagingContext> getMessagingContexts() {
        return messagingContexts.values();
    }

	public void addProcessingContext(String txId, ProcessingContext ctx) {
		processingContexts.put(txId, ctx);
		SessionManager.getInstance().mapProcessingSessionToTestSession(ctx.getSession(), sessionId);
	}

	public ProcessingContext getProcessingContext(String txId) {
		return processingContexts.get(txId);
	}

	public void removeProcessingContext(String txId) {
		if (processingContexts.containsKey(txId)) {
			processingContexts.remove(txId);
			SessionManager.getInstance().removeProcessingSession(processingContexts.get(txId).getSession());
		}
	}

	private static class MessagingContextBuilder {
		private final TransactionInfo transactionInfo;
		private final Map<Tuple<String>, ActorConfiguration> sutConfigurations;

		public MessagingContextBuilder(TransactionInfo transactionInfo) {
			this.transactionInfo = transactionInfo;
			this.sutConfigurations = new HashMap<>();
		}

		public MessagingContextBuilder addActorConfiguration(String actorIdToBeSimulated, String endpointNameToBeSimulated,
		                                                     ActorConfiguration sutActorConfiguration) {
			if (sutActorConfiguration != null) {
				Tuple<String> actorTuple = new Tuple<>(new String[]{
						actorIdToBeSimulated, endpointNameToBeSimulated,
						sutActorConfiguration.getActor(), sutActorConfiguration.getEndpoint()
				});

				if(!sutConfigurations.containsKey(actorTuple)) {
					sutConfigurations.put(actorTuple, sutActorConfiguration);
				}
			}

			return this;
		}

		private boolean isURL(String handler) {
			try {
				new URI(handler).toURL();
			} catch (Exception e) {
				return false;
			}
			return true;
		}

		private IMessagingHandler getRemoteMessagingHandler(TransactionInfo transactionInfo, String sessionId) {
			try {
				return new RemoteMessagingModuleClient(new URI(transactionInfo.handler).toURL(), transactionInfo.properties, sessionId);
			} catch (MalformedURLException e) {
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote validation module found with an malformed URL ["+transactionInfo.handler+"]"), e);
			} catch (URISyntaxException e) {
				throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote validation module found with an invalid URI syntax ["+transactionInfo.handler+"]"), e);
			}
		}

		public MessagingContext build(String sessionId) {
			IMessagingHandler messagingHandler;
			if (isURL(transactionInfo.handler)) {
				messagingHandler = getRemoteMessagingHandler(transactionInfo, sessionId);
			} else {
				messagingHandler = ModuleManager.getInstance().getMessagingHandler(transactionInfo.handler);
			}
			if (messagingHandler == null) {
				throw new IllegalStateException("Messaging handler for ["+transactionInfo.handler+"] could not be resolved");
			}

			List<ActorConfiguration> configurations = new ArrayList<>(sutConfigurations.values());
			Map<Tuple<String>, SUTConfiguration> sutHandlerConfigurations = new HashMap<>();

			InitiateResponse initiateResponse;
			if (messagingHandler instanceof AbstractMessagingHandler) {
				initiateResponse = ((AbstractMessagingHandler) messagingHandler).initiateWithSession(configurations, sessionId);
			} else {
				initiateResponse = messagingHandler.initiate(configurations);
			}
			if (!configurations.isEmpty()) {
				for(Map.Entry<Tuple<String>, ActorConfiguration> entry : sutConfigurations.entrySet()) {
					ActorConfiguration simulatedActor = ActorUtils.getActorConfiguration(initiateResponse.getActorConfiguration(), entry.getValue().getActor(), entry.getValue().getEndpoint());
					if (simulatedActor != null) {
						Tuple<String> actorTuple = entry.getKey();
						String actorIdToBeSimulated = actorTuple.getContents()[0];
						String endpointNameToBeSimulated = actorTuple.getContents()[1];

						simulatedActor.setActor(actorIdToBeSimulated);
						simulatedActor.setEndpoint(endpointNameToBeSimulated);
					}
				}

				for(Map.Entry<Tuple<String>, ActorConfiguration> entry : sutConfigurations.entrySet()) {
					Tuple<String> concatenatedActorTuple = entry.getKey();

					String actorIdToBeSimulated = concatenatedActorTuple.getContents()[0];
					String endpointNameToBeSimulated = concatenatedActorTuple.getContents()[1];

					String sutActorId = concatenatedActorTuple.getContents()[2];
					String sutEndpointName = concatenatedActorTuple.getContents()[3];

					ActorConfiguration simulatedActor = ActorUtils.getActorConfiguration(initiateResponse.getActorConfiguration(), actorIdToBeSimulated, endpointNameToBeSimulated);
					if (simulatedActor != null) {
						Tuple<String> sutActorTuple = new Tuple<>(new String[] {sutActorId, sutEndpointName});

						if(!sutHandlerConfigurations.containsKey(sutActorTuple)) {
							SUTConfiguration sutHandlerConfiguration = new SUTConfiguration();
							sutHandlerConfiguration.setActor(sutActorId);
							sutHandlerConfiguration.setEndpoint(sutEndpointName);

							sutHandlerConfigurations.put(sutActorTuple, sutHandlerConfiguration);
						}

						SUTConfiguration sutHandlerConfiguration = sutHandlerConfigurations.get(sutActorTuple);

						sutHandlerConfiguration.getConfigs().add(simulatedActor);
					}
				}
			}
			if (initiateResponse.getSessionId() == null) {
				throw new IllegalStateException("The remote messaging service must return a session identifier");
			}
			return new MessagingContext(messagingHandler, transactionInfo.handler, initiateResponse.getSessionId(),
				new ArrayList<>(sutHandlerConfigurations.values()));
		}

		public String getHandler() {
			return transactionInfo.handler;
		}
	}

    private static class TransactionInfo {
        public final String fromActorId;
	    public final String fromEndpointName;

        public final String toActorId;
	    public final String toEndpointName;

	    public final String handler;
	    public final Properties properties;

        public TransactionInfo(String fromActorId, String fromEndpointName, String toActorId, String toEndpointName, String handler, Properties properties) {
	        this.fromActorId = fromActorId;
	        this.fromEndpointName = fromEndpointName;
	        this.toActorId = toActorId;
	        this.toEndpointName = toEndpointName;
	        this.handler = handler;
	        this.properties = properties;
        }
    }
}
