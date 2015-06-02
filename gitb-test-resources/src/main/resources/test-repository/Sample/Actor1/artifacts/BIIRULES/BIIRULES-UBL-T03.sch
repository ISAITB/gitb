<?xml version="1.0" encoding="UTF-8"?>
<!-- 

        	UBL syntax binding to the T03   
        	Author: Oriol Bausà

     -->
<schema xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:ubl="urn:oasis:names:specification:ubl:schema:xsd:OrderResponseSimple-2" queryBinding="xslt2" xmlns="http://purl.oclc.org/dsdl/schematron">
  <title>BIIRULES  T03 bound to UBL</title>
  <ns prefix="cbc" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"/>
  <ns prefix="cac" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"/>
  <ns prefix="ubl" uri="urn:oasis:names:specification:ubl:schema:xsd:OrderResponseSimple-2"/>
  <phase id="BIIRULEST03_phase">
    <active pattern="UBL-T03"/>
  </phase>
  <!-- Abstract CEN BII patterns -->
  <!-- ========================= -->
  <include href="abstract/BIIRULES-T03.sch"/>
  <!-- Data Binding parameters -->
  <!-- ======================= -->
  <include href="UBL/BIIRULES-UBL-T03.sch"/>
</schema>
