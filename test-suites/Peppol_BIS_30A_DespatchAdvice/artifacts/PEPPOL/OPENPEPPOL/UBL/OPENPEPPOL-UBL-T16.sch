<!-- Schematron binding rules generated automatically. -->
<!-- Data binding to UBL syntax for T16 -->
<!-- (2009). Invinet Sistemes -->
<pattern xmlns="http://purl.oclc.org/dsdl/schematron" is-a="T16" id="UBL-T16">
  <param name="EUGEN-T16-R001" value="@schemeID"/>
  <param name="EUGEN-T16-R002" value="(//cac:DespatchSupplierParty/cac:Party/cac:PartyIdentification/cbc:ID/@schemeID) and (//cac:DeliveryCustomerParty/cac:Party/cac:PartyIdentification/cbc:ID/@schemeID)"/>
  <param name="EUGEN-T16-R003" value="(cbc:IdentificationCode/@listID='ISO3166-1:Alpha2')"/>
  <param name="EUGEN-T16-R004" value="not(attribute::unitCode) or (attribute::unitCode and attribute::unitCodeListID = 'UNECERec20')"/>
  <param name="EUGEN-T16-R005" value="@listID = 'UNCL8273'"/>
  <param name="EUGEN-T16-R006" value="@listID = 'UNECERec21'"/>
  <param name="unit_code" value="//*[contains(name(),'Quantity')]"/>
  <param name="undg_code" value="cbc:UNDGCode"/>
  <param name="transport_handling_unit_type" value="cbc:TransportHandlingUnitTypeCode"/>
  <param name="despatch_advice" value="/ubl:DespatchAdvice"/>
  <param name="country" value="/cac:Country"/>
  <param name="endpoint" value="//cbc:EndpointID"/>
</pattern>
