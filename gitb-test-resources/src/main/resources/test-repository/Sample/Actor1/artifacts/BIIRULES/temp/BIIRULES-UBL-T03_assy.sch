<?xml version="1.0" encoding="utf-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:ubl="urn:oasis:names:specification:ubl:schema:xsd:OrderResponseSimple-2" queryBinding="xslt2">
  <title>BIIRULES  T03 bound to UBL</title>
  <ns prefix="cbc" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"/>
  <ns prefix="cac" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"/>
  <ns prefix="ubl" uri="urn:oasis:names:specification:ubl:schema:xsd:OrderResponseSimple-2"/>
  <phase id="BIIRULEST03_phase">
    <active pattern="UBL-T03"/>
  </phase>
  
  
  <!--Suppressed abstract pattern T03 was here-->
  
  
  <!--Start pattern based on abstract T03--><pattern id="UBL-T03">
  <rule context="/ubl:OrderResponseSimple">
    <assert test="(cbc:UBLVersionID)" flag="fatal">[BIIRULE-T03-R001]-A conformant CEN BII order response core data model MUST have a syntax identifier.</assert>
    <assert test="(cbc:CustomizationID)" flag="fatal">[BIIRULE-T03-R002]-A conformant CEN BII order response  core data model MUST have a syntax identifier.</assert>
    <assert test="(cbc:ProfileID)" flag="fatal">[BIIRULE-T03-R003]-A conformant CEN BII order response  core data model MUST have a profile identifier.</assert>
    <assert test="not(cbc:Note) or count(cbc:Note)=1" flag="warning">[BIIRULE-T03-R004]-Only one note field must be specified </assert>
  </rule>
  <rule context="/ubl:OrderResponseSimple_Note">
    <assert test="(@languageID)" flag="warning">[BIIRULE-T03-R005]-Language SHOULD be defined for note field</assert>
  </rule>
  <rule context="//cac:BuyerCustomerParty">
    <assert test="(cac:Party/cac:PartyName/cbc:Name)" flag="fatal">[BIIRULE-T03-R006]-An order response  MUST contain the full name of the customer.</assert>
  </rule>
  <rule context="//cac:SellerSupplierParty">
    <assert test="(cac:Party/cac:PartyName/cbc:Name)" flag="fatal">[BIIRULE-T03-R007]-An order response  MUST contain the full name of the supplier.</assert>
  </rule>
</pattern>
</schema>