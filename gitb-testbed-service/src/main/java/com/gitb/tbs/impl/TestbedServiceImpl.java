package com.gitb.tbs.impl;

import com.gitb.core.ActorConfiguration;
import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tbs.Error;
import com.gitb.tbs.Void;
import com.gitb.tbs.*;
import com.gitb.tpl.TestCase;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.Addressing;
import java.util.List;

/**
 * Created by senan on 9/4/14.
 */
//@SOAPBinding(parameterStyle= SOAPBinding.ParameterStyle.BARE)
//@WebService(name = "TestbedService", serviceName = "TestbedService", targetNamespace = "http://www.gitb.com/tbs/v1/")
@Component
@Addressing(enabled = true, required = true)
public class TestbedServiceImpl implements TestbedService {

    private static final Logger logger = LoggerFactory.getLogger(TestbedService.class);

    @Resource
    private WebServiceContext wsc;

    @Override
    public GetTestCaseDefinitionResponse getTestCaseDefinition(BasicRequest parameters) throws Error {
        try {
            String testCaseId = parameters.getTcId();
            //Call the real TestbedService
            TestCase testCase = com.gitb.engine.TestbedService.getTestCaseDefinition(testCaseId);
            //Construct Response
            GetTestCaseDefinitionResponse response = new GetTestCaseDefinitionResponse();
            response.setTestcase(testCase);
            return response;
        } catch (GITBEngineInternalError e) {
            logger.error("An error occurred", e);
            throw new Error(e.getMessage(), e.getErrorInfo());
        } catch (Exception e) {
            logger.error("An error occurred", e);
            throw new Error("An error occurred.", ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR), e);
        }
    }

    @Override
    public GetActorDefinitionResponse getActorDefinition(GetActorDefinitionRequest parameters) throws Error {
        throw new IllegalStateException("This call is deprecated");
    }

    @Override
    public InitiateResponse initiate(BasicRequest parameters) throws Error {
        String sessionId = null;
        try {
            String testCaseId = parameters.getTcId();
            //Call the real TestbedService
            sessionId = com.gitb.engine.TestbedService.initiate(testCaseId);
            //Save the WSAddressing properties so we can use callbacks
            TestbedServiceCallbackHandler.
                    getInstance().
                    saveWSAddressingProperties(sessionId, wsc);
            //Construct Response
            InitiateResponse response = new InitiateResponse();
            response.setTcInstanceId(sessionId);
            return response;
        } catch (GITBEngineInternalError e) {
            if (StringUtils.isBlank(sessionId)) {
                logger.error("An error occurred", e);
            } else {
                logger.error(MarkerFactory.getDetachedMarker(sessionId), "An error occurred", e);
            }
            throw new Error(e.getMessage(), e.getErrorInfo());
        } catch (Exception e) {
            if (StringUtils.isBlank(sessionId)) {
                logger.error("An error occurred", e);
            } else {
                logger.error(MarkerFactory.getDetachedMarker(sessionId), "An error occurred", e);
            }
            throw new Error("An error occurred.", ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR), e);
        }
    }

    @Override
    public ConfigureResponse configure(ConfigureRequest parameters) throws Error {
        String sessionId = null;
        try {
            sessionId = parameters.getTcInstanceId();
            List<ActorConfiguration> actorConfigurations = parameters.getConfigs();
            //Call the real TestbedService
            List<SUTConfiguration> simulatedActorsConfigurations = com.gitb.engine.TestbedService.configure(sessionId, actorConfigurations);
            //Construct Response
            ConfigureResponse response = new ConfigureResponse();
            response.getConfigs().addAll(simulatedActorsConfigurations);
            return response;
        } catch (GITBEngineInternalError e) {
            logger.error(MarkerFactory.getDetachedMarker(sessionId), "An error occurred", e);
            throw new Error(e.getMessage(), e.getErrorInfo());
        } catch (Exception e) {
            logger.error(MarkerFactory.getDetachedMarker(sessionId), "An error occurred", e);
            throw new Error("An error occurred.", ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR), e);
        }
    }

    @Override
    public Void provideInput(ProvideInputRequest parameters) throws Error {
        String sessionId = null;
        try {
            sessionId = parameters.getTcInstanceId();
            String interactionStepId = parameters.getStepId();
            List<UserInput> userInputs= parameters.getInput();
            //Call the real TestbedService
            com.gitb.engine.TestbedService.provideInput(sessionId, interactionStepId, userInputs);
            return new Void();
        } catch (GITBEngineInternalError e) {
            logger.error(MarkerFactory.getDetachedMarker(sessionId), "An error occurred", e);
            throw new Error(e.getMessage(), e.getErrorInfo());
        } catch (Exception e) {
            logger.error(MarkerFactory.getDetachedMarker(sessionId), "An error occurred", e);
            throw new Error("An error occurred.", ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR), e);
        }
    }

    @Override
    public Void initiatePreliminary(BasicCommand parameters) throws Error {
        String sessionId = null;
        try {
            sessionId = parameters.getTcInstanceId();
            //Call the real TestbedService
            com.gitb.engine.TestbedService.initiatePreliminary(sessionId);
            return new Void();
        } catch (GITBEngineInternalError e) {
            logger.error(MarkerFactory.getDetachedMarker(sessionId), "An error occurred", e);
            throw new Error(e.getMessage(), e.getErrorInfo());
        } catch (Exception e) {
            logger.error(MarkerFactory.getDetachedMarker(sessionId), "An error occurred", e);
            throw new Error("An error occurred.", ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR), e);
        }
    }


    @Override
    public Void start(BasicCommand parameters) {
        String sessionId = parameters.getTcInstanceId();
        //Call the real TestbedService
        com.gitb.engine.TestbedService.start(sessionId);
        //Construct Response
        return new Void();
    }

    @Override
    public Void stop(BasicCommand parameters) {
        String sessionId = parameters.getTcInstanceId();
        //Call the real TestbedService
        com.gitb.engine.TestbedService.stop(sessionId);
        //Construct Response
        return new Void();
    }

    @Override
    public Void restart(BasicCommand parameters) {
        String sessionId = parameters.getTcInstanceId();
        //Call the real TestbedService
        com.gitb.engine.TestbedService.restart(sessionId);
        //Construct Response
        return new Void();
    }
}
