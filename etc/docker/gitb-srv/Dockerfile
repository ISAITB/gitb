FROM openjdk:11.0.14-jre

RUN mkdir /itbsrv
COPY itbsrv.war /itbsrv
RUN sh -c 'touch /itbsrv/itbsrv.war'
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Xmx2048m","-jar","/itbsrv/itbsrv.war"]
EXPOSE 8080-8380
EXPOSE 80
WORKDIR /itbsrv