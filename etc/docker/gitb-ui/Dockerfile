FROM openjdk:11.0.14-jre

ENV PATH /usr/local/gitb-ui/bin:$PATH
ENV TESTBED_SERVICE_URL http://gitb-srv:8080/itbsrv/TestbedService
ENV TESTBED_CLIENT_URL http://gitb-ui:9090/TestbedClient
ENV DB_DEFAULT_URL jdbc:mysql://gitb-mysql/gitb?characterEncoding=UTF-8&useUnicode=true&autoReconnect=true&useSSL=false&verifyServerCertificate=false&allowPublicKeyRetrieval=true
ENV DB_DEFAULT_ROOTURL jdbc:mysql://gitb-mysql/
ENV REDIS_HOST gitb-redis

COPY gitb-ui /usr/local/gitb-ui

EXPOSE 9000
EXPOSE 9090
WORKDIR /usr/local/gitb-ui
CMD ["gitb"]