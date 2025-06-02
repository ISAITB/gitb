package com.gitb.tbs.impl;

import com.gitb.core.Actor;
import com.gitb.core.ErrorCode;
import com.gitb.engine.TestCaseManager;
import com.gitb.engine.TestEngineConfiguration;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.tbs.*;
import com.gitb.tbs.Error;
import com.gitb.tbs.Void;
import com.gitb.tpl.TestCase;
import com.gitb.utils.ErrorUtils;
import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.soap.Addressing;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by senan on 9/4/14.
 */
@Component
@Addressing(required = true)
public class TestbedServiceImpl implements TestbedService {

    private static final Logger logger = LoggerFactory.getLogger(TestbedService.class);

    @Resource
    private WebServiceContext wsc;

    @Override
    public GetTestCaseDefinitionResponse getTestCaseDefinition(GetTestCaseDefinitionRequest parameters) throws Error {
        try {
            TestCase testCase;
            if (parameters.getTcInstanceId() != null) {
                testCase = TestCaseManager.getTestCasePresentationBySessionId(parameters.getTcInstanceId(), parameters.getConfigs());
            } else {
                testCase = TestCaseManager.getTestCasePresentationByTestCaseId(parameters.getTcId(), parameters.getConfigs());
            }
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
    public GetActorDefinitionResponse getActorDefinition(GetActorDefinitionRequest parameters) {
        /*
         * The getActorDefinition operation is deprecated for use in test sessions. However, as it is never
         * called for test sessions we are taking advantage of it to use for a healthcheck ping without needing
         * to add a separate operation that would modify the published API.
         *
         * The healthcheck result is returned as a JSON string as the actor's description.
         */
        String result;
        if ("callbacks".equals(parameters.getActorId())) {
            result = TestEngineConfiguration.ROOT_CALLBACK_URL;
        } else {
            result = com.gitb.engine.TestbedService.healthCheck((msg) -> {
                TestbedClient client = TestbedServiceCallbackHandler.createTestBedClient(wsc);
                client.updateStatus(msg);
            });
        }
        // Prepare result
        GetActorDefinitionResponse response = new GetActorDefinitionResponse();
        response.setActor(new Actor());
        response.getActor().setDesc(result);
        return response;
    }

    @Override
    public InitiateResponse initiate(InitiateRequest parameters) throws Error {
        String sessionId = null;
        try {
            //Call the real TestbedService
            sessionId = com.gitb.engine.TestbedService.initiate(parameters.getTcId(), parameters.getTcInstanceId());
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
    public Void configure(ConfigureRequest parameters) throws Error {
        String sessionId = null;
        try {
            sessionId = parameters.getTcInstanceId();
            // Call the real TestbedService
            logger.debug("Received configure request for session [{}]", sessionId);
            com.gitb.engine.TestbedService.configure(sessionId, parameters.getConfigs(), parameters.getInputs());
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
    public Void provideInput(ProvideInputRequest parameters) throws Error {
        String sessionId = null;
        try {
            sessionId = parameters.getTcInstanceId();
            String interactionStepId = parameters.getStepId();
            List<UserInput> userInputs= parameters.getInput();
            //Call the real TestbedService
            com.gitb.engine.TestbedService.provideInput(sessionId, interactionStepId, userInputs, parameters.isAdmin());
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
