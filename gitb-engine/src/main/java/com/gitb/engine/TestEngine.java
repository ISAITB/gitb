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
