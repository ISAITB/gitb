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

package com.gitb.engine;

import com.gitb.engine.messaging.MessagingHandler;
import com.gitb.engine.processing.ProcessingHandler;
import com.gitb.engine.repository.RemoteTestCaseRepository;
import com.gitb.engine.validation.ValidationHandler;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.processing.IProcessingHandler;
import com.gitb.repository.ITestCaseRepository;
import com.gitb.validation.IValidationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Created by serbay on 9/15/14.
 * Plug-in Manager for the TestEngine
 */
public class ModuleManager {

    private static final Logger logger = LoggerFactory.getLogger(ModuleManager.class);

	private static ModuleManager instance;
	private Map<String, Supplier<?>> validationHandlers;
    private Map<String, Supplier<?>> messagingHandlers;
    private Map<String, Supplier<?>> processingHandlers;
	private ITestCaseRepository testCaseRepository;

	private ModuleManager() {
		init();
	}

	private void init() {
		validationHandlers = new ConcurrentHashMap<>();
		messagingHandlers = new ConcurrentHashMap<>();
		processingHandlers = new ConcurrentHashMap<>();
		var classpathScanner = new ClassPathScanningCandidateComponentProvider(false);
		classpathScanner.addIncludeFilter(new AnnotationTypeFilter(ValidationHandler.class));
		classpathScanner.addIncludeFilter(new AnnotationTypeFilter(MessagingHandler.class));
		classpathScanner.addIncludeFilter(new AnnotationTypeFilter(ProcessingHandler.class));
		classpathScanner.findCandidateComponents("com.gitb.engine").forEach(item -> {
			try {
				var itemClass = Class.forName(item.getBeanClassName());
				var itemInstance = instantiateHandler(itemClass);
				if (itemInstance instanceof IMessagingHandler) {
					var annotation = itemClass.getAnnotation(MessagingHandler.class);
					addHandlerSupplier(messagingHandlers, itemClass, itemInstance, annotation.name(), annotation.singleton());
				} else if (itemInstance instanceof IValidationHandler) {
					var annotation = itemClass.getAnnotation(ValidationHandler.class);
					addHandlerSupplier(validationHandlers, itemClass, itemInstance, annotation.name(), annotation.singleton());
				} else if (itemInstance instanceof IProcessingHandler) {
					var annotation = itemClass.getAnnotation(ProcessingHandler.class);
					addHandlerSupplier(processingHandlers, itemClass, itemInstance, annotation.name(), annotation.singleton());
				} else {
					throw new IllegalStateException("Unrecognised class ["+item.getBeanClassName()+"] annotated as an embedded step handler.");
				}
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("Unable to load embedded step handlers", e);
			}
		});
		this.testCaseRepository = new RemoteTestCaseRepository();
        logger.info("ModuleManager has been initialized...");
	}

	private Object instantiateHandler(Class<?> handlerClass) {
		try {
			return handlerClass.getDeclaredConstructor().newInstance();
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
			throw new IllegalStateException("Embedded step handler did not have a no-argument accessible constructor", e);
		}
	}

	private void addHandlerSupplier(Map<String, Supplier<?>> handlerMap, Class<?> handlerClass, Object handlerInstance, String name, boolean singleton) {
		if (singleton) {
			// We can always use the same instance.
			handlerMap.put(name, () -> handlerInstance);
		} else {
			// Separate instances should be used.
			handlerMap.put(name, () -> instantiateHandler(handlerClass));
		}
	}

	public ITestCaseRepository getTestCaseRepository() {
		return testCaseRepository;
	}

	public IMessagingHandler getMessagingHandler(String name) {
		if (messagingHandlers.containsKey(name)) {
			var supplier = messagingHandlers.get(name);
			if (supplier != null) {
				return (IMessagingHandler) supplier.get();
			} else {
				throw new IllegalStateException("An invalid value [%s] was provided as a messaging handler".formatted(name));
			}
		} else {
			throw new IllegalStateException("An invalid value [%s] was provided as a messaging handler".formatted(name));
		}
	}

	public IValidationHandler getValidationHandler(String name) {
		if (validationHandlers.containsKey(name)) {
			var supplier = validationHandlers.get(name);
			if (supplier != null) {
				return (IValidationHandler) supplier.get();
			} else {
				throw new IllegalStateException("An invalid value [%s] was provided as a validation handler".formatted(name));
			}
		} else {
			throw new IllegalStateException("An invalid value [%s] was provided as a validation handler".formatted(name));
		}
	}

	public IProcessingHandler getProcessingHandler(String name) {
		if (processingHandlers.containsKey(name)) {
			var supplier = processingHandlers.get(name);
			if (supplier != null) {
				return (IProcessingHandler) supplier.get();
			} else {
				throw new IllegalStateException("An invalid value [%s] was provided as a processing handler".formatted(name));
			}
		} else {
			throw new IllegalStateException("An invalid value [%s] was provided as a processing handler".formatted(name));
		}
	}

	public synchronized static ModuleManager getInstance() {
		if(instance == null) {
			instance = new ModuleManager();
		}
		return instance;
	}

}
