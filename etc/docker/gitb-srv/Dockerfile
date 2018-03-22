FROM tomcat:8.0.32-jre8

RUN rm -fr /usr/local/tomcat/webapps/*

COPY itbsrv.war /usr/local/tomcat/webapps
COPY setenv.sh /usr/local/tomcat/bin

EXPOSE 8080-8380
EXPOSE 80