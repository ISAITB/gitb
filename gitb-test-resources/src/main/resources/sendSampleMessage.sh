#!/bin/sh
curl --header "Content-Type: text/xml;charset=UTF-8" --header "SOAPAction:GetTestCaseDefinitionRequest" --data @sampleSOAPMessage.xml localhost:8081
