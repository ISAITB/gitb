FROM eclipse-temurin:25-jre-jammy

RUN mkdir /itbsrv
COPY itbsrv.war /itbsrv
RUN sh -c 'touch /itbsrv/itbsrv.war'

EXPOSE 8080-8090
EXPOSE 80
WORKDIR /itbsrv

# JDK_JAVA_OPTIONS is the JVM standard environment variable for passing arguments to the JVM
# This can be set when creating a container to override the defaults.
ENV JDK_JAVA_OPTIONS="-Xmx2048m"
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/itbsrv/itbsrv.war"]