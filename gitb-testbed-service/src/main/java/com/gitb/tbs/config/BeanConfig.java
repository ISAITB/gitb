/*
 * Copyright (C) 2026 European Union
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

import com.gitb.tbs.filters.CallbackAuthorizationFilter;
import com.gitb.tbs.filters.TestBedServiceAuthorizationFilter;
import com.gitb.tbs.impl.MessagingClientImpl;
import com.gitb.tbs.impl.ProcessingClientImpl;
import com.gitb.tbs.impl.TestbedServiceImpl;
import com.gitb.tbs.impl.ValidationClientImpl;
import com.gitb.tdl.HandlerApiType;
import jakarta.servlet.MultipartConfigElement;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.DispatcherServlet;

import javax.xml.namespace.QName;

import static com.gitb.CoreConfiguration.*;
import static com.gitb.engine.TestEngineConfiguration.HANDLER_API_SEGMENT;

@Configuration
public class BeanConfig {

    @Bean
    public FilterRegistrationBean<CallbackAuthorizationFilter> restCallbackFilter() {
        var registration = new FilterRegistrationBean<>(new CallbackAuthorizationFilter(HandlerApiType.REST));
        registration.addUrlPatterns("/"+ HANDLER_API_SEGMENT +"/gitb/notifyForMessage/*", "/"+ HANDLER_API_SEGMENT +"/gitb/log/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CallbackAuthorizationFilter> soapCallbackFilter() {
        var registration = new FilterRegistrationBean<>(new CallbackAuthorizationFilter(HandlerApiType.SOAP));
        registration.addUrlPatterns("/MessagingClient/*", "/ValidationClient/*", "/ProcessingClient/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<TestBedServiceAuthorizationFilter> testBedServiceFilter() {
        var registration = new FilterRegistrationBean<>(new TestBedServiceAuthorizationFilter());
        registration.addUrlPatterns("/TestbedService/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * Executor used by {@code HttpMessagingServer}/{@code SoapMessagingServer} to build and send the response
     * to an incoming call once matched to a receive step - possibly after being held for a while awaiting a
     * matching step (see {@code CallbackManager.lookupHandlingData}). Core threads are pre-started deliberately:
     * {@link #dispatcherServlet()} makes the request context ThreadLocal inheritable, so a pool thread lazily
     * created while handling a request would otherwise inherit (and retain a reference to) that request.
     */
    @Bean
    public ThreadPoolTaskExecutor messagingCallbackExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("messaging-callback-");
        executor.initialize();
        executor.getThreadPoolExecutor().prestartAllCoreThreads();
        return executor;
    }

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
    public DispatcherServletRegistrationBean dispatcherServletRegistration(DispatcherServlet dispatcherServlet, ObjectProvider<MultipartConfigElement> multipartConfig) {
        var registration = new DispatcherServletRegistrationBean(dispatcherServlet, "/"+ HANDLER_API_SEGMENT +"/*");
        registration.setLoadOnStartup(0);
        registration.setName(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME);
        multipartConfig.ifAvailable(registration::setMultipartConfig);
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
        endpoint.setPublishedEndpointUrl(MESSAGING_CALLBACK_URL);
        endpoint.publish("/MessagingClient");
        return endpoint;
    }

    @Bean
    public EndpointImpl validationClientService(Bus cxfBus, ValidationClientImpl validationClientImpl) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, validationClientImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/vs/v1/", "ValidationClientService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/vs/v1/", "ValidationClientPort"));
        endpoint.setPublishedEndpointUrl(VALIDATION_CALLBACK_URL);
        endpoint.publish("/ValidationClient");
        return endpoint;
    }

    @Bean
    public EndpointImpl processingClientService(Bus cxfBus, ProcessingClientImpl processingClientImpl) {
        EndpointImpl endpoint = new EndpointImpl(cxfBus, processingClientImpl);
        endpoint.setServiceName(new QName("http://www.gitb.com/ps/v1/", "ProcessingClientService"));
        endpoint.setEndpointName(new QName("http://www.gitb.com/ps/v1/", "ProcessingClientPort"));
        endpoint.setPublishedEndpointUrl(PROCESSING_CALLBACK_URL);
        endpoint.publish("/ProcessingClient");
        return endpoint;
    }

}
