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
