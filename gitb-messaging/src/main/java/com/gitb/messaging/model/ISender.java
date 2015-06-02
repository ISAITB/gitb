package com.gitb.messaging.model;

import com.gitb.core.Configuration;
import com.gitb.messaging.Message;

import java.util.List;

/**
 * Created by serbay.
 */
public interface ISender {
	public Message send(List<Configuration> configurations, Message message) throws Exception;
	public void onEnd() throws Exception;
}
