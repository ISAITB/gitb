<?xml version="1.0" encoding="UTF-8"?>
<!-- 

        	FACTURAE syntax binding to the T10   
        	Author: Oriol Bausà

     -->
<schema xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:ubl="http://www.facturae.es/Facturae/2009/v3.2/Facturae" queryBinding="xslt2" xmlns="http://purl.oclc.org/dsdl/schematron">
  <title>BIIRULES  T10 bound to FACTURAE</title>
  <ns prefix="cbc" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"/>
  <ns prefix="cac" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"/>
  <ns prefix="facturae" uri="http://www.facturae.es/Facturae/2009/v3.2/Facturae"/>
  <phase id="BIIRULEST10_phase">
    <active pattern="FACTURAE-T10"/>
  </phase>
  <phase id="codelist_phase">
    <active pattern="CodesT10"/>
  </phase>
  <!-- Abstract CEN BII patterns -->
  <!-- ========================= -->
  <include href="abstract/BIIRULES-T10.sch"/>
  <!-- Data Binding parameters -->
  <!-- ======================= -->
  <include href="Facturae/BIIRULES-Facturae-T10.sch"/>
  <!-- Code Lists Binding rules -->
  <!-- ======================== -->
  <include href="codelist/BIIRULESCodesT10.sch"/>
</schema>
