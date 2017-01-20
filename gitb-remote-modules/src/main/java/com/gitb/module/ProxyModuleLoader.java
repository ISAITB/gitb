package com.gitb.module;

import com.gitb.core.ErrorCode;
import com.gitb.core.MessagingModule;
import com.gitb.core.ValidationModule;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.messaging.IMessagingHandler;
import com.gitb.module.remote.Configuration;
import com.gitb.module.validation.ProxyValidationHandler;
import com.gitb.processing.IProcessingHandler;
import com.gitb.utils.ErrorUtils;
import com.gitb.utils.JarUtils;
import com.gitb.utils.XMLUtils;
import com.gitb.validation.IValidationHandler;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by serbay.
 */
@MetaInfServices(IModuleLoader.class)
public class ProxyModuleLoader implements IModuleLoader {

    private static final Pattern VALIDATION_MODULES_FOLDER_PATTERN =
            Pattern.compile("^" + Configuration
                    .defaultConfiguration()
                    .getValidationModuleFolder().replace("/", "\\/") + ".+");
    private static final Pattern MESSAGING_MODULES_FOLDER_PATTERN =
            Pattern.compile("^" + Configuration
                    .defaultConfiguration()
                    .getMessagingModuleFolder().replace("/", "\\/") + ".+");

    private static final Logger logger = LoggerFactory.getLogger(ProxyModuleLoader.class);

	public ProxyModuleLoader() {
	}

	public void load() {
    }

    @Override
    public List<IValidationHandler> loadValidationHandlers() {
        try {
            logger.debug("Loading remote validation module proxies...");

            List<String> validationEntries = JarUtils.getJarFileNamesForPattern(getClass(), VALIDATION_MODULES_FOLDER_PATTERN);
            List<IValidationHandler> validationModules = new ArrayList<>();

            for(String entry : validationEntries) {
                if(entry.endsWith(".xml")) {
                    InputStream inputStream = getClass().getClassLoader().getResourceAsStream(entry);

                    ValidationModule module = XMLUtils.unmarshal(ValidationModule.class, new StreamSource(inputStream));
                    if(module.isIsRemote()) {
                        validationModules.add((IValidationHandler) ProxyValidationHandler.newInstance(module));
                    }
                }
            }

            return validationModules;
        } catch (Exception e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Could not load proxy modules"), e);
        }
    }

    @Override
    public List<IMessagingHandler> loadMessagingHandlers() {
        try {
            logger.debug("Loading remote messaging module proxies...");

            List<String> messagingEntries = JarUtils.getJarFileNamesForPattern(getClass(), MESSAGING_MODULES_FOLDER_PATTERN);
            List<IMessagingHandler> messagingModules = new ArrayList<>();

            for(String entry : messagingEntries) {
                if(entry.endsWith(".xml")) {
                    InputStream inputStream = getClass().getResourceAsStream(entry);

                    MessagingModule module = XMLUtils.unmarshal(MessagingModule.class, new StreamSource(inputStream));
                    if(module.isIsRemote()) {

                    }
                }
            }

            return messagingModules;
        } catch (Exception e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Could not load proxy modules"), e);
        }
    }

    @Override
    public List<IProcessingHandler> loadProcessingHandlers() {
        return Collections.emptyList();
    }
}
