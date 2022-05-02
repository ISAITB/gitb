package com.gitb.engine.messaging.handlers.model.tcp;

import com.gitb.engine.messaging.handlers.model.IReceiver;

import java.net.Socket;

/**
 * Created by serbay on 9/25/14.
 */
public interface ITransactionReceiver extends IReceiver {
	public void onReceive(Socket socket);
}
