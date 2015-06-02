package com.gitb.tbs.impl;

import com.gitb.core.Actor;
import com.gitb.core.ActorConfiguration;
import com.gitb.core.ErrorCode;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tbs.*;
import com.gitb.tbs.Error;
import com.gitb.tbs.Void;
import com.gitb.tbs.TestbedService;
import com.gitb.tpl.TestCase;
import com.gitb.tr.SR;
import com.gitb.utils.ErrorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.Addressing;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by senan on 9/4/14.
 */
@Addressing(enabled = true, required = true)
@SOAPBinding(parameterStyle= SOAPBinding.ParameterStyle.BARE)
@WebService(name = "TestbedService", serviceName = "TestbedService", targetNamespace = "http://www.gitb.com/tbs/v1/")
public class TestbedServiceImpl implements TestbedService {

    private static final Logger logger = LoggerFactory.getLogger(TestbedService.class);

    @Resource
    private WebServiceContext wsc;

    @Override
    public GetTestCaseDefinitionResponse getTestCaseDefinition(@WebParam(name = "GetTestCaseDefinitionRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") BasicRequest parameters) throws Error {
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
    public GetActorDefinitionResponse getActorDefinition(@WebParam(name = "GetActorDefinitionRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") GetActorDefinitionRequest parameters) throws Error {
        try {
            String testCaseId = parameters.getTcId();
            String actorId = parameters.getActorId();
            //Call the real TestbedService
            Actor actor = com.gitb.engine.TestbedService.getActorDefinition(testCaseId, actorId);
            //Construct Response
            GetActorDefinitionResponse response = new GetActorDefinitionResponse();
            response.setActor(actor);
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
    public InitiateResponse initiate(@WebParam(name = "InitiateRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") BasicRequest parameters) throws Error {
        try {
            String testCaseId = parameters.getTcId();
            //Call the real TestbedService
            String sessionId = com.gitb.engine.TestbedService.initiate(testCaseId);
            //Save the WSAddressing properties so we can use callbacks
            TestbedServiceCallbackHandler.
                    getInstance().
                    saveWSAddressingProperties(sessionId, wsc);
            //Construct Response
            InitiateResponse response = new InitiateResponse();
            response.setTcInstanceId(sessionId);
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
    public ConfigureResponse configure(@WebParam(name = "ConfigureRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") ConfigureRequest parameters) throws Error {
        try {
            String sessionId = parameters.getTcInstanceId();
            List<ActorConfiguration> actorConfigurations = parameters.getConfigs();
            //Call the real TestbedService
            List<SUTConfiguration> simulatedActorsConfigurations = com.gitb.engine.TestbedService.configure(sessionId, actorConfigurations);
            //Construct Response
            ConfigureResponse response = new ConfigureResponse();
            response.getConfigs().addAll(simulatedActorsConfigurations);
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
    public Void provideInput(@WebParam(name = "ProvideInputRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") ProvideInputRequest parameters) throws Error {
        try {
            String sessionId = parameters.getTcInstanceId();
            String interactionStepId = parameters.getStepId();
            List<UserInput> userInputs= parameters.getInput();
            //Call the real TestbedService
            com.gitb.engine.TestbedService.provideInput(sessionId, interactionStepId, userInputs);
            return new Void();
        } catch (GITBEngineInternalError e) {
            logger.error("An error occurred", e);
            throw new Error(e.getMessage(), e.getErrorInfo());
        } catch (Exception e) {
            logger.error("An error occurred", e);
            throw new Error("An error occurred.", ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR), e);
        }
    }

    @Override
    public Void initiatePreliminary(@WebParam(name = "InitiatePreliminaryRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") BasicCommand parameters) throws Error {
        try {
            String sessionId = parameters.getTcInstanceId();
            //Call the real TestbedService
            com.gitb.engine.TestbedService.initiatePreliminary(sessionId);
            return new Void();
        } catch (GITBEngineInternalError e) {
            logger.error("An error occurred", e);
            throw new Error(e.getMessage(), e.getErrorInfo());
        } catch (Exception e) {
            logger.error("An error occurred", e);
            throw new Error("An error occurred.", ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR), e);
        }
    }


    @Override
    public Void start(@WebParam(name = "StartRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") BasicCommand parameters) {
        String sessionId = parameters.getTcInstanceId();
        //Call the real TestbedService
        com.gitb.engine.TestbedService.start(sessionId);
        //Construct Response
        return new Void();
    }

    @Override
    public Void stop(@WebParam(name = "StopRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") BasicCommand parameters) {
        String sessionId = parameters.getTcInstanceId();
        //Call the real TestbedService
        com.gitb.engine.TestbedService.stop(sessionId);
        //Construct Response
        return new Void();
    }

    @Override
    public Void restart(@WebParam(name = "RestartRequest", targetNamespace = "http://www.gitb.com/tbs/v1/", partName = "parameters") BasicCommand parameters) {
        String sessionId = parameters.getTcInstanceId();
        //Call the real TestbedService
        com.gitb.engine.TestbedService.restart(sessionId);
        //Construct Response
        return new Void();
    }
}
