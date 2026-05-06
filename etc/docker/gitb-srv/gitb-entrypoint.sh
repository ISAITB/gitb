#!/bin/bash
exec java \
  -Djava.security.egd=file:/dev/./urandom \
  -Xmx2048m \
  ${JVM_OPTIONS} \
  -jar /itbsrv/itbsrv.war

