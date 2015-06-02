package com.gitb.engine;

import com.gitb.tbs.TestbedClient;

/**
 * Created by tuncay on 9/24/14.
 */
public interface ITestbedServiceCallbackHandler {
    /**
     * Returns the Client object to handle TBS callbacks
     * @return
     */
    public TestbedClient getTestbedClient(String sessionId);

    /**
     * Release the resources related with the TestbedClient
     * @param sessionId
     */
    public void releaseTestbedClient(String sessionId);
}
