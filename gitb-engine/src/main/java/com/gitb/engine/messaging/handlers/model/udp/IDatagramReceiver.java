package com.gitb.engine.messaging.handlers.model.udp;

import com.gitb.engine.messaging.handlers.model.IReceiver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by serbay.
 */
public interface IDatagramReceiver extends IReceiver {
	public void onReceive(DatagramSocket socket, DatagramPacket datagramPacket);
}
