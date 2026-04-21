/*
 * Copyright (C) 2026 European Union
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

package com.gitb.utils;

import com.gitb.core.*;
import com.gitb.ms.FinalizeRequest;
import com.gitb.ms.InitiateRequest;
import com.gitb.ms.InitiateResponse;
import com.gitb.ms.NotifyForMessageRequest;
import com.gitb.ms.ReceiveRequest;
import com.gitb.ms.SendRequest;
import com.gitb.ms.SendResponse;
import com.gitb.model.tr.SeverityLevel;
import com.gitb.ps.ProcessingModule;
import com.gitb.ps.ProcessingOperation;
import com.gitb.ps.ProcessRequest;
import com.gitb.ps.ProcessResponse;
import com.gitb.tr.*;
import com.gitb.tr.ObjectFactory;
import com.gitb.vs.ValidateRequest;
import com.gitb.vs.ValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigInteger;
import java.util.List;

public class ModelUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ModelUtils.class);
    private static final ObjectFactory OBJECT_FACTORY_TR = new ObjectFactory();

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a list of model typed parameters into the wrapped {@link TypedParameters} container
     * expected by the Jakarta JAXB types.
     *
     * @param source the list of model typed parameters (may be {@code null} or empty)
     * @return a populated {@link TypedParameters} instance, or {@code null} if the list is empty
     */
    private static TypedParameters toTypedParameters(List<com.gitb.model.core.TypedParameter> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        TypedParameters result = new TypedParameters();
        result.getParam().addAll(source.stream().map(ModelUtils::fromModel).toList());
        return result;
    }

    /**
     * Converts a Jakarta {@link TypedParameters} container to a list of model typed parameters.
     *
     * @param source the Jakarta typed parameters (may be {@code null})
     * @return a list of model typed parameters, or an empty list if source is {@code null}
     */
    private static List<com.gitb.model.core.TypedParameter> toModelTypedParametersList(TypedParameters source) {
        if (source == null || source.getParam() == null || source.getParam().isEmpty()) {
            return List.of();
        }
        return source.getParam().stream().map(ModelUtils::toModel).toList();
    }

    // -------------------------------------------------------------------------
    // com.gitb.core.*
    // -------------------------------------------------------------------------

    /**
     * Converts a model {@link com.gitb.model.core.Configuration} to a Jakarta
     * {@link Configuration}.
     *
     * @param source the model configuration
     * @return the converted configuration, or {@code null} if source is {@code null}
     */
    public static Configuration fromModel(com.gitb.model.core.Configuration source) {
        Configuration target = null;
        if (source != null) {
            target = new Configuration();
            target.setName(source.getName());
            target.setValue(source.getValue());
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link com.gitb.model.core.Configuration} to a model
     * {@link Configuration}.
     *
     * @param source the Jakarta configuration
     * @return the converted configuration, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.core.Configuration toModel(Configuration source) {
        com.gitb.model.core.Configuration target = null;
        if (source != null) {
            target = new com.gitb.model.core.Configuration();
            target.setName(source.getName());
            target.setValue(source.getValue());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.core.ActorConfiguration} to a Jakarta
     * {@link ActorConfiguration}.
     *
     * @param source the model actor configuration
     * @return the converted actor configuration, or {@code null} if source is {@code null}
     */
    public static ActorConfiguration fromModel(com.gitb.model.core.ActorConfiguration source) {
        ActorConfiguration target = null;
        if (source != null) {
            target = new ActorConfiguration();
            target.setActor(source.getActor());
            target.setEndpoint(source.getEndpoint());
            target.getConfig().addAll(source.getConfig().stream().map(ModelUtils::fromModel).toList());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.core.Metadata} to a Jakarta {@link Metadata}.
     *
     * @param source the model metadata
     * @return the converted metadata, or {@code null} if source is {@code null}
     */
    public static Metadata fromModel(com.gitb.model.core.Metadata source) {
        Metadata target = null;
        if (source != null) {
            target = new Metadata();
            target.setName(source.getName());
            target.setVersion(source.getVersion());
            target.setAuthors(source.getAuthors());
            target.setDescription(source.getDescription());
            target.setLastModified(source.getLastModified());
            target.setPublished(source.getPublished());
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link Metadata} to a model {@link com.gitb.model.core.Metadata}.
     *
     * @param source the Jakarta metadata
     * @return the converted metadata, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.core.Metadata toModel(Metadata source) {
        com.gitb.model.core.Metadata target = null;
        if (source != null) {
            target = new com.gitb.model.core.Metadata();
            target.setName(source.getName());
            target.setVersion(source.getVersion());
            target.setAuthors(source.getAuthors());
            target.setDescription(source.getDescription());
            target.setLastModified(source.getLastModified());
            target.setPublished(source.getPublished());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.core.Parameter} to a Jakarta {@link Parameter}.
     *
     * @param source the model parameter
     * @return the converted parameter, or {@code null} if source is {@code null}
     */
    public static Parameter fromModel(com.gitb.model.core.Parameter source) {
        Parameter target = null;
        if (source != null) {
            target = new Parameter();
            target.setName(source.getName());
            target.setValue(source.getValue());
            target.setDesc(source.getDesc());
            target.setLabel(source.getLabel());
            target.setKind(fromModel(source.getKind()));
            target.setUse(fromModel(source.getUse()));
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link Parameter} to a model {@link com.gitb.model.core.Parameter}.
     *
     * @param source the Jakarta parameter
     * @return the converted parameter, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.core.Parameter toModel(Parameter source) {
        com.gitb.model.core.Parameter target = null;
        if (source != null) {
            target = new com.gitb.model.core.Parameter();
            target.setName(source.getName());
            target.setValue(source.getValue());
            target.setDesc(source.getDesc());
            target.setLabel(source.getLabel());
            target.setKind(toModel(source.getKind()));
            target.setUse(toModel(source.getUse()));
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.core.TypedParameter} to a Jakarta
     * {@link TypedParameter}.
     *
     * @param source the model typed parameter
     * @return the converted typed parameter, or {@code null} if source is {@code null}
     */
    public static TypedParameter fromModel(com.gitb.model.core.TypedParameter source) {
        TypedParameter target = null;
        if (source != null) {
            target = new TypedParameter();
            target.setName(source.getName());
            target.setValue(source.getValue());
            target.setDesc(source.getDesc());
            target.setLabel(source.getLabel());
            target.setKind(fromModel(source.getKind()));
            target.setUse(fromModel(source.getUse()));
            target.setType(source.getType());
            target.setEncoding(source.getEncoding());
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link TypedParameter} to a model {@link com.gitb.model.core.TypedParameter}.
     *
     * @param source the Jakarta typed parameter
     * @return the converted typed parameter, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.core.TypedParameter toModel(TypedParameter source) {
        com.gitb.model.core.TypedParameter target = null;
        if (source != null) {
            target = new com.gitb.model.core.TypedParameter();
            target.setName(source.getName());
            target.setValue(source.getValue());
            target.setDesc(source.getDesc());
            target.setLabel(source.getLabel());
            target.setKind(toModel(source.getKind()));
            target.setUse(toModel(source.getUse()));
            target.setType(source.getType());
            target.setEncoding(source.getEncoding());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.core.ConfigurationType} enum value to the Jakarta
     * equivalent.
     *
     * @param source the model enum value
     * @return the converted enum value, or {@code null} if source is {@code null}
     */
    public static ConfigurationType fromModel(com.gitb.model.core.ConfigurationType source) {
        ConfigurationType target = null;
        if (source != null) {
            target = ConfigurationType.fromValue(source.value());
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link com.gitb.core.ConfigurationType} enum value to the model
     * equivalent.
     *
     * @param source the model enum value
     * @return the converted enum value, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.core.ConfigurationType toModel(ConfigurationType source) {
        com.gitb.model.core.ConfigurationType target = null;
        if (source != null) {
            target = com.gitb.model.core.ConfigurationType.fromValue(source.value());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.core.UsageEnumeration} enum value to the Jakarta
     * equivalent.
     *
     * @param source the model enum value
     * @return the converted enum value, or {@code null} if source is {@code null}
     */
    public static UsageEnumeration fromModel(com.gitb.model.core.UsageEnumeration source) {
        UsageEnumeration target = null;
        if (source != null) {
            target = UsageEnumeration.fromValue(source.value());
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link com.gitb.core.UsageEnumeration} enum value to the model
     * equivalent.
     *
     * @param source the model enum value
     * @return the converted enum value, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.core.UsageEnumeration toModel(com.gitb.core.UsageEnumeration source) {
        com.gitb.model.core.UsageEnumeration target = null;
        if (source != null) {
            target = com.gitb.model.core.UsageEnumeration.fromValue(source.value());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.core.ValueEmbeddingEnumeration} enum value to the
     * Jakarta equivalent.
     *
     * @param source the model enum value
     * @return the converted enum value, defaulting to {@link ValueEmbeddingEnumeration#STRING} if
     *         source is {@code null}
     */
    public static ValueEmbeddingEnumeration fromModel(com.gitb.model.core.ValueEmbeddingEnumeration source) {
        return (source == null) ? ValueEmbeddingEnumeration.STRING : ValueEmbeddingEnumeration.fromValue(source.value());
    }

    /**
     * Converts a Jakarta {@link com.gitb.core.ValueEmbeddingEnumeration} enum value to the
     * model equivalent.
     *
     * @param source the model enum value
     * @return the converted enum value, defaulting to {@link ValueEmbeddingEnumeration#STRING} if
     *         source is {@code null}
     */
    public static com.gitb.model.core.ValueEmbeddingEnumeration toModel(ValueEmbeddingEnumeration source) {
        return (source == null) ? com.gitb.model.core.ValueEmbeddingEnumeration.STRING : com.gitb.model.core.ValueEmbeddingEnumeration.fromValue(source.value());
    }

    /**
     * Converts a model {@link com.gitb.model.core.LogLevel} enum value to the Jakarta equivalent.
     *
     * @param source the model enum value
     * @return the converted enum value, or {@code null} if source is {@code null}
     */
    public static LogLevel fromModel(com.gitb.model.core.LogLevel source) {
        LogLevel target = null;
        if (source != null) {
            target = LogLevel.fromValue(source.value());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.core.AnyContent} to a Jakarta {@link AnyContent}.
     *
     * @param source the API any-content value
     * @return the converted value, or {@code null} if source is {@code null}
     */
    public static AnyContent fromModel(com.gitb.model.core.AnyContent source) {
        AnyContent target = null;
        if (source != null) {
            target = new AnyContent();
            target.setForContext(source.isForContext());
            target.setForDisplay(source.isForDisplay());
            target.setEmbeddingMethod(fromModel(source.getEmbeddingMethod()));
            target.setType(source.getType());
            target.setMimeType(source.getMimeType());
            target.setValue(source.getValue());
            target.setName(source.getName());
            target.setEncoding(source.getEncoding());
            target.getItem().addAll(source.getItem().stream().map(ModelUtils::fromModel).toList());
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link com.gitb.core.AnyContent} to a model {@link AnyContent}.
     *
     * @param source the API any-content value
     * @return the converted value, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.core.AnyContent toModel(com.gitb.core.AnyContent source) {
        com.gitb.model.core.AnyContent target = null;
        if (source != null) {
            target = com.gitb.model.core.AnyContent.builder()
                    .withForContext(source.isForContext())
                    .withForDisplay(source.isForDisplay())
                    .withEmbeddingMethod(toModel(source.getEmbeddingMethod()))
                    .withType(source.getType())
                    .withMimeType(source.getMimeType())
                    .withValue(source.getValue())
                    .withName(source.getName())
                    .withEncoding(source.getEncoding())
                    .withItem(source.getItem().stream().map(ModelUtils::toModel).toArray(com.gitb.model.core.AnyContent[]::new))
                    .build();
        }
        return target;
    }

    // -------------------------------------------------------------------------
    // com.gitb.tr.*
    // -------------------------------------------------------------------------

    /**
     * Converts a transport-report API model to the internal TAR representation.
     *
     * @param source the API report model
     * @return the converted report, or {@code null} if source is {@code null}
     */
    public static TAR fromModel(com.gitb.model.tr.TAR source) {
        TAR target = null;
        if (source != null) {
            target = new TAR();
            target.setName(source.getName());
            target.setId(source.getId());
            if (source.getResult() != null) {
                target.setResult(TestResultType.fromValue(source.getResult().value()));
            }
            if (source.getDate() != null) {
                try {
                    target.setDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(source.getDate().toOffsetDateTime().toString()));
                } catch (DatatypeConfigurationException e) {
                    LOG.warn("Unable to parse report date. Setting current time.", e);
                    try { target.setDate(XMLDateTimeUtils.getXMLGregorianCalendarDateTime()); } catch (DatatypeConfigurationException ex) { /* Ignore */ }
                }
            }
            if (source.getCounters() != null) {
                target.setCounters(new ValidationCounters());
                target.getCounters().setNrOfErrors((source.getCounters().getNrOfErrors() == null) ? null : BigInteger.valueOf(source.getCounters().getNrOfErrors()));
                target.getCounters().setNrOfWarnings((source.getCounters().getNrOfWarnings() == null) ? null : BigInteger.valueOf(source.getCounters().getNrOfWarnings()));
                target.getCounters().setNrOfAssertions((source.getCounters().getNrOfAssertions() == null) ? null : BigInteger.valueOf(source.getCounters().getNrOfAssertions()));
            }
            if (source.getOverview() != null) {
                target.setOverview(new ValidationOverview());
                target.getOverview().setCustomizationID(source.getOverview().getCustomizationID());
                target.getOverview().setNote(source.getOverview().getNote());
                target.getOverview().setProfileID(source.getOverview().getProfileID());
                target.getOverview().setTransactionID(source.getOverview().getTransactionID());
                target.getOverview().setValidationServiceName(source.getOverview().getValidationServiceName());
                target.getOverview().setValidationServiceVersion(source.getOverview().getValidationServiceVersion());
            }
            target.setContext(fromModel(source.getContext()));
            var sourceItems = source.getItems();
            if (sourceItems != null && !sourceItems.isEmpty()) {
                target.setReports(new TestAssertionGroupReportsType());
                target.getReports().getInfoOrWarningOrError().addAll(sourceItems.stream().map(sourceItem -> {
                    BAR targetItem = new BAR();
                    targetItem.setDescription(sourceItem.getDescription());
                    targetItem.setLocation(sourceItem.getLocation());
                    targetItem.setTest(sourceItem.getTest());
                    targetItem.setAssertionID(sourceItem.getAssertionID());
                    targetItem.setType(sourceItem.getType());
                    targetItem.setValue(sourceItem.getValue());
                    return switch (sourceItem.getLevel()) {
                        case SeverityLevel.INFO -> OBJECT_FACTORY_TR.createTestAssertionGroupReportsTypeInfo(targetItem);
                        case SeverityLevel.WARNING -> OBJECT_FACTORY_TR.createTestAssertionGroupReportsTypeWarning(targetItem);
                        case null, default -> OBJECT_FACTORY_TR.createTestAssertionGroupReportsTypeError(targetItem);
                    };
                }).toList());
            }
        }
        return target;
    }

    // -------------------------------------------------------------------------
    // com.gitb.ps.*
    // -------------------------------------------------------------------------

    /**
     * Converts a model {@link com.gitb.model.ps.ProcessingModule} to a Jakarta
     * {@link ProcessingModule}.
     *
     * @param source the model processing module
     * @return the converted processing module, or {@code null} if source is {@code null}
     */
    public static ProcessingModule fromModel(com.gitb.model.ps.ProcessingModule source) {
        ProcessingModule finalTarget = null;
        if (source != null) {
            var target = new ProcessingModule();
            target.setServiceLocation(source.getServiceLocation());
            target.setUri(source.getUri());
            target.setIsRemote(true);
            target.setId(source.getId());
            var sourceConfigs = source.getConfigs();
            if (!sourceConfigs.isEmpty()) {
                target.setConfigs(new ConfigurationParameters());
                target.getConfigs().getParam().addAll(sourceConfigs.stream().map(ModelUtils::fromModel).toList());
            }
            target.setMetadata(fromModel(source.getMetadata()));
            source.getOperation().forEach(op -> target.getOperation().add(fromModel(op)));
            finalTarget = target;
        }
        return finalTarget;
    }

    /**
     * Converts a Jakarta {@link ProcessingModule} to a model {@link com.gitb.model.ps.ProcessingModule}.
     *
     * @param source the Jakarta processing module
     * @return the converted processing module, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.ps.ProcessingModule toModel(ProcessingModule source) {
        com.gitb.model.ps.ProcessingModule target = null;
        if (source != null) {
            target = new com.gitb.model.ps.ProcessingModule();
            target.setServiceLocation(source.getServiceLocation());
            target.setUri(source.getUri());
            target.setId(source.getId());
            var sourceConfigs = source.getConfigs();
            if (sourceConfigs != null && !sourceConfigs.getParam().isEmpty()) {
                target.getConfigs().addAll(sourceConfigs.getParam().stream().map(ModelUtils::toModel).toList());
            }
            target.setMetadata(toModel(source.getMetadata()));
            var finalTarget = target;
            source.getOperation().forEach(op -> finalTarget.getOperation().add(toModel(op)));
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ps.ProcessingOperation} to a Jakarta
     * {@link ProcessingOperation}.
     *
     * @param source the model processing operation
     * @return the converted processing operation, or {@code null} if source is {@code null}
     */
    public static ProcessingOperation fromModel(com.gitb.model.ps.ProcessingOperation source) {
        ProcessingOperation target = null;
        if (source != null) {
            target = new ProcessingOperation();
            target.setName(source.getName());
            target.setInputs(toTypedParameters(source.getInputs()));
            target.setOutputs(toTypedParameters(source.getOutputs()));
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link ProcessingOperation} to a model {@link com.gitb.model.ps.ProcessingOperation}.
     *
     * @param source the Jakarta processing operation
     * @return the converted processing operation, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.ps.ProcessingOperation toModel(ProcessingOperation source) {
        com.gitb.model.ps.ProcessingOperation target = null;
        if (source != null) {
            target = new com.gitb.model.ps.ProcessingOperation();
            target.setName(source.getName());
            target.getInputs().addAll(toModelTypedParametersList(source.getInputs()));
            target.getOutputs().addAll(toModelTypedParametersList(source.getOutputs()));
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ps.GetModuleDefinitionResponse} to a Jakarta
     * {@link com.gitb.ps.GetModuleDefinitionResponse}.
     *
     * @param source the model response
     * @return the converted response, or {@code null} if source is {@code null}
     */
    public static com.gitb.ps.GetModuleDefinitionResponse fromModel(com.gitb.model.ps.GetModuleDefinitionResponse source) {
        com.gitb.ps.GetModuleDefinitionResponse target = null;
        if (source != null) {
            target = new com.gitb.ps.GetModuleDefinitionResponse();
            target.setModule(fromModel(source.getModule()));
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link com.gitb.ps.GetModuleDefinitionResponse} to a model
     * {@link com.gitb.model.ps.GetModuleDefinitionResponse}.
     *
     * @param source the Jakarta response
     * @return the converted response, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.ps.GetModuleDefinitionResponse toModel(com.gitb.ps.GetModuleDefinitionResponse source) {
        com.gitb.model.ps.GetModuleDefinitionResponse target = null;
        if (source != null) {
            target = new com.gitb.model.ps.GetModuleDefinitionResponse();
            target.setModule(toModel(source.getModule()));
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ps.ProcessRequest} to a Jakarta
     * {@link ProcessRequest}.
     *
     * @param source the model request
     * @return the converted request, or {@code null} if source is {@code null}
     */
    public static ProcessRequest fromModel(com.gitb.model.ps.ProcessRequest source) {
        ProcessRequest target = null;
        if (source != null) {
            target = new ProcessRequest();
            target.setSessionId(source.getSessionId());
            target.setOperation(source.getOperation());
            target.getInput().addAll(source.getInput().stream().map(ModelUtils::fromModel).toList());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ps.ProcessResponse} to a Jakarta
     * {@link ProcessResponse}.
     *
     * @param source the model response
     * @return the converted response, or {@code null} if source is {@code null}
     */
    public static ProcessResponse fromModel(com.gitb.model.ps.ProcessResponse source) {
        ProcessResponse target = null;
        if (source != null) {
            target = new ProcessResponse();
            target.setReport(fromModel(source.getReport()));
            target.getOutput().addAll(source.getOutput().stream().map(ModelUtils::fromModel).toList());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ps.BeginTransactionRequest} to a Jakarta
     * {@link com.gitb.ps.BeginTransactionRequest}.
     *
     * @param source the model request
     * @return the converted request, or {@code null} if source is {@code null}
     */
    public static com.gitb.ps.BeginTransactionRequest fromModel(com.gitb.model.ps.BeginTransactionRequest source) {
        com.gitb.ps.BeginTransactionRequest target = null;
        if (source != null) {
            target = new com.gitb.ps.BeginTransactionRequest();
            target.getConfig().addAll(source.getConfig().stream().map(ModelUtils::fromModel).toList());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ps.BeginTransactionResponse} to a Jakarta
     * {@link com.gitb.ps.BeginTransactionResponse}.
     *
     * @param source the model response
     * @return the converted response, or {@code null} if source is {@code null}
     */
    public static com.gitb.ps.BeginTransactionResponse fromModel(com.gitb.model.ps.BeginTransactionResponse source) {
        com.gitb.ps.BeginTransactionResponse target = null;
        if (source != null) {
            target = new com.gitb.ps.BeginTransactionResponse();
            target.setSessionId(source.getSessionId());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.core.LogRequest} to a Jakarta
     * {@link com.gitb.ps.LogRequest}.
     *
     * @param source the model log request
     * @return the converted log request, or {@code null} if source is {@code null}
     */
    public static com.gitb.ps.LogRequest fromModelForProcessing(com.gitb.model.core.LogRequest source) {
        com.gitb.ps.LogRequest target = null;
        if (source != null) {
            target = new com.gitb.ps.LogRequest();
            target.setSessionId(source.getSessionId());
            target.setMessage(source.getMessage());
            target.setLevel(fromModel(source.getLevel()));
        }
        return target;
    }

    // -------------------------------------------------------------------------
    // com.gitb.ms.*
    // -------------------------------------------------------------------------

    /**
     * Converts a model {@link com.gitb.model.ms.MessagingModule} to a Jakarta
     * {@link MessagingModule}.
     *
     * <p>Note: {@code isProxy} present in {@link MessagingModule} has no counterpart in the
     * model and is therefore not set.
     * <p>Note: {@code isRemote} (inherited from {@link TestModule}) has no counterpart in the
     * model and is hardcoded to {@code true}.
     *
     * @param source the model messaging module
     * @return the converted messaging module, or {@code null} if source is {@code null}
     */
    public static MessagingModule fromModel(com.gitb.model.ms.MessagingModule source) {
        MessagingModule target = null;
        if (source != null) {
            target = new MessagingModule();
            target.setMetadata(fromModel(source.getMetadata()));
            target.setId(source.getId());
            target.setUri(source.getUri());
            target.setServiceLocation(source.getServiceLocation());
            target.setIsRemote(true);
            target.setIsProxy(false);
            target.setInputs(toTypedParameters(source.getInputs()));
            target.setOutputs(toTypedParameters(source.getOutputs()));
            var actorConfigs = source.getActorConfigs();
            if (!actorConfigs.isEmpty()) {
                target.setActorConfigs(new ConfigurationParameters());
                target.getActorConfigs().getParam().addAll(actorConfigs.stream().map(ModelUtils::fromModel).toList());
            }
            var transactionConfigs = source.getTransactionConfigs();
            if (!transactionConfigs.isEmpty()) {
                target.setTransactionConfigs(new ConfigurationParameters());
                target.getTransactionConfigs().getParam().addAll(transactionConfigs.stream().map(ModelUtils::fromModel).toList());
            }
            var listenConfigs = source.getListenConfigs();
            if (!listenConfigs.isEmpty()) {
                target.setListenConfigs(new ConfigurationParameters());
                target.getListenConfigs().getParam().addAll(listenConfigs.stream().map(ModelUtils::fromModel).toList());
            }
            var receiveConfigs = source.getReceiveConfigs();
            if (!receiveConfigs.isEmpty()) {
                target.setReceiveConfigs(new ConfigurationParameters());
                target.getReceiveConfigs().getParam().addAll(receiveConfigs.stream().map(ModelUtils::fromModel).toList());
            }
            var sendConfigs = source.getSendConfigs();
            if (!sendConfigs.isEmpty()) {
                target.setSendConfigs(new ConfigurationParameters());
                target.getSendConfigs().getParam().addAll(sendConfigs.stream().map(ModelUtils::fromModel).toList());
            }
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link MessagingModule} to a model {@link com.gitb.model.ms.MessagingModule}.
     *
     * <p>Note: {@code isProxy} and {@code isRemote} properties are not converted back to the model
     * as they have no counterpart in the model definition.
     *
     * @param source the Jakarta messaging module
     * @return the converted messaging module, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.ms.MessagingModule toModel(MessagingModule source) {
        com.gitb.model.ms.MessagingModule target = null;
        if (source != null) {
            target = new com.gitb.model.ms.MessagingModule();
            target.setMetadata(toModel(source.getMetadata()));
            target.setId(source.getId());
            target.setUri(source.getUri());
            target.setServiceLocation(source.getServiceLocation());
            target.getInputs().addAll(toModelTypedParametersList(source.getInputs()));
            target.getOutputs().addAll(toModelTypedParametersList(source.getOutputs()));
            var actorConfigs = source.getActorConfigs();
            if (actorConfigs != null && !actorConfigs.getParam().isEmpty()) {
                target.getActorConfigs().addAll(actorConfigs.getParam().stream().map(ModelUtils::toModel).toList());
            }
            var transactionConfigs = source.getTransactionConfigs();
            if (transactionConfigs != null && !transactionConfigs.getParam().isEmpty()) {
                target.getTransactionConfigs().addAll(transactionConfigs.getParam().stream().map(ModelUtils::toModel).toList());
            }
            var listenConfigs = source.getListenConfigs();
            if (listenConfigs != null && !listenConfigs.getParam().isEmpty()) {
                target.getListenConfigs().addAll(listenConfigs.getParam().stream().map(ModelUtils::toModel).toList());
            }
            var receiveConfigs = source.getReceiveConfigs();
            if (receiveConfigs != null && !receiveConfigs.getParam().isEmpty()) {
                target.getReceiveConfigs().addAll(receiveConfigs.getParam().stream().map(ModelUtils::toModel).toList());
            }
            var sendConfigs = source.getSendConfigs();
            if (sendConfigs != null && !sendConfigs.getParam().isEmpty()) {
                target.getSendConfigs().addAll(sendConfigs.getParam().stream().map(ModelUtils::toModel).toList());
            }
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ms.GetModuleDefinitionResponse} to a Jakarta
     * {@link com.gitb.ms.GetModuleDefinitionResponse}.
     *
     * @param source the model response
     * @return the converted response, or {@code null} if source is {@code null}
     */
    public static com.gitb.ms.GetModuleDefinitionResponse fromModel(com.gitb.model.ms.GetModuleDefinitionResponse source) {
        com.gitb.ms.GetModuleDefinitionResponse target = null;
        if (source != null) {
            target = new com.gitb.ms.GetModuleDefinitionResponse();
            target.setModule(fromModel(source.getModule()));
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link com.gitb.ms.GetModuleDefinitionResponse} to a model
     * {@link com.gitb.model.ms.GetModuleDefinitionResponse}.
     *
     * @param source the Jakarta response
     * @return the converted response, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.ms.GetModuleDefinitionResponse toModel(com.gitb.ms.GetModuleDefinitionResponse source) {
        com.gitb.model.ms.GetModuleDefinitionResponse target = null;
        if (source != null) {
            target = new com.gitb.model.ms.GetModuleDefinitionResponse();
            target.setModule(toModel(source.getModule()));
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ms.InitiateRequest} to a Jakarta
     * {@link InitiateRequest}.
     *
     * @param source the model request
     * @return the converted request, or {@code null} if source is {@code null}
     */
    public static InitiateRequest fromModel(com.gitb.model.ms.InitiateRequest source) {
        InitiateRequest target = null;
        if (source != null) {
            target = new InitiateRequest();
            target.getActorConfiguration().addAll(source.getActorConfiguration().stream().map(ModelUtils::fromModel).toList());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ms.InitiateResponse} to a Jakarta
     * {@link InitiateResponse}.
     *
     * @param source the model response
     * @return the converted response, or {@code null} if source is {@code null}
     */
    public static InitiateResponse fromModel(com.gitb.model.ms.InitiateResponse source) {
        InitiateResponse target = null;
        if (source != null) {
            target = new InitiateResponse();
            target.setSessionId(source.getSessionId());
            target.getActorConfiguration().addAll(source.getActorConfiguration().stream().map(ModelUtils::fromModel).toList());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ms.ReceiveRequest} to a Jakarta
     * {@link ReceiveRequest}.
     *
     * @param source the model request
     * @return the converted request, or {@code null} if source is {@code null}
     */
    public static ReceiveRequest fromModel(com.gitb.model.ms.ReceiveRequest source) {
        ReceiveRequest target = null;
        if (source != null) {
            target = new ReceiveRequest();
            target.setSessionId(source.getSessionId());
            target.setCallId(source.getCallId());
            target.setFrom(source.getFrom());
            target.getInput().addAll(source.getInput().stream().map(ModelUtils::fromModel).toList());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ms.SendRequest} to a Jakarta {@link SendRequest}.
     *
     * @param source the model request
     * @return the converted request, or {@code null} if source is {@code null}
     */
    public static SendRequest fromModel(com.gitb.model.ms.SendRequest source) {
        SendRequest target = null;
        if (source != null) {
            target = new SendRequest();
            target.setSessionId(source.getSessionId());
            target.setTo(source.getTo());
            target.getInput().addAll(source.getInput().stream().map(ModelUtils::fromModel).toList());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ms.SendResponse} to a Jakarta {@link SendResponse}.
     *
     * @param source the model response
     * @return the converted response, or {@code null} if source is {@code null}
     */
    public static SendResponse fromModel(com.gitb.model.ms.SendResponse source) {
        SendResponse target = null;
        if (source != null) {
            target = new SendResponse();
            target.setReport(fromModel(source.getReport()));
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ms.NotifyForMessageRequest} to a Jakarta
     * {@link NotifyForMessageRequest}.
     *
     * @param source the model request
     * @return the converted request, or {@code null} if source is {@code null}
     */
    public static NotifyForMessageRequest fromModel(com.gitb.model.ms.NotifyForMessageRequest source) {
        NotifyForMessageRequest target = null;
        if (source != null) {
            target = new NotifyForMessageRequest();
            target.setSessionId(source.getSessionId());
            target.setCallId(source.getCallId());
            target.setFrom(source.getFrom());
            target.setTo(source.getTo());
            target.setReport(fromModel(source.getReport()));
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ms.FinalizeRequest} to a Jakarta
     * {@link FinalizeRequest}.
     *
     * @param source the model request
     * @return the converted request, or {@code null} if source is {@code null}
     */
    public static FinalizeRequest fromModel(com.gitb.model.ms.FinalizeRequest source) {
        FinalizeRequest target = null;
        if (source != null) {
            target = new FinalizeRequest();
            target.setSessionId(source.getSessionId());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.ms.BeginTransactionRequest} to a Jakarta
     * {@link com.gitb.ms.BeginTransactionRequest}.
     *
     * @param source the model request
     * @return the converted request, or {@code null} if source is {@code null}
     */
    public static com.gitb.ms.BeginTransactionRequest fromModel(com.gitb.model.ms.BeginTransactionRequest source) {
        com.gitb.ms.BeginTransactionRequest target = null;
        if (source != null) {
            target = new com.gitb.ms.BeginTransactionRequest();
            target.setSessionId(source.getSessionId());
            target.setFrom(source.getFrom());
            target.setTo(source.getTo());
            target.getConfig().addAll(source.getConfig().stream().map(ModelUtils::fromModel).toList());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.core.LogRequest} to a Jakarta
     * {@link com.gitb.ms.LogRequest}.
     *
     * @param source the model log request
     * @return the converted log request, or {@code null} if source is {@code null}
     */
    public static com.gitb.ms.LogRequest fromModelForMessaging(com.gitb.model.core.LogRequest source) {
        com.gitb.ms.LogRequest target = null;
        if (source != null) {
            target = new com.gitb.ms.LogRequest();
            target.setSessionId(source.getSessionId());
            target.setMessage(source.getMessage());
            target.setLevel(fromModel(source.getLevel()));
        }
        return target;
    }

    // -------------------------------------------------------------------------
    // com.gitb.vs.*
    // -------------------------------------------------------------------------

    /**
     * Converts a model {@link com.gitb.model.vs.ValidationModule} to a Jakarta
     * {@link ValidationModule}.
     *
     * @param source the model validation module
     * @return the converted validation module, or {@code null} if source is {@code null}
     */
    public static ValidationModule fromModel(com.gitb.model.vs.ValidationModule source) {
        ValidationModule target = null;
        if (source != null) {
            target = new ValidationModule();
            target.setMetadata(fromModel(source.getMetadata()));
            target.setId(source.getId());
            target.setUri(source.getUri());
            target.setServiceLocation(source.getServiceLocation());
            target.setIsRemote(true);
            target.setInputs(toTypedParameters(source.getInputs()));
            target.setOutputs(toTypedParameters(source.getOutputs()));
            target.setOperation(source.getOperation());
            var configs = source.getConfigs();
            if (!configs.isEmpty()) {
                target.setConfigs(new ConfigurationParameters());
                target.getConfigs().getParam().addAll(configs.stream().map(ModelUtils::fromModel).toList());
            }
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link ValidationModule} to a model {@link com.gitb.model.vs.ValidationModule}.
     *
     * @param source the Jakarta validation module
     * @return the converted validation module, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.vs.ValidationModule toModel(ValidationModule source) {
        com.gitb.model.vs.ValidationModule target = null;
        if (source != null) {
            target = new com.gitb.model.vs.ValidationModule();
            target.setMetadata(toModel(source.getMetadata()));
            target.setId(source.getId());
            target.setUri(source.getUri());
            target.setServiceLocation(source.getServiceLocation());
            target.getInputs().addAll(toModelTypedParametersList(source.getInputs()));
            target.getOutputs().addAll(toModelTypedParametersList(source.getOutputs()));
            if (source.getOperation() != null) {
                target.setOperation(source.getOperation());
            }
            var configs = source.getConfigs();
            if (configs != null && !configs.getParam().isEmpty()) {
                target.getConfigs().addAll(configs.getParam().stream().map(ModelUtils::toModel).toList());
            }
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.vs.GetModuleDefinitionResponse} to a Jakarta
     * {@link com.gitb.vs.GetModuleDefinitionResponse}.
     *
     * @param source the model response
     * @return the converted response, or {@code null} if source is {@code null}
     */
    public static com.gitb.vs.GetModuleDefinitionResponse fromModel(com.gitb.model.vs.GetModuleDefinitionResponse source) {
        com.gitb.vs.GetModuleDefinitionResponse target = null;
        if (source != null) {
            target = new com.gitb.vs.GetModuleDefinitionResponse();
            target.setModule(fromModel(source.getModule()));
        }
        return target;
    }

    /**
     * Converts a Jakarta {@link com.gitb.vs.GetModuleDefinitionResponse} to a model
     * {@link com.gitb.model.vs.GetModuleDefinitionResponse}.
     *
     * @param source the Jakarta response
     * @return the converted response, or {@code null} if source is {@code null}
     */
    public static com.gitb.model.vs.GetModuleDefinitionResponse toModel(com.gitb.vs.GetModuleDefinitionResponse source) {
        com.gitb.model.vs.GetModuleDefinitionResponse target = null;
        if (source != null) {
            target = new com.gitb.model.vs.GetModuleDefinitionResponse();
            target.setModule(toModel(source.getModule()));
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.vs.ValidateRequest} to a Jakarta
     * {@link ValidateRequest}.
     *
     * @param source the model request
     * @return the converted request, or {@code null} if source is {@code null}
     */
    public static ValidateRequest fromModel(com.gitb.model.vs.ValidateRequest source) {
        ValidateRequest target = null;
        if (source != null) {
            target = new ValidateRequest();
            target.setSessionId(source.getSessionId());
            target.getConfig().addAll(source.getConfig().stream().map(ModelUtils::fromModel).toList());
            target.getInput().addAll(source.getInput().stream().map(ModelUtils::fromModel).toList());
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.vs.ValidationResponse} to a Jakarta
     * {@link ValidationResponse}.
     *
     * @param source the model response
     * @return the converted response, or {@code null} if source is {@code null}
     */
    public static ValidationResponse fromModel(com.gitb.model.vs.ValidationResponse source) {
        ValidationResponse target = null;
        if (source != null) {
            target = new ValidationResponse();
            target.setReport(fromModel(source.getReport()));
        }
        return target;
    }

    /**
     * Converts a model {@link com.gitb.model.core.LogRequest} to a Jakarta
     * {@link com.gitb.vs.LogRequest}.
     *
     * @param source the model log request
     * @return the converted log request, or {@code null} if source is {@code null}
     */
    public static com.gitb.vs.LogRequest fromModelForValidation(com.gitb.model.core.LogRequest source) {
        com.gitb.vs.LogRequest target = null;
        if (source != null) {
            target = new com.gitb.vs.LogRequest();
            target.setSessionId(source.getSessionId());
            target.setMessage(source.getMessage());
            target.setLevel(fromModel(source.getLevel()));
        }
        return target;
    }

}