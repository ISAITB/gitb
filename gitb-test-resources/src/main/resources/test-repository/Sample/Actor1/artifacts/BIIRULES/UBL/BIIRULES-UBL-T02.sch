<!-- Schematron binding rules generated automatically. -->
<!-- Data binding to UBL syntax for T02 -->
<!-- (2009). Invinet Sistemes -->
<pattern id="UBL-T02" xmlns="http://purl.oclc.org/dsdl/schematron" is-a="T02">
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
</pattern>
