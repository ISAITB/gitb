package com.gitb.engine.processing;

import com.gitb.ModuleManager;
import com.gitb.core.ErrorCode;
import com.gitb.engine.remote.processing.RemoteProcessingModuleClient;
import com.gitb.exceptions.GITBEngineInternalError;
import com.gitb.processing.IProcessingHandler;
import com.gitb.utils.ErrorUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public final class ProcessingContext {

    private IProcessingHandler handler;
    private String session;

    public ProcessingContext(String handler, Properties transactionProperties) {
        this.handler = resolveHandler(handler, transactionProperties);
    }

    public void setSession(String session) {
        this.session = session;
    }

    public String getSession() {
        return session;
    }

    public IProcessingHandler getHandler() {
        return handler;
    }

    private boolean isURL(String handler) {
        try {
            new URI(handler).toURL();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private IProcessingHandler resolveHandler(String handler, Properties transactionProperties) {
        if (isURL(handler)) {
            return getRemoteProcessor(handler, transactionProperties);
        } else {
            return ModuleManager.getInstance().getProcessingHandler(handler);
        }
    }

    private IProcessingHandler getRemoteProcessor(String handler, Properties transactionProperties) {
        try {
            return new RemoteProcessingModuleClient(new URI(handler).toURL(), transactionProperties);
        } catch (MalformedURLException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote processing module found with an malformed URL [" + handler + "]"), e);
        } catch (URISyntaxException e) {
            throw new GITBEngineInternalError(ErrorUtils.errorInfo(ErrorCode.INTERNAL_ERROR, "Remote processing module found with an invalid URI syntax [" + handler + "]"), e);
        }
    }

}
