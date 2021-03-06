package com.gitb.module.remote;

import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.configuration2.CompositeConfiguration;
import org.apache.commons.configuration2.SystemConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

/**
 * Created by root on 3/11/15.
 */
public class Configuration {
    private final String messagingModuleFolder;
    private final String validationModuleFolder;

    public Configuration(String messagingModuleFolder, String validationModuleFolder) {
        this.messagingModuleFolder = messagingModuleFolder;
        this.validationModuleFolder = validationModuleFolder;
    }

    public String getMessagingModuleFolder() {
        return messagingModuleFolder;
    }

    public String getValidationModuleFolder() {
        return validationModuleFolder;
    }

    public static Configuration defaultConfiguration() {
        try {
            CompositeConfiguration config = new CompositeConfiguration();
            config.addConfiguration(new SystemConfiguration());
            config.addConfiguration(new Configurations().properties("remote-modules.properties"));

            return new Configuration(
                    config.getString("module.messaging.folder"),
                    config.getString("module.validation.folder")
            );
        } catch (ConfigurationException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Module properties could not be read"), e);
        }
    }
}
