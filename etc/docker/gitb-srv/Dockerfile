FROM eclipse-temurin:21-jre-jammy

RUN mkdir /itbsrv
COPY itbsrv.war /itbsrv
COPY gitb-entrypoint.sh /itbsrv
RUN sh -c 'touch /itbsrv/itbsrv.war'
RUN chmod +x /itbsrv/gitb-entrypoint.sh

EXPOSE 8080-8090
EXPOSE 80
WORKDIR /itbsrv

ENTRYPOINT ["/itbsrv/gitb-entrypoint.sh"]