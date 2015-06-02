package com.gitb.messaging.model.udp;

import com.gitb.messaging.Message;
import com.gitb.messaging.model.IReceiver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Created by serbay.
 */
public interface IDatagramReceiver extends IReceiver {
	public void onReceive(DatagramSocket socket, DatagramPacket datagramPacket);
}
