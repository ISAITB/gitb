package com.gitb.engine.remote.validation;

import com.gitb.vs.ValidationService_Service;

import jakarta.jws.HandlerChain;
import jakarta.xml.ws.WebServiceClient;
import java.net.URL;

/**
 * Created by simatosc on 28/11/2016.
 */
@WebServiceClient(name = "ValidationService", targetNamespace = "http://www.gitb.com/vs/v1/", wsdlLocation = "http://www.gitb.com/services")
@HandlerChain(file="handler-chain-validation.xml")
public class ValidationServiceClient extends ValidationService_Service {

    public ValidationServiceClient(URL serviceURL) {
        super(serviceURL);
    }
}
