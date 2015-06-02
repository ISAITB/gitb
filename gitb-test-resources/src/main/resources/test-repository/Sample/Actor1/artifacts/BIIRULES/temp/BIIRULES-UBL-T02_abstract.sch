<?xml version="1.0" encoding="utf-8"?><!-- 

        	UBL syntax binding to the T02   
        	Author: Oriol Bausà

     --><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:ubl="urn:oasis:names:specification:ubl:schema:xsd:OrderResponseSimple-2" queryBinding="xslt2">
  <title>BIIRULES  T02 bound to UBL</title>
  <ns prefix="cbc" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"/>
  <ns prefix="cac" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"/>
  <ns prefix="ubl" uri="urn:oasis:names:specification:ubl:schema:xsd:OrderResponseSimple-2"/>
  <phase id="BIIRULEST02_phase">
    <active pattern="UBL-T02"/>
  </phase>
  <!-- Abstract CEN BII patterns -->
  <!-- ========================= -->
  <?DSDL_INCLUDE_START abstract/BIIRULES-T02.sch?><pattern abstract="true" id="T02">
  <rule context="$Order_Response">
    <assert test="$BIIRULE-T02-R001" flag="fatal">[BIIRULE-T02-R001]-A conformant CEN BII order core data model MUST have a syntax identifier.</assert>
    <assert test="$BIIRULE-T02-R002" flag="fatal">[BIIRULE-T02-R002]-A conformant CEN BII order core data model MUST have a syntax identifier.</assert>
    <assert test="$BIIRULE-T02-R003" flag="fatal">[BIIRULE-T02-R003]-A conformant CEN BII order core data model MUST have a profile identifier.</assert>
    <assert test="$BIIRULE-T02-R004" flag="warning">[BIIRULE-T02-R004]-Only one note field must be specified </assert>
  </rule>
  <rule context="$Order_Response_Note">
    <assert test="$BIIRULE-T02-R005" flag="warning">[BIIRULE-T02-R005]-Language SHOULD be defined for note field</assert>
  </rule>
  <rule context="$Customer">
    <assert test="$BIIRULE-T02-R006" flag="fatal">[BIIRULE-T02-R006]-An order response  MUST contain the full name of the customer.</assert>
  </rule>
  <rule context="$Supplier">
    <assert test="$BIIRULE-T02-R007" flag="fatal">[BIIRULE-T02-R007]-An order response  MUST contain the full name of the supplier.</assert>
  </rule>
</pattern><?DSDL_INCLUDE_END abstract/BIIRULES-T02.sch?>
  <!-- Data Binding parameters -->
  <!-- ======================= -->
  <?DSDL_INCLUDE_START UBL/BIIRULES-UBL-T02.sch?><pattern id="UBL-T02" is-a="T02">
  <param value="(cbc:UBLVersionID)" name="BIIRULE-T02-R001"/>
  <param value="(cbc:CustomizationID)" name="BIIRULE-T02-R002"/>
  <param value="(cbc:ProfileID)" name="BIIRULE-T02-R003"/>
  <param value="not(cbc:Note) or count(cbc:Note)=1" name="BIIRULE-T02-R004"/>
  <param value="(@languageID)" name="BIIRULE-T02-R005"/>
  <param value="(cac:Party/cac:PartyName/cbc:Name)" name="BIIRULE-T02-R006"/>
  <param value="(cac:Party/cac:PartyName/cbc:Name)" name="BIIRULE-T02-R007"/>
  <param value="//cac:BuyerCustomerParty" name="Customer"/>
  <param value="/ubl:OrderResponseSimple" name="Order_Response"/>
  <param value="//cac:SellerSupplierParty" name="Supplier"/>
  <param value="//cbc:Note" name="Order_Response_Note"/>
</pattern><?DSDL_INCLUDE_END UBL/BIIRULES-UBL-T02.sch?>
</schema>