package com.gitb;

import com.gitb.core.ValidationModule;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.module.IModuleLoader;
import com.gitb.processing.IProcessingHandler;
import com.gitb.repository.IFunctionRegistry;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.types.DataType;
import com.gitb.validation.IValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by serbay on 9/15/14.
 * Plug-in Manager for the TestEngine
 */
public class ModuleManager {

    private static Logger logger = LoggerFactory.getLogger(ModuleManager.class);

	private static ModuleManager instance;
    //Registered Validation Handler Modules
	private Map<String, IValidationHandler> validationHandlers;
	//Registered Messaging Handler Modules
    private Map<String, IMessagingHandler> messagingHandlers;
    private Map<String, IProcessingHandler> processingHandlers;
	//Registered Function Libraries for TDL Expression Language
    private Map<String, IFunctionRegistry> functionRegistries;
    //Registered Test Artifact Repositories
	private ITestCaseRepository testCaseRepository;
	//Registered Data Types
    private Map<String, DataType> dataTypes;

	private ModuleManager() {
		init();
	}

	private void init() {


        validationHandlers = new ConcurrentHashMap<>();
        messagingHandlers = new ConcurrentHashMap<>();
        functionRegistries = new ConcurrentHashMap<>();

        for(IValidationHandler validationHandler : ServiceLoader.load(IValidationHandler.class)) {
            validationHandlers.put(validationHandler.getModuleDefinition().getId(), validationHandler);
        }
        for(IMessagingHandler messagingHandler : ServiceLoader.load(IMessagingHandler.class)) {
            messagingHandlers.put(messagingHandler.getModuleDefinition().getId(), messagingHandler);
        }
        for(IFunctionRegistry functionRegistry : ServiceLoader.load(IFunctionRegistry.class)) {
            functionRegistries.put(functionRegistry.getName(), functionRegistry);
        }
        for(IModuleLoader moduleLoader : ServiceLoader.load(IModuleLoader.class)) {
            for(IValidationHandler validationHandler : moduleLoader.loadValidationHandlers()) {
                validationHandlers.put(validationHandler.getModuleDefinition().getId(), validationHandler);
            }
            for(IMessagingHandler messagingHandler : moduleLoader.loadMessagingHandlers()) {
                messagingHandlers.put(messagingHandler.getModuleDefinition().getId(), messagingHandler);
            }
			for(IProcessingHandler processingHandler : moduleLoader.loadProcessingHandlers()) {
				processingHandlers.put(processingHandler.getModuleDefinition().getId(), processingHandler);
			}
        }
        for(ITestCaseRepository testCaseRepository : ServiceLoader.load(ITestCaseRepository.class)) {
			if(testCaseRepository.getName().equals(CoreConfiguration.TEST_CASE_REPOSITORY)) {
				this.testCaseRepository = testCaseRepository;
				break;
			}
		}
        dataTypes = new ConcurrentHashMap<>();
        for(DataType dataType : ServiceLoader.load(DataType.class)) {
            dataTypes.put(dataType.getType(), dataType);
        }
        logger.info("ModuleManager has been initialized...");
	}

	public ITestCaseRepository getTestCaseRepository() {
		return testCaseRepository;
	}

	public IMessagingHandler getMessagingHandler(String name) {
		return messagingHandlers.get(name);
	}

	public IValidationHandler getValidationHandler(String name) {
		return validationHandlers.get(name);
	}

	public IProcessingHandler getProcessingHandler(String name) {
		return processingHandlers.get(name);
	}

	public IFunctionRegistry getFunctionRegistry(String name) {
		return functionRegistries.get(name);
	}

    public DataType getDataType(String name) {
        return dataTypes.get(name);
    }

	public Collection<IValidationHandler> getValidationHandlers() {
		return validationHandlers.values();
	}

	public Collection<IMessagingHandler> getMessagingHandlers() {
		return messagingHandlers.values();
	}

	public Collection<IProcessingHandler> getProcessingHandlers() {
		return processingHandlers.values();
	}

	public Collection<IFunctionRegistry> getFunctionRegistries() {
		return functionRegistries.values();
	}

    public Collection<DataType> getDataTypes() {
        return dataTypes.values();
    }

	public synchronized static ModuleManager getInstance() {
		if(instance == null) {
			instance = new ModuleManager();
		}
		return instance;
	}
}
