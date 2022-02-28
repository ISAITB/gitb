package com.gitb.engine.remote.processing;

import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.engine.remote.RemoteCallContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.Void;
import com.gitb.ps.*;
import com.gitb.types.DataType;
import com.gitb.types.DataTypeFactory;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;

import javax.xml.ws.soap.AddressingFeature;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

public class RemoteProcessingModuleClient implements IProcessingHandler {

    private URL serviceURL;
    private ProcessingModule processingModule;
    private final Properties transactionProperties;
    private final String testSessionId;

    public RemoteProcessingModuleClient(URL serviceURL, Properties transactionProperties, String sessionId) {
        this.serviceURL = serviceURL;
        this.transactionProperties = transactionProperties;
        testSessionId = sessionId;
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    private <T> T call(Supplier<T> supplier) {
        try {
            RemoteCallContext.setCallProperties(transactionProperties);
            return supplier.get();
        } finally {
            RemoteCallContext.clearCallProperties();
        }
    }

    @Override
    public ProcessingModule getModuleDefinition() {
        if (processingModule == null) {
            processingModule = call(() -> getServiceClient().getModuleDefinition(new Void()).getModule());
        }
        return processingModule;
    }

    @Override
    public String beginTransaction(List<Configuration> config) {
        BeginTransactionRequest transactionRequest = new BeginTransactionRequest();
        transactionRequest.getConfig().addAll(config);
        return call(() -> getServiceClient().beginTransaction(transactionRequest).getSessionId());
    }

    private String sessionIdToUse(String processingSessionId) {
        if (processingSessionId == null) {
            return testSessionId;
        } else {
            return processingSessionId;
        }
    }

    @Override
    public ProcessingReport process(String processingSessionId, String operation, ProcessingData data) {
        ProcessRequest processRequest = new ProcessRequest();
        processRequest.setSessionId(sessionIdToUse(processingSessionId));
        processRequest.setOperation(operation);
        processRequest.getInput().addAll(getInput(data));
        ProcessResponse processResponse = call(() -> getServiceClient().process(processRequest));
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
    public void endTransaction(String processingSessionId) {
        BasicRequest basicRequest = new BasicRequest();
        basicRequest.setSessionId(sessionIdToUse(processingSessionId));
        call(() -> getServiceClient().endTransaction(basicRequest));
    }

    private ProcessingService getServiceClient() {
        TestCaseUtils.prepareRemoteServiceLookup(transactionProperties);
        return new ProcessingServiceClient(getServiceURL()).getProcessingServicePort(new AddressingFeature(true));
    }

    private URL getServiceURL() {
        if (serviceURL == null) {
            if (processingModule == null) {
                throw new IllegalStateException("Remote processing module found but with no URL or ProcessingModule definition");
            } else {
                try {
                    serviceURL = new URI(processingModule.getServiceLocation()).toURL();
                } catch (MalformedURLException e) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote processing module found named [" + processingModule.getId() + "] with an malformed URL [" + processingModule.getServiceLocation() + "]"), e);
                } catch (URISyntaxException e) {
                    throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote processing module found named [" + processingModule.getId() + "] with an invalid URI syntax [" + processingModule.getServiceLocation() + "]"), e);
                }
            }
        }
        return serviceURL;
    }

}
