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

package com.gitb.tbs.config;

import com.gitb.engine.TestEngineConfiguration;
import com.gitb.tbs.impl.MessagingClientImpl;
import com.gitb.tbs.impl.ProcessingClientImpl;
import com.gitb.tbs.impl.TestbedServiceImpl;
import com.gitb.tbs.impl.ValidationClientImpl;
import jakarta.servlet.MultipartConfigElement;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

import javax.xml.namespace.QName;

import static com.gitb.engine.TestEngineConfiguration.HANDLER_API_SEGMENT;

@Configuration
public class BeanConfig {

    @Bean
    public ServletRegistrationBean<CXFServlet> servletRegistrationBean() {
        var srb = new ServletRegistrationBean<>(new CXFServlet(), "/*");
        srb.addInitParameter("hide-service-list-page", "true");
        return srb;
    }

    @Bean
    public DispatcherServlet dispatcherServlet() {
        DispatcherServlet dispatcherServlet = new DispatcherServlet();
        dispatcherServlet.setThreadContextInheritable(true);
        return dispatcherServlet;
    }

    @Bean
    public DispatcherServletRegistrationBean dispatcherServletRegistration(DispatcherServlet dispatcherServlet, MultipartConfigElement multipartConfig) {
        var registration = new DispatcherServletRegistrationBean(dispatcherServlet, "/"+ HANDLER_API_SEGMENT +"/*");
        registration.setLoadOnStartup(0);
        registration.setName(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME);
        registration.setMultipartConfig(multipartConfig);
        return registration;
    }

    @Bean
    public EndpointImpl testbedService(Bus cxfBus, TestbedServiceImpl testBedServiceImpl) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, testBedServiceImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/tbs/v1/", "TestbedService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/tbs/v1/", "TestbedServicePort"));
        endpoint.publish("/TestbedService");
        return endpoint;
    }

    @Bean
    public EndpointImpl messagingClientService(Bus cxfBus, MessagingClientImpl messagingClientImpl) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, messagingClientImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/ms/v1/", "MessagingClientService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/ms/v1/", "MessagingClientPort"));
        endpoint.setPublishedEndpointUrl(TestEngineConfiguration.MESSAGING_CALLBACK_URL);
        endpoint.publish("/MessagingClient");
        return endpoint;
    }

    @Bean
    public EndpointImpl validationClientService(Bus cxfBus, ValidationClientImpl validationClientImpl) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, validationClientImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/vs/v1/", "ValidationClientService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/vs/v1/", "ValidationClientPort"));
        endpoint.setPublishedEndpointUrl(TestEngineConfiguration.VALIDATION_CALLBACK_URL);
        endpoint.publish("/ValidationClient");
        return endpoint;
    }

    @Bean
    public EndpointImpl processingClientService(Bus cxfBus, ProcessingClientImpl processingClientImpl) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, processingClientImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/ps/v1/", "ProcessingClientService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/ps/v1/", "ProcessingClientPort"));
        endpoint.setPublishedEndpointUrl(TestEngineConfiguration.PROCESSING_CALLBACK_URL);
        endpoint.publish("/ProcessingClient");
        return endpoint;
    }

}
