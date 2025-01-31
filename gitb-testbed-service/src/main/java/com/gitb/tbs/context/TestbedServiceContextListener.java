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
