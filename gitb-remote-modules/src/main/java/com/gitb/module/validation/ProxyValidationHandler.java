package com.gitb.module.validation;

import com.gitb.core.Configuration;
import com.gitb.core.ValidationModule;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.types.DataType;
import com.gitb.validation.IValidationHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

/**
 * Created by serbay.
 */
public class ProxyValidationHandler implements InvocationHandler {

	private final ValidationModule validationModule;

	public ProxyValidationHandler(ValidationModule validationModule) {
		this.validationModule = validationModule;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		switch (method.getName()) {
			case "getModuleDefinition":
				return validationModule;
			case "validate":
				List<Configuration> configurations = (List<Configuration>) args[0];
				Map<String, DataType> inputs = (Map<String, DataType>) args[1];

				return RemoteValidationModuleClient.validate(validationModule, configurations, inputs);
			default:
				throw new InvocationTargetException(new GITBEngineInternalError("Method is not found"));
		}
	}

	public static Object newInstance(ValidationModule validationModule) {
		return Proxy.newProxyInstance(
			IValidationHandler.class.getClassLoader(),
			new Class<?>[] {IValidationHandler.class},
			new ProxyValidationHandler(validationModule));
	}
}
