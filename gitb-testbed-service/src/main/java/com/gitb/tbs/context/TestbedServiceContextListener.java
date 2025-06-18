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

package com.gitb.tbs.context;

import com.gitb.engine.TestEngine;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.tbs.impl.TestbedServiceCallbackHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by senan on 9/16/14.
 */
@Component
public class TestbedServiceContextListener {

    private static final Logger logger = LoggerFactory.getLogger(TestbedServiceContextListener.class);

    /**
     * Called when war is deployed and context is initialized.
     * Do all necessary initializations in this method
     */
    @PostConstruct
    public void contextInitialized() {
        logger.info("Context created. Initializing TestBed related resources...");
        //All initializations go below:
        TestbedServiceCallbackHandler tbsCallbackHandle = TestbedServiceCallbackHandler.getInstance();
        TestEngine.getInstance().initialize(tbsCallbackHandle);
    }

    /**
     * Called when context destroyed. Do all clean up here
     */
    @PreDestroy
    public void contextDestroyed() {
        logger.info("Context destroyed...");
	    TestEngine.getInstance().destroy();
    }

    @EventListener
    public void onStartup(ApplicationReadyEvent event) {
        String banner;
        try (var in = Thread.currentThread().getContextClassLoader().getResourceAsStream("banner.txt")) {
            if (in != null) {
                banner = IOUtils.toString(in, StandardCharsets.UTF_8);
            } else {
                banner = "";
            }
        } catch (IOException e) {
            banner = "";
        }
        logger.info("Started ITB test engine (itb-srv) - release {}\n{}", TestCaseUtils.TEST_ENGINE_VERSION, banner);
    }
}
