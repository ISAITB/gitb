/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package com.gitb.remote.validation;

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
