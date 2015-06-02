<!-- Schematron binding rules generated automatically. -->
<!-- Data binding to UBL syntax for T14 -->
<!-- (2009). Invinet Sistemes -->
<pattern id="UBL-T14" xmlns="http://purl.oclc.org/dsdl/schematron" is-a="T14">
  <param value="(cbc:StartDate and cbc:EndDate) and not(number(translate(cbc:StartDate,'-','')) &gt; number(translate(cbc:EndDate,'-',''))) or number(translate(cbc:EndDate,'-','')) = number(translate(cbc:StartDate,'-',''))" name="BIIRULE-T14-R001"/>
  <param value="(cac:Party/cac:PostalAddress/cbc:CityName and cac:Party/cac:PostalAddress/cbc:PostalZone) or (cac:Party/cac:PostalAddress/cbc:ID)" name="BIIRULE-T14-R002"/>
  <param value="((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID) and (cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and (following::cac:AccountingCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) = (following::cac:AccountingCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) or ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) != (following::cac:AccountingCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and cac:Party/cac:PartyTaxScheme/cac:TaxScheme/cbc:ID='VAT' and starts-with(cac:Party/cac:PartyTaxScheme/cbc:CompanyID,cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)))) or not((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID)) or not((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)) or not((following::cac:AccountingCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode))" name="BIIRULE-T14-R003"/>
  <param value="(cac:Party/cac:PostalAddress/cbc:CityName and cac:Party/cac:PostalAddress/cbc:PostalZone) or (cac:Party/cac:PostalAddress/cbc:ID)" name="BIIRULE-T14-R004"/>
  <param value="((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID) and (cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and (preceding::cac:AccountingSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and  ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) = (preceding::cac:AccountingSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) or ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) != (preceding::cac:AccountingSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and cac:Party/cac:PartyTaxScheme/cac:TaxScheme/cbc:ID='VAT' and starts-with(cac:Party/cac:PartyTaxScheme/cbc:CompanyID,cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)))) or not((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID)) or not((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)) or not((preceding::cac:AccountingSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode))" name="BIIRULE-T14-R005"/>
  <param value="count(cac:TaxSubtotal)&gt;1 and (cac:TaxSubtotal[1]/cac:TaxCategory/cac:TaxScheme/cbc:ID) =(cac:TaxSubtotal[2]/cac:TaxCategory/cac:TaxScheme/cbc:ID) or count(cac:TaxSubtotal)&lt;=1" name="BIIRULE-T14-R009"/>
  <param value="number(cbc:TaxAmount) = number(round(sum(cac:TaxSubtotal/cbc:TaxAmount) * 100) div 100)" name="BIIRULE-T14-R010"/>
  <param value="number(cbc:LineExtensionAmount) = number(round(sum(//cac:CreditNoteLine/cbc:LineExtensionAmount) * 100) div 100)" name="BIIRULE-T14-R011"/>
  <param value="((cbc:ChargeTotalAmount) and (cbc:AllowanceTotalAmount) and (number(cbc:TaxExclusiveAmount) = (number(cbc:LineExtensionAmount) + number(cbc:ChargeTotalAmount) - number(cbc:AllowanceTotalAmount)))) or (not(cbc:ChargeTotalAmount) and (cbc:AllowanceTotalAmount) and (number(cbc:TaxExclusiveAmount) = number(cbc:LineExtensionAmount) - number(cbc:AllowanceTotalAmount))) or ((cbc:ChargeTotalAmount) and not(cbc:AllowanceTotalAmount) and (number(cbc:TaxExclusiveAmount) = number(cbc:LineExtensionAmount) + number(cbc:ChargeTotalAmount))) or (not(cbc:ChargeTotalAmount) and not(cbc:AllowanceTotalAmount) and (number(cbc:TaxExclusiveAmount) = number(cbc:LineExtensionAmount)))" name="BIIRULE-T14-R012"/>
  <param value="((cbc:PayableRoundingAmount) and (number(cbc:TaxInclusiveAmount) = (round((number(cbc:TaxExclusiveAmount) + number(round(sum(preceding::cac:TaxTotal/cbc:TaxAmount) * 100) div 100) + number(cbc:PayableRoundingAmount)) * 100) div 100))) or (number(cbc:TaxInclusiveAmount) = (round((number(cbc:TaxExclusiveAmount) + number(sum(preceding::cac:TaxTotal/cbc:TaxAmount))) * 100) div 100))" name="BIIRULE-T14-R013"/>
  <param value="number(cbc:TaxInclusiveAmount) &gt;= 0" name="BIIRULE-T14-R014"/>
  <param value="(cbc:AllowanceTotalAmount) and cbc:AllowanceTotalAmount = (round(sum(preceding::cac:AllowanceCharge[cbc:ChargeIndicator=&quot;false&quot;]/cbc:Amount) * 100) div 100) or not(cbc:AllowanceTotalAmount)" name="BIIRULE-T14-R015"/>
  <param value="(cbc:ChargeTotalAmount) and cbc:ChargeTotalAmount = (round(sum(preceding::cac:AllowanceCharge[cbc:ChargeIndicator=&quot;true&quot;]/cbc:Amount) * 100) div 100) or not(cbc:ChargeTotalAmount)" name="BIIRULE-T14-R016"/>
  <param value="(cbc:PrepaidAmount) and (number(cbc:PayableAmount) = number(cbc:TaxInclusiveAmount - cbc:PrepaidAmount)) or cbc:PayableAmount = cbc:TaxInclusiveAmount" name="BIIRULE-T14-R017"/>
  <param value="string-length(string(cbc:Name)) &lt;= 50" name="BIIRULE-T14-R019"/>
  <param value="not((cac:StandardItemIdentification)) or (cac:StandardItemIdentification/cbc:ID/@schemeID)" name="BIIRULE-T14-R020"/>
  <param value="not((cac:CommodityClassification)) or (cac:CommodityClassification/cbc:ItemClassificationCode/@listID)" name="BIIRULE-T14-R021"/>
  <param value="number(.) &gt;=0" name="BIIRULE-T14-R022"/>
  <param value="number(.) &gt;=0" name="BIIRULE-T14-R023"/>
  <param value="(cbc:MultiplierFactorNumeric and cbc:BaseAmount) or (not(cbc:MultiplierFactorNumeric) and not(cbc:BaseAmount))" name="BIIRULE-T14-R024"/>
  <param value="(cbc:IssueDate)" name="BIIRULE-T14-R025"/>
  <param value="(cbc:ID)" name="BIIRULE-T14-R026"/>
  <param value="(cac:Item/cbc:Name)" name="BIIRULE-T14-R027"/>
  <param value="(cac:AccountingSupplierParty/cac:Party/cac:PartyName/cbc:Name)" name="BIIRULE-T14-R028"/>
  <param value="(cac:AccountingCustomerParty/cac:Party/cac:PartyName/cbc:Name)" name="BIIRULE-T14-R029"/>
  <param value="((cac:TaxTotal[cac:TaxSubtotal/cac:TaxCategory/cac:TaxScheme/cbc:ID = 'VAT']/cbc:TaxAmount) and (sum(cac:TaxTotal//cac:TaxSubtotal/cbc:TaxableAmount) = number(cac:LegalMonetaryTotal/cbc:TaxExclusiveAmount))) or not((cac:TaxTotal[cac:TaxSubtotal/cac:TaxCategory/cac:TaxScheme/cbc:ID = 'VAT']))" name="BIIRULE-T14-R030"/>
  <param value="(cbc:UBLVersionID)" name="BIIRULE-T14-R031"/>
  <param value="(cbc:CustomizationID)" name="BIIRULE-T14-R032"/>
  <param value="(cbc:ProfileID)" name="BIIRULE-T14-R033"/>
  <param value="cbc:ID" name="BIIRULE-T14-R034"/>
  <param value="(cac:CreditNoteLine)" name="BIIRULE-T14-R035"/>
  <param value="(cbc:DocumentCurrencyCode)" name="BIIRULE-T14-R036"/>
  <param value="(cac:LegalMonetaryTotal/cbc:PayableAmount)" name="BIIRULE-T14-R037"/>
  <param value="(cac:LegalMonetaryTotal/cbc:TaxInclusiveAmount)" name="BIIRULE-T14-R038"/>
  <param value="(cbc:CompanyID)" name="BIIRULE-T14-R039"/>
  <param value="(cac:LegalMonetaryTotal/cbc:TaxExclusiveAmount)" name="BIIRULE-T14-R040"/>
  <param value="(cac:LegalMonetaryTotal/cbc:LineExtensionAmount)" name="BIIRULE-T14-R041"/>
  <param value="(cac:TaxTotal/cbc:TaxAmount)" name="BIIRULE-T14-R042"/>
  <param value="not(cac:TaxSubtotal) or (cac:TaxSubtotal/cbc:TaxableAmount)" name="BIIRULE-T14-R043"/>
  <param value="not(cac:TaxSubtotal) or (cac:TaxSubtotal/cbc:TaxAmount)" name="BIIRULE-T14-R044"/>
  <param value="cbc:ID" name="BIIRULE-T14-R045"/>
  <param value="cbc:ID" name="BIIRULE-T14-R046"/>
  <param value="(//cac:BillingReference/cac:InvoiceDocumentReference/cbc:ID) or (//cac:BillingReference/cac:CreditNoteDocumentReference/cbc:ID)" name="BIIRULE-T14-R047"/>
  <param value="cbc:LineExtensionAmount" name="BIIRULE-T14-R050"/>
  <param value="//cac:CreditNoteLine" name="Credit_Note_Line"/>
  <param value="//cac:PartyLegalEntity" name="Party_Legal_Entity"/>
  <param value="//cac:TaxCategory" name="Tax_Category"/>
  <param value="//cac:TaxScheme" name="Tax_Scheme"/>
  <param value="/ubl:CreditNote/cac:TaxTotal" name="Tax_Total"/>
  <param value="/ubl:CreditNote" name="Credit_Note"/>
  <param value="//cac:AccountingCustomerParty" name="Customer"/>
  <param value="//cac:InvoicePeriod" name="Invoice_Period"/>
  <param value="//cac:Item" name="Item"/>
  <param value="//cac:AccountingSupplierParty" name="Supplier"/>
  <param value="//cac:LegalMonetaryTotal" name="Total_Amounts"/>
  <param value="//cac:AllowanceCharge[cbc:ChargeIndicator='false']/cbc:MultiplierFactorNumeric" name="Allowance_Percentage"/>
  <param value="//cac:AllowanceCharge[cbc:ChargeIndicator='false']" name="Allowance"/>
</pattern>
