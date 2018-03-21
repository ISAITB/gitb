package com.gitb.messaging.model;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;

import java.util.List;

/**
 * Created by serbay.
 */
public interface IReceiver {
	public Message receive(List<Configuration> configurations, Message inputs) throws Exception;
	public void onError(Exception cause);
	public void onEnd() throws Exception;
}
