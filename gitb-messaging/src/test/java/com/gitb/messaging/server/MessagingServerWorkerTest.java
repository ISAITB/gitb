package com.gitb.messaging.server;

import com.gitb.messaging.server.tcp.TCPMessagingServerWorker;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by serbay.
 */
public class MessagingServerWorkerTest {

	@Test
	public void testSessionStartAndStop() throws InterruptedException, IOException {
		TCPMessagingServerWorker worker = new TCPMessagingServerWorker(12345);

		worker.start();

		Thread.sleep(1000);

		assertTrue(worker.isActive());

		worker.stop();

		Thread.sleep(1000);

		assertFalse(worker.isActive());
	}
}
