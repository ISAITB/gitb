<!-- Schematron binding rules generated automatically. -->
<!-- Data binding to UBL syntax for T03 -->
<!-- (2009). Invinet Sistemes -->
<pattern id="UBL-T03" xmlns="http://purl.oclc.org/dsdl/schematron" is-a="T03">
  <param value="(cbc:UBLVersionID)" name="BIIRULE-T03-R001"/>
  <param value="(cbc:CustomizationID)" name="BIIRULE-T03-R002"/>
  <param value="(cbc:ProfileID)" name="BIIRULE-T03-R003"/>
  <param value="not(cbc:Note) or count(cbc:Note)=1" name="BIIRULE-T03-R004"/>
  <param value="(@languageID)" name="BIIRULE-T03-R005"/>
  <param value="(cac:Party/cac:PartyName/cbc:Name)" name="BIIRULE-T03-R006"/>
  <param value="(cac:Party/cac:PartyName/cbc:Name)" name="BIIRULE-T03-R007"/>
  <param value="//cac:BuyerCustomerParty" name="Customer"/>
  <param value="/ubl:OrderResponseSimple" name="Order_Response"/>
  <param value="//cac:SellerSupplierParty" name="Supplier"/>
  <param value="//cbc:Note" name="Order_Response_Note"/>
</pattern>
