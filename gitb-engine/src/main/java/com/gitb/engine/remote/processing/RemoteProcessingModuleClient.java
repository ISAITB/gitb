package com.gitb.engine.remote.processing;

import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.engine.remote.RemoteServiceClient;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.Void;
import com.gitb.ps.*;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.utils.DataTypeUtils;

import javax.xml.ws.soap.AddressingFeature;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class RemoteProcessingModuleClient extends RemoteServiceClient implements IProcessingHandler {

    private ProcessingModule serviceModule;

    public RemoteProcessingModuleClient(URL serviceURL, Properties callProperties, String sessionId) {
        super(serviceURL, callProperties, sessionId);
    }

    @Override
    protected String getServiceLocation() {
        if (serviceModule != null) {
            return serviceModule.getServiceLocation();
        }
        return null;
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public ProcessingModule getModuleDefinition() {
        if (serviceModule == null) {
            serviceModule = call(() -> getServiceClient().getModuleDefinition(new Void()).getModule());
        }
        return serviceModule;
    }

    @Override
    public String beginTransaction(String stepId, List<Configuration> config) {
        BeginTransactionRequest transactionRequest = new BeginTransactionRequest();
        transactionRequest.getConfig().addAll(config);
        return call(() -> getServiceClient().beginTransaction(transactionRequest).getSessionId(), stepIdMap(stepId));
    }

    private String sessionIdToUse(String processingSessionId) {
        if (processingSessionId == null) {
            return testSessionId;
        } else {
            return processingSessionId;
        }
    }

    @Override
    public ProcessingReport process(String processingSessionId, String stepId, String operation, ProcessingData data) {
        ProcessRequest processRequest = new ProcessRequest();
        processRequest.setSessionId(sessionIdToUse(processingSessionId));
        processRequest.setOperation(operation);
        processRequest.getInput().addAll(getInput(data));
        ProcessResponse processResponse = call(() -> getServiceClient().process(processRequest), stepIdMap(stepId));
        return new ProcessingReport(processResponse.getReport(), getOutput(processResponse.getOutput()));
    }

    private List<AnyContent> getInput(ProcessingData data) {
        List<AnyContent> result = new ArrayList<>();
        for (Map.Entry<String, DataType> inputEntry : data.getData().entrySet()) {
            AnyContent anyContent = DataTypeUtils.convertDataTypeToAnyContent(inputEntry.getKey(), inputEntry.getValue());
            result.add(anyContent);
        }
        return result;
    }

    private ProcessingData getOutput(List<AnyContent> output) {
        ProcessingData data = new ProcessingData();
        for (AnyContent content : output) {
            data.getData().put(content.getName(), DataTypeFactory.getInstance().create(content));
        }
        return data;
    }

    @Override
    public void endTransaction(String processingSessionId, String stepId) {
        BasicRequest basicRequest = new BasicRequest();
        basicRequest.setSessionId(sessionIdToUse(processingSessionId));
        call(() -> getServiceClient().endTransaction(basicRequest), stepIdMap(stepId));
    }

    private ProcessingService getServiceClient() {
        TestCaseUtils.prepareRemoteServiceLookup(getCallProperties());
        return new ProcessingServiceClient(getServiceURL()).getProcessingServicePort(new AddressingFeature(true));
    }

}
