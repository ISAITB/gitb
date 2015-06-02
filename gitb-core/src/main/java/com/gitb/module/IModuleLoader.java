package com.gitb.module;

import com.gitb.core.MessagingModule;
import com.gitb.core.ValidationModule;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.validation.IValidationHandler;

import java.util.List;

/**
 * Created by serbay.
 */
public interface IModuleLoader {
	public List<IValidationHandler> loadValidationHandlers();
	public List<IMessagingHandler> loadMessagingHandlers();
}
