package com.gitb.engine.testcase;

import com.gitb.ModuleManager;
import com.gitb.core.*;
import com.gitb.engine.expr.resolvers.VariableResolver;
import com.gitb.engine.messaging.MessagingContext;
import com.gitb.engine.processing.ProcessingContext;
import com.gitb.engine.remote.messaging.RemoteMessagingModuleClient;
import com.gitb.engine.utils.ScriptletCache;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.messaging.layer.AbstractMessagingHandler;
import com.gitb.messaging.model.InitiateResponse;
import com.gitb.tbs.SUTConfiguration;
import com.gitb.tdl.*;
import com.gitb.types.BooleanType;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.types.MapType;
import com.gitb.utils.ActorUtils;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.map.Tuple;
import org.apache.commons.codec.binary.Base64;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by serbay on 9/3/14.
 *
 * Class that encapsulates all the necessary information for a test case execution session
 */
public class TestCaseContext {

	/**
	 * The map containing the status values for each executed step.
	 */
	public static final String STEP_SUCCESS_MAP = "STEP_SUCCESS";

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
     * Test case scope where all the variables, artifacts are stored and accessed.
     */
    private TestCaseScope scope;

    /**
     * Configurations of SUTs which are provided by the users. Map key is set to (actorId, endpointName) tuples.
     * Note that in the case of an actor having just one endpoint, an empty endpoint value is possible. In this
     * case, the key part will be in the (actorId, null) form.
     */
    private Map<Tuple<String>, ActorConfiguration> sutConfigurations;

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
		 * Execution is completed or stopped by user or exit with some error
		 */
        STOPPED,
		/**
		 * Execution is in the process of stopping
		 */
		STOPPING
    }

	private LogLevel logLevelToSignal = LogLevel.DEBUG;

    public TestCaseContext(TestCase testCase, String sessionId) {
        this.currentState = TestCaseStateEnum.IDLE;
        this.testCase = testCase;
        this.sessionId = sessionId;
        this.sutConfigurations = new ConcurrentHashMap<>();
        this.sutHandlerConfigurations = new ConcurrentHashMap<>();
        this.messagingContexts = new ConcurrentHashMap<>();
		this.processingContexts = new ConcurrentHashMap<>();
        this.scope = new TestCaseScope(this, testCase.getImports());
		if (testCase.getSteps() != null) {
			this.logLevelToSignal = testCase.getSteps().getLogLevel();
		}
        addStepStatus();
        processVariables();

        actorRoles = new HashMap<>();

        // Initialize configuration lists for SutHandlerConfigurations
        for(TestRole role : this.testCase.getActors().getActor()) {
            actorRoles.put(role.getId(), role);
        }
    }

	public ScriptletCache getScriptletCache() {
    	return this.scriptletCache;
	}

    private void addStepStatusForStep(MapType map, Object step) {
    	if (step != null) {
			if (step instanceof TestConstruct) {
				if (((TestConstruct)step).getId() != null) {
					map.addItem(((TestConstruct)step).getId(), new BooleanType(false));
				}
			}
			if (step instanceof IfStep) {
				addStepStatusForStep(map, ((IfStep)step).getThen());
				addStepStatusForStep(map, ((IfStep)step).getElse());
			} else if (step instanceof WhileStep) {
				addStepStatusForStep(map, ((WhileStep)step).getDo());
			} else if (step instanceof RepeatUntilStep) {
				addStepStatusForStep(map, ((RepeatUntilStep)step).getDo());
			} else if (step instanceof ForEachStep) {
				addStepStatusForStep(map, ((ForEachStep)step).getDo());
			} else if (step instanceof FlowStep) {
				if (((FlowStep)step).getThread() != null) {
					for (Sequence flowSequence: ((FlowStep)step).getThread()) {
						addStepStatusForStep(map, flowSequence);
					}
				}
			} else if (step instanceof Sequence) {
				for (Object sequenceStep: ((Sequence) step).getSteps()) {
					addStepStatusForStep(map, sequenceStep);
				}
			}
		}
	}

	private void addStepStatus() {
		TestCaseScope.ScopedVariable variable = scope.createVariable("STEP_SUCCESS");
		MapType map = (MapType) DataTypeFactory.getInstance().create(DataType.MAP_DATA_TYPE);
		addStepStatusForStep(map, testCase.getSteps());
		variable.setValue(map);
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

		addSpecialConfiguration("DOMAIN", domainConfiguration);
		addSpecialConfiguration("ORGANISATION", organisationConfiguration);
		addSpecialConfiguration("SYSTEM", systemConfiguration);

		for(ActorConfiguration actorConfiguration : configurations) {
		    Tuple<String> actorIdEndpointTupleKey = new Tuple<>(new String[] {actorConfiguration.getActor(), actorConfiguration.getEndpoint()});
		    sutConfigurations.put(actorIdEndpointTupleKey, actorConfiguration);
		    sutHandlerConfigurations.put(actorIdEndpointTupleKey, new CopyOnWriteArrayList<ActorConfiguration>());
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

	private List<SUTConfiguration> configureSimulatedActorsForSUTs(List<ActorConfiguration> configurations) {
		List<TransactionInfo> testCaseTransactions = createTransactionInfo(testCase.getSteps(), null);
		Map<String, MessagingContextBuilder> messagingContextBuilders = new HashMap<>();

        // collect all transactions needed to configure the messaging handlers
		for(TransactionInfo transactionInfo : testCaseTransactions) {
			TestRole fromRole = actorRoles.get(transactionInfo.fromActorId);
			TestRole toRole = actorRoles.get(transactionInfo.toActorId);

			if(!messagingContextBuilders.containsKey(transactionInfo.handler)) {
				messagingContextBuilders.put(transactionInfo.handler, new MessagingContextBuilder(transactionInfo));
			}

			MessagingContextBuilder builder = messagingContextBuilders.get(transactionInfo.handler);
            builder.incrementTransactionCount();

			if(fromRole.getRole() == TestRoleEnumeration.SUT
				&& toRole.getRole() == TestRoleEnumeration.SUT) {
				// both of them are SUTs, messaging handler acts as a proxy between them
				builder.addActorConfiguration(transactionInfo.toActorId, transactionInfo.toEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.fromActorId, transactionInfo.fromEndpointName))
					   .addActorConfiguration(transactionInfo.fromActorId, transactionInfo.fromEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.toActorId, transactionInfo.toEndpointName));
			} else if(fromRole.getRole() == TestRoleEnumeration.SUT && toRole.getRole() == TestRoleEnumeration.SIMULATED) {
				// just one of them is SUT, messaging handler acts as the simulated actor
				builder.addActorConfiguration(transactionInfo.toActorId, transactionInfo.toEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.fromActorId, transactionInfo.fromEndpointName));
			} else if(fromRole.getRole() == TestRoleEnumeration.SIMULATED && toRole.getRole() == TestRoleEnumeration.SUT) {
				// just one of them is SUT, messaging handler acts as the simulated actor
				builder.addActorConfiguration(transactionInfo.fromActorId, transactionInfo.fromEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.toActorId, transactionInfo.toEndpointName));
			} else {
				// both of them are simulated actors
				builder.addActorConfiguration(transactionInfo.toActorId, transactionInfo.toEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.fromActorId, transactionInfo.fromEndpointName))
						.addActorConfiguration(transactionInfo.fromActorId, transactionInfo.fromEndpointName, ActorUtils.getActorConfiguration(configurations, transactionInfo.toActorId, transactionInfo.toEndpointName));
			}
		}

		List<SUTConfiguration> handlerConfigurations = new ArrayList<>();

		for(MessagingContextBuilder builder : messagingContextBuilders.values()) {
			MessagingContext messagingContext = builder.build(sessionId);
			handlerConfigurations.addAll(messagingContext.getSutHandlerConfigurations());

			messagingContexts.put(builder.getHandler(), messagingContext);
		}

        List<SUTConfiguration> configurationsBySUTActor = groupGeneratedSUTConfigurationsBySUTActor(handlerConfigurations);

        for(SUTConfiguration sutConfiguration : configurationsBySUTActor) {
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


		List<SUTConfiguration> groupedSUTConfigurations = new ArrayList<>();

		for(SUTConfiguration group : groups.values()) {
			groupedSUTConfigurations.add(group);
		}

		return groupedSUTConfigurations;
	}

    /**
     * Traverses test case steps to generate transaction information containing (from, handler, to) information
     * @param sequence test step sequence
     * @return transactions occurring in the given sequence
     */
    private List<TransactionInfo> createTransactionInfo(Sequence sequence, String testSuiteContext) {
        List<TransactionInfo> transactions = new ArrayList<>();
		VariableResolver resolver = new VariableResolver(scope);
        for(Object step : sequence.getSteps()) {
            if(step instanceof Sequence) {
                transactions.addAll(createTransactionInfo((Sequence) step, testSuiteContext));
            } else if(step instanceof BeginTransaction) {
                BeginTransaction beginTransactionStep = (BeginTransaction) step;

	            String fromActorId = ActorUtils.extractActorId(beginTransactionStep.getFrom());
	            String fromEndpoint = ActorUtils.extractEndpointName(beginTransactionStep.getFrom());

	            String toActorId = ActorUtils.extractActorId(beginTransactionStep.getTo());
	            String toEndpoint = ActorUtils.extractEndpointName(beginTransactionStep.getTo());

				String handlerIdentifier = beginTransactionStep.getHandler();
				if (resolver.isVariableReference(handlerIdentifier)) {
					handlerIdentifier = resolver.resolveVariableAsString(handlerIdentifier).toString();
				}

                transactions.add(new TransactionInfo(fromActorId, fromEndpoint, toActorId, toEndpoint, handlerIdentifier, TestCaseUtils.getStepProperties(beginTransactionStep.getProperty(), resolver)));
            } else if (step instanceof IfStep) {
	            transactions.addAll(createTransactionInfo(((IfStep) step).getThen(), testSuiteContext));
	            if (((IfStep) step).getElse() != null) {
					transactions.addAll(createTransactionInfo(((IfStep) step).getElse(), testSuiteContext));
				}
            } else if(step instanceof WhileStep) {
	            transactions.addAll(createTransactionInfo(((WhileStep) step).getDo(), testSuiteContext));
            } else if(step instanceof ForEachStep) {
	            transactions.addAll(createTransactionInfo(((ForEachStep) step).getDo(), testSuiteContext));
            } else if(step instanceof RepeatUntilStep) {
	            transactions.addAll(createTransactionInfo(((RepeatUntilStep) step).getDo(), testSuiteContext));
            } else if(step instanceof FlowStep) {
	            for(Sequence thread : ((FlowStep) step).getThread()) {
		            transactions.addAll(createTransactionInfo(thread, testSuiteContext));
	            }
            } else if(step instanceof CallStep) {
            	String testSuiteContextToUse = ((CallStep) step).getFrom();
            	if (testSuiteContextToUse == null && testSuiteContext != null) {
					testSuiteContextToUse = testSuiteContext;
				}
	            Scriptlet scriptlet = getScriptlet(testSuiteContextToUse, ((CallStep) step).getPath(), true);
				transactions.addAll(createTransactionInfo(scriptlet.getSteps(), testSuiteContextToUse));
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

	public LogLevel getLogLevelToSignal() {
		return logLevelToSignal;
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

    public void destroy(){
        destroyMessagingContexts();
    }

	private void destroyMessagingContexts() {
		for(MessagingContext messagingContext:messagingContexts.values()){
			messagingContext.getHandler().endSession(messagingContext.getSessionId());
            messagingContext.cleanup();
		}
		messagingContexts.clear();
	}

    public MessagingContext endMessagingContext(String handler) {
        return messagingContexts.remove(handler);
    }

    public Collection<MessagingContext> getMessagingContexts() {
        return messagingContexts.values();
    }

	public void addProcessingContext(String txId, ProcessingContext ctx) {
		processingContexts.put(txId, ctx);
	}

	public ProcessingContext getProcessingContext(String txId) {
		return processingContexts.get(txId);
	}

	public void removeProcessingContext(String txId) {
		processingContexts.remove(txId);
	}

	private static class MessagingContextBuilder {
		private final TransactionInfo transactionInfo;
		private final Map<Tuple<String>, ActorConfiguration> sutConfigurations;
        private int transactionCount;

		public MessagingContextBuilder(TransactionInfo transactionInfo) {
			this.transactionInfo = transactionInfo;
			this.sutConfigurations = new HashMap<>();
            this.transactionCount = 0;
		}

        public void incrementTransactionCount() {
            this.transactionCount++;
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
				throw new IllegalStateException("Validation handler for ["+transactionInfo.handler+"] could not be resolved");
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
					ActorConfiguration simulatedActor = ActorUtils.getActorConfiguration(initiateResponse.getActorConfigurations(), entry.getValue().getActor(), entry.getValue().getEndpoint());
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

					ActorConfiguration simulatedActor = ActorUtils.getActorConfiguration(initiateResponse.getActorConfigurations(), actorIdToBeSimulated, endpointNameToBeSimulated);
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
			return new MessagingContext(messagingHandler, initiateResponse.getSessionId(),
				initiateResponse.getActorConfigurations(), new ArrayList<>(sutHandlerConfigurations.values()), transactionCount);
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
