package com.gitb.tbs.config;

import com.gitb.engine.TestEngineConfiguration;
import com.gitb.tbs.impl.MessagingClientImpl;
import com.gitb.tbs.impl.ProcessingClientImpl;
import com.gitb.tbs.impl.TestbedServiceImpl;
import com.gitb.tbs.impl.ValidationClientImpl;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.namespace.QName;

@Configuration
public class WebServiceConfig {

    @Autowired
    Bus cxfBus;
    @Autowired
    TestbedServiceImpl testBedServiceImpl;
    @Autowired
    MessagingClientImpl messagingClientImpl;
    @Autowired
    ValidationClientImpl validationClientImpl;
    @Autowired
    ProcessingClientImpl processingClientImpl;

    @Bean
    public ServletRegistrationBean<CXFServlet> servletRegistrationBean(ApplicationContext context) {
        return new ServletRegistrationBean<>(new CXFServlet(), "/*");
    }

    @Bean
    public EndpointImpl testbedService() {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, testBedServiceImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/tbs/v1/", "TestbedService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/tbs/v1/", "TestbedServicePort"));
        endpoint.publish("/TestbedService");
        return endpoint;
    }

    @Bean
    public EndpointImpl messagingClientService() {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, messagingClientImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/ms/v1/", "MessagingClientService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/ms/v1/", "MessagingClientPort"));
        endpoint.setPublishedEndpointUrl(TestEngineConfiguration.MESSAGING_CALLBACK_URL);
        endpoint.publish("/MessagingClient");
        return endpoint;
    }

    @Bean
    public EndpointImpl validationClientService() {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, validationClientImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/vs/v1/", "ValidationClientService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/vs/v1/", "ValidationClientPort"));
        endpoint.setPublishedEndpointUrl(TestEngineConfiguration.VALIDATION_CALLBACK_URL);
        endpoint.publish("/ValidationClient");
        return endpoint;
    }

    @Bean
    public EndpointImpl processingClientService() {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, processingClientImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/ps/v1/", "ProcessingClientService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/ps/v1/", "ProcessingClientPort"));
        endpoint.setPublishedEndpointUrl(TestEngineConfiguration.PROCESSING_CALLBACK_URL);
        endpoint.publish("/ProcessingClient");
        return endpoint;
    }

}
