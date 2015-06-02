package com.gitb.exceptions;

/**
 * Created by tuncay on 9/30/14.
 */
public abstract class GITBEngineRuntimeException extends RuntimeException{

    public GITBEngineRuntimeException(){
        super();
    }

    public GITBEngineRuntimeException(String message) {
        super(message);
    }

    public GITBEngineRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public GITBEngineRuntimeException(Throwable cause) {
        super(cause);
    }
}
