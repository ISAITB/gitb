package com.gitb.messaging.model.tcp;

import com.gitb.messaging.Message;
import com.gitb.messaging.model.IReceiver;

import java.net.Socket;

/**
 * Created by serbay on 9/25/14.
 */
public interface ITransactionReceiver extends IReceiver {
	public void onReceive(Socket socket);
}
