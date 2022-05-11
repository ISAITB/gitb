package com.gitb.engine;

import com.gitb.engine.actors.ActorSystem;

/**
 * Created by serbay on 9/5/14.
 */
public class TestEngine {
	private static TestEngine instance;
	private final ActorSystem actorSystem;
    private ITestbedServiceCallbackHandler tbsCallbackHandle;

	private TestEngine() {
		TestEngineConfiguration.load();
        //Initialize the Akka Actor System for engine
		actorSystem = new ActorSystem();
	}

	public void initialize(ITestbedServiceCallbackHandler tbsCallbackHandle) {
		this.tbsCallbackHandle = tbsCallbackHandle;
        //Initialize the Module Manager
        ModuleManager.getInstance();
        //Initialize the SessionManager
		SessionManager.getInstance();
	}

	public void destroy() {

		actorSystem.shutdown();

		SessionManager
			.getInstance()
			.destroy();
	}

	public ActorSystem getEngineActorSystem() {
		return actorSystem;
	}

	public ITestbedServiceCallbackHandler getTbsCallbackHandle() {
		return tbsCallbackHandle;
	}

	public synchronized static TestEngine getInstance() {
		if(instance == null) {
			instance = new TestEngine();
		}
		return instance;
	}
}
