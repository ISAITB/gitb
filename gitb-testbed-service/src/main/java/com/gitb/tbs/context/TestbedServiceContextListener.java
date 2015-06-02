package com.gitb.tbs.context;

import com.gitb.engine.TestEngine;
import com.gitb.tbs.impl.TestbedServiceCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by senan on 9/16/14.
 */
public class TestbedServiceContextListener implements ServletContextListener{
    private static Logger logger = LoggerFactory.getLogger(TestbedServiceContextListener.class);

    /**
     * Called when war is deployed and context is initialized.
     * Do all necessary initializations in this method
     */
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        logger.info("Context created. Initializing TestBed related resources...");
        //All initializations go below:
        TestbedServiceCallbackHandler tbsCallbackHandle = TestbedServiceCallbackHandler.getInstance();
        TestEngine.getInstance().initialize(tbsCallbackHandle);
    }

    /**
     * Called when context destroyed. Do all clean up here
     */
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        logger.info("Context destroyed...");
	    TestEngine.getInstance().destroy();
    }
}
