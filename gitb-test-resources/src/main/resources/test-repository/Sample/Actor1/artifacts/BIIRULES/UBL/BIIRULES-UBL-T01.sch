<!-- Schematron binding rules generated automatically. -->
<!-- Data binding to UBL syntax for T01 -->
<!-- (2009). Invinet Sistemes -->
<pattern id="UBL-T01" xmlns="http://purl.oclc.org/dsdl/schematron" is-a="T01">
  <param value="(cbc:UBLVersionID)" name="BIIRULE-T01-R001"/>
  <param value="(cbc:CustomizationID)" name="BIIRULE-T01-R002"/>
  <param value="(cbc:ProfileID)" name="BIIRULE-T01-R003"/>
  <param value="(@languageID)" name="BIIRULE-T01-R004"/>
  <param value="(@languageID)" name="BIIRULE-T01-R005"/>
  <param value="(cbc:DocumentType) and (cbc:DocumentType != '')" name="BIIRULE-T01-R006"/>
  <param value="(cbc:DocumentType) and (cbc:DocumentType != '')" name="BIIRULE-T01-R007"/>
  <param value="((cbc:ID) and (cbc:ID != '' )) or ((cbc:ContractType) and (cbc:ContractType != '' ))" name="BIIRULE-T01-R008"/>
  <param value="(cac:Party/cac:PartyName/cbc:Name)" name="BIIRULE-T01-R009"/>
  <param value="(cac:Party/cac:PartyName/cbc:Name)" name="BIIRULE-T01-R010"/>
  <param value="(cbc:StartDate and cbc:EndDate) and not(number(translate(cbc:StartDate,'-','')) &gt; number(translate(cbc:EndDate,'-',''))) or number(translate(cbc:EndDate,'-','')) = number(translate(cbc:StartDate,'-',''))" name="BIIRULE-T01-R011"/>
  <param value="(cac:Party/cac:PostalAddress/cbc:CityName and cac:Party/cac:PostalAddress/cbc:PostalZone) or (cac:Party/cac:PostalAddress/AddressLine)" name="BIIRULE-T01-R012"/>
  <param value="((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID) and (cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and (following::cac:BuyerCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) = (following::cac:BuyerCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) or ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) != (following::cac:BuyerCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and cac:Party/cac:PartyTaxScheme/cac:TaxScheme/cbc:ID='VAT' and starts-with(cac:Party/cac:PartyTaxScheme/cbc:CompanyID,cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)))) or not((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID)) or not((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)) or not((following::cac:BuyerCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode))" name="BIIRULE-T01-R013"/>
  <param value="(cac:Party/cac:PostalAddress/cbc:CityName and cac:Party/cac:PostalAddress/cbc:PostalZone) or (cac:Party/cac:PostalAddress/AddressLine)" name="BIIRULE-T01-R014"/>
  <param value="((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID) and (cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and (preceding::cac:SellerSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and  ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) = (preceding::cac:SellerSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) or ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) != (preceding::cac:SellerSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and cac:Party/cac:PartyTaxScheme/cac:TaxScheme/cbc:ID='VAT' and starts-with(cac:Party/cac:PartyTaxScheme/cbc:CompanyID,cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)))) or not((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID)) or not((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)) or not((preceding::cac:SellerSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode))" name="BIIRULE-T01-R015"/>
  <param value="cbc:AllowanceChargeReason and (cbc:AllowanceChargeReason != '' )" name="BIIRULE-T01-R016"/>
  <param value="count(cac:TaxSubtotal)&gt;1 and (cac:TaxSubtotal[1]/cac:TaxCategory/cac:TaxScheme/cbc:ID) =(cac:TaxSubtotal[2]/cac:TaxCategory/cac:TaxScheme/cbc:ID) or count(cac:TaxSubtotal)&lt;=1" name="BIIRULE-T01-R017"/>
  <param value="number(cbc:LineExtensionAmount) = number(round(sum(//cac:LineItem/cbc:LineExtensionAmount) * 100) div 100)" name="BIIRULE-T01-R018"/>
  <param value="(cbc:AllowanceTotalAmount) and cbc:AllowanceTotalAmount = (round(sum(/ubl:Order/cac:AllowanceCharge[cbc:ChargeIndicator=&quot;false&quot;]/cbc:Amount) * 100) div 100) or not(cbc:AllowanceTotalAmount)" name="BIIRULE-T01-R019"/>
  <param value="(cbc:ChargeTotalAmount) and cbc:ChargeTotalAmount = (round(sum(/ubl:Order/cac:AllowanceCharge[cbc:ChargeIndicator=&quot;true&quot;]/cbc:Amount) * 100) div 100) or not(cbc:ChargeTotalAmount)" name="BIIRULE-T01-R020"/>
  <param value="((cbc:ChargeTotalAmount) and (cbc:AllowanceTotalAmount) and (number(cbc:PayableAmount) = (number(cbc:LineExtensionAmount) + number(cbc:ChargeTotalAmount) - number(cbc:AllowanceTotalAmount)))) or (not(cbc:ChargeTotalAmount) and (cbc:AllowanceTotalAmount) and (number(cbc:PayableAmount) = number(cbc:LineExtensionAmount) - number(cbc:AllowanceTotalAmount))) or ((cbc:ChargeTotalAmount) and not(cbc:AllowanceTotalAmount) and (number(cbc:PayableAmount) = number(cbc:LineExtensionAmount) + number(cbc:ChargeTotalAmount))) or (not(cbc:ChargeTotalAmount) and not(cbc:AllowanceTotalAmount))" name="BIIRULE-T01-R021"/>
  <param value="not(cbc:Quantity) or not(cac:Price/cbc:PriceAmount) or (not(cac:Price/cbc:BaseQuantity) and  number(cbc:LineExtensionAmount) = (round(number(cac:Price/cbc:PriceAmount) *number(cbc:Quantity) * 100) div 100) + ( sum(cac:AllowanceCharge[child::cbc:ChargeIndicator='true']/cbc:Amount) ) - ( sum(cac:AllowanceCharge[child::cbc:ChargeIndicator='false']/cbc:Amount) ) ) or ((cac:Price/cbc:BaseQuantity) and  number(cbc:LineExtensionAmount) = (round(number(cac:Price/cbc:PriceAmount) div (number(cac:Price/cbc:BaseQuantity)) * number(cbc:Quantity) * 100) div 100)+ ( sum(cac:AllowanceCharge[child::cbc:ChargeIndicator='true']/cbc:Amount) ) -( sum(cac:AllowanceCharge[child::cbc:ChargeIndicator='false']/cbc:Amount)))" name="BIIRULE-T01-R022"/>
  <param value="string-length(string(cbc:Name)) &lt;= 50" name="BIIRULE-T01-R023"/>
  <param value="not((cac:StandardItemIdentification)) or (cac:StandardItemIdentification/cbc:ID/@schemeID)" name="BIIRULE-T01-R024"/>
  <param value="not((cac:CommodityClassification)) or (cac:CommodityClassification/cbc:ItemClassificationCode/@listID)" name="BIIRULE-T01-R025"/>
  <param value="number(.) &gt;=0" name="BIIRULE-T01-R026"/>
  <param value="//cac:BuyerCustomerParty" name="Customer"/>
  <param value="//cac:LineItem" name="Order_Line"/>
  <param value="//cac:RequestedDeliveryPeriod" name="Requested_delivery_period"/>
  <param value="/ubl:Order" name="Order"/>
  <param value="//cac:LineItem/cac:Price/cbc:PriceAmount" name="Item_Price"/>
  <param value="//cac:Item" name="Item"/>
  <param value="//cac:SellerSupplierParty" name="Supplier"/>
  <param value="/ubl:Order/cac:TaxTotal" name="Tax_Total"/>
  <param value="//cac:AnticipatedMonetaryTotal" name="Total_Amounts"/>
  <param value="//cac:OriginatorDocumentReference" name="Originator_document"/>
  <param value="//cac:AdditionalDocumentReference" name="Annex"/>
  <param value="//cbc:Note" name="Order_Note"/>
  <param value="//cac:LineItem/cbc:Note" name="Line_Note"/>
  <param value="//cac:Contract" name="Contract"/>
  <param value="//cac:AllowanceCharge" name="AllowanceCharge"/>
</pattern>
