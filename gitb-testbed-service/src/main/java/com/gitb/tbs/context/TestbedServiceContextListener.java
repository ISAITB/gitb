package com.gitb.tbs.context;

import com.gitb.engine.TestEngine;
import com.gitb.tbs.impl.TestbedServiceCallbackHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
}
