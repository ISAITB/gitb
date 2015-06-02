package com.gitb.engine;

import com.gitb.engine.actors.ActorSystem;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Created by serbay on 9/8/14.
 */
public class ActorSystemTest {

	protected static ActorSystem system;

	@BeforeClass
	public static void before() {
		system = TestEngine
					.getInstance()
					.getEngineActorSystem();
	}

	@Test
	public void testSystemIsNotNull() {
		assertNotNull(system);
	}
}
