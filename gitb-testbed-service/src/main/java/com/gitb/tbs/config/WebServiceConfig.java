package com.gitb.tbs.config;

import com.gitb.tbs.impl.MessagingClientImpl;
import com.gitb.tbs.impl.TestbedServiceImpl;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;

@Configuration
public class WebServiceConfig {

    @Autowired
    Bus cxfBus;

    @Autowired
    TestbedServiceImpl testBedServiceImpl;

    @Autowired
    MessagingClientImpl messagingClientImpl;

    @Bean
    public ServletRegistrationBean servletRegistrationBean(ApplicationContext context) {
        ServletRegistrationBean<CXFServlet> srb = new ServletRegistrationBean<>(new CXFServlet(), "/*");
        return srb;
    }

    @Bean
    public Endpoint testbedService() {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, testBedServiceImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/tbs/v1/", "TestbedService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/tbs/v1/", "TestbedServicePort"));
        endpoint.publish("/TestbedService");
        return endpoint;
    }

    @Bean
    public Endpoint messagingClientService() {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, messagingClientImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/ms/v1/", "MessagingClientService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/ms/v1/", "MessagingClientPort"));
        endpoint.publish("/MessagingClient");
        return endpoint;
    }

}
