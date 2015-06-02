<?xml version="1.0" encoding="UTF-8"?>
<!-- 

        	UBL syntax binding to the T14   
        	Author: Oriol Bausà

     -->
<schema xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:ubl="urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2" queryBinding="xslt2" xmlns="http://purl.oclc.org/dsdl/schematron">
  <title>BIICORE  T14 bound to UBL</title>
  <ns prefix="cbc" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"/>
  <ns prefix="cac" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"/>
  <ns prefix="ubl" uri="urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2"/>
  <phase id="BIICORET14_phase">
    <active pattern="UBL-T14"/>
  </phase>
  <!-- Abstract CEN BII patterns -->
  <!-- ========================= -->
  <include href="abstract/BIICORE-T14.sch"/>
  <!-- Data Binding parameters -->
  <!-- ======================= -->
  <include href="UBL/BIICORE-UBL-T14.sch"/>
</schema>
