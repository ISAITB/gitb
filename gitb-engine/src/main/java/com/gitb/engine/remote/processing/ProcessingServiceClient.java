package com.gitb.engine.remote.processing;

import com.gitb.ps.ProcessingServiceService;

import javax.jws.HandlerChain;
import javax.xml.ws.WebServiceClient;
import java.net.URL;

/**
 * Created by simatosc on 28/11/2016.
 */
@WebServiceClient(name = "ProcessingServiceService", targetNamespace = "http://www.gitb.com/ps/v1/", wsdlLocation = "http://www.gitb.com/services")
@HandlerChain(file="handler-chain-processing.xml")
public class ProcessingServiceClient extends ProcessingServiceService {

    public ProcessingServiceClient(URL serviceURL) {
        super(serviceURL);
    }
}
