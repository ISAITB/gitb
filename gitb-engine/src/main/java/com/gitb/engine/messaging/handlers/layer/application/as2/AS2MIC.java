package com.gitb.engine.messaging.handlers.layer.application.as2;

/**
 * Created by senan on 07.01.2015.
 */
public class AS2MIC {
    private String messageIntegrityCheck;

    public AS2MIC(String MIC) {
        messageIntegrityCheck = MIC;
    }

    public String getMessageIntegrityCheck() {
        return messageIntegrityCheck;
    }

    public void setMessageIntegrityCheck(String MIC){
        messageIntegrityCheck = MIC;
    }
}
