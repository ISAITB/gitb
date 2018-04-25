package com.gitb.engine.remote.processing;

import com.gitb.core.AnyContent;
import com.gitb.core.Configuration;
import com.gitb.core.ErrorCode;
import com.gitb.core.ValueEmbeddingEnumeration;
import com.gitb.engine.remote.RemoteCallContext;
import com.gitb.engine.utils.TestCaseUtils;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.processing.IProcessingHandler;
import com.gitb.processing.ProcessingData;
import com.gitb.processing.ProcessingReport;
import com.gitb.ps.*;
import com.gitb.ps.Void;
import com.gitb.types.*;
import com.gitb.utils.DataTypeUtils;
import com.gitb.utils.ErrorUtils;
import org.apache.commons.codec.binary.Base64;

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

    public RemoteProcessingModuleClient(URL serviceURL, Properties transactionProperties) {
        this.serviceURL = serviceURL;
        this.transactionProperties = transactionProperties;
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

    @Override
    public ProcessingReport process(String session, String operation, ProcessingData data) {
        ProcessRequest processRequest = new ProcessRequest();
        processRequest.setSessionId(session);
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
            data.getData().put(content.getName(), toDataType(content));
        }
        return data;
    }

    @Override
    public void endTransaction(String session) {
        BasicRequest basicRequest = new BasicRequest();
        basicRequest.setSessionId(session);
        call(() -> getServiceClient().endTransaction(basicRequest));
    }

    private ProcessingService getServiceClient() {
        TestCaseUtils.prepareRemoteServiceLookup(transactionProperties);
        return new ProcessingServiceClient(getServiceURL()).getProcessingServicePort();
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

    private DataType toDataType(AnyContent content) {
        DataType type;
        if (DataType.MAP_DATA_TYPE.equals(content.getType())) {
            type = new MapType();
            for (AnyContent child : content.getItem()) {
                ((MapType) type).addItem(child.getName(), toDataType(child));
            }
        } else if (DataType.STRING_DATA_TYPE.equals(content.getType())) {
            type = new StringType();
            type.setValue(content.getValue());
        } else if (DataType.BINARY_DATA_TYPE.equals(content.getType())) {
            type = new BinaryType();
            if (ValueEmbeddingEnumeration.BASE_64.equals(content.getEmbeddingMethod())) {
                type.setValue(Base64.decodeBase64(content.getValue()));
            } else {
                throw new IllegalStateException("Only base64 embedding supported for binary types");
            }
        } else if (DataType.BOOLEAN_DATA_TYPE.equals(content.getType())) {
            type = new BooleanType();
            type.setValue(Boolean.valueOf(content.getValue()));
        } else if (DataType.NUMBER_DATA_TYPE.equals(content.getType())) {
            type = new NumberType();
            type.setValue(content.getValue());
        } else if (DataType.LIST_DATA_TYPE.equals(content.getType())) {
            type = new ListType();
            for (AnyContent child : content.getItem()) {
                ((ListType) type).append(toDataType(child));
            }
        } else if (DataType.OBJECT_DATA_TYPE.equals(content.getType())) {
            type = new ObjectType();
            if (ValueEmbeddingEnumeration.BASE_64.equals(content.getEmbeddingMethod())) {
                type.deserialize(Base64.decodeBase64(content.getValue()));
            } else if (ValueEmbeddingEnumeration.STRING.equals(content.getEmbeddingMethod())) {
                type.deserialize(content.getValue().getBytes());
            } else {
                throw new IllegalStateException("Only base64 and string embedding supported for object types");
            }
        } else {
            throw new IllegalStateException("Unsupported data type [" + content.getType() + "]");
        }
        return type;
    }

}
