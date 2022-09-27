package com.gitb.vs.tdl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExternalConfiguration {

    private Map<String, MessagingHandlerConfiguration> embeddedMessagingHandlers = new HashMap<>();
    private Map<String, ValidationHandlerConfiguration> embeddedValidationHandlers = new HashMap<>();
    private Map<String, ProcessingHandlerConfiguration> embeddedProcessingHandlers = new HashMap<>();
    private Set<String> externalActorIds = new HashSet<>();
    private Set<String> externalParameters = new HashSet<>();
    private Set<String> dataTypes = new HashSet<>();
    private Set<String> containerDataTypes = new HashSet<>();
    private Set<String> containedDataTypes = new HashSet<>();
    private Set<String> acceptedMimeTypes = new HashSet<>();

    public Map<String, MessagingHandlerConfiguration> getEmbeddedMessagingHandlers() {
        return embeddedMessagingHandlers;
    }

    public void setEmbeddedMessagingHandlers(Map<String, MessagingHandlerConfiguration> embeddedMessagingHandlers) {
        this.embeddedMessagingHandlers = embeddedMessagingHandlers;
    }

    public Map<String, ValidationHandlerConfiguration> getEmbeddedValidationHandlers() {
        return embeddedValidationHandlers;
    }

    public void setEmbeddedValidationHandlers(Map<String, ValidationHandlerConfiguration> embeddedValidationHandlers) {
        this.embeddedValidationHandlers = embeddedValidationHandlers;
    }

    public Map<String, ProcessingHandlerConfiguration> getEmbeddedProcessingHandlers() {
        return embeddedProcessingHandlers;
    }

    public void setEmbeddedProcessingHandlers(Map<String, ProcessingHandlerConfiguration> embeddedProcessingHandlers) {
        this.embeddedProcessingHandlers = embeddedProcessingHandlers;
    }

    public Set<String> getExternalActorIds() {
        return externalActorIds;
    }

    public void setExternalActorIds(Set<String> externalActorIds) {
        this.externalActorIds = externalActorIds;
    }

    public Set<String> getExternalParameters() {
        return externalParameters;
    }

    public void setExternalParameters(Set<String> externalParameters) {
        this.externalParameters = externalParameters;
    }

    public Set<String> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(Set<String> dataTypes) {
        this.dataTypes = dataTypes;
    }

    public Set<String> getContainerDataTypes() {
        return containerDataTypes;
    }

    public void setContainerDataTypes(Set<String> containerDataTypes) {
        this.containerDataTypes = containerDataTypes;
    }

    public Set<String> getContainedDataTypes() {
        return containedDataTypes;
    }

    public void setContainedDataTypes(Set<String> containedDataTypes) {
        this.containedDataTypes = containedDataTypes;
    }

    public Set<String> getAcceptedMimeTypes() {
        return acceptedMimeTypes;
    }

    public void setAcceptedMimeTypes(Set<String> acceptedMimeTypes) {
        this.acceptedMimeTypes = acceptedMimeTypes;
    }

    public static class MessagingHandlerConfiguration extends BasicConfiguration {

        private Set<String> requiredTxConfigs = new HashSet<>();
        private Set<String> optionalTxConfigs = new HashSet<>();
        private Set<String> requiredSendConfigs = new HashSet<>();
        private Set<String> optionalSendConfigs = new HashSet<>();
        private Set<String> requiredReceiveConfigs = new HashSet<>();
        private Set<String> optionalReceiveConfigs = new HashSet<>();

        public Set<String> getRequiredTxConfigs() {
            return requiredTxConfigs;
        }

        public Set<String> getOptionalTxConfigs() {
            return optionalTxConfigs;
        }

        public Set<String> getRequiredSendConfigs() {
            return requiredSendConfigs;
        }

        public Set<String> getOptionalSendConfigs() {
            return optionalSendConfigs;
        }

        public Set<String> getRequiredReceiveConfigs() {
            return requiredReceiveConfigs;
        }

        public Set<String> getOptionalReceiveConfigs() {
            return optionalReceiveConfigs;
        }
    }

    public static class ProcessingHandlerConfiguration {

        private Map<String, BasicConfiguration> operations = new HashMap<>();
        private Set<String> requiredConfigs = new HashSet<>();
        private Set<String> optionalConfigs = new HashSet<>();

        public Map<String, BasicConfiguration> getOperations() {
            return operations;
        }

        public Set<String> getRequiredConfigs() {
            return requiredConfigs;
        }

        public Set<String> getOptionalConfigs() {
            return optionalConfigs;
        }

    }

    public static class ValidationHandlerConfiguration extends BasicConfiguration {

        private Set<String> requiredConfigs = new HashSet<>();
        private Set<String> optionalConfigs = new HashSet<>();

        public Set<String> getRequiredConfigs() {
            return requiredConfigs;
        }

        public Set<String> getOptionalConfigs() {
            return optionalConfigs;
        }
    }

    public static class BasicConfiguration {

        private Set<String> requiredInputs = new HashSet<>();
        private Set<String> optionalInputs = new HashSet<>();

        public Set<String> getRequiredInputs() {
            return requiredInputs;
        }

        public Set<String> getOptionalInputs() {
            return optionalInputs;
        }
    }

}
