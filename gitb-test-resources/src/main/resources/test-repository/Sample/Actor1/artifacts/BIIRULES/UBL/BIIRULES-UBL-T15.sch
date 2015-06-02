<!-- Schematron binding rules generated automatically. -->
<!-- Data binding to UBL syntax for T15 -->
<!-- (2009). Invinet Sistemes -->
<pattern id="UBL-T15" xmlns="http://purl.oclc.org/dsdl/schematron" is-a="T15">
  <param value="(cbc:StartDate and cbc:EndDate) and not(number(translate(cbc:StartDate,'-','')) &gt; number(translate(cbc:EndDate,'-',''))) or number(translate(cbc:EndDate,'-','')) = number(translate(cbc:StartDate,'-',''))" name="BIIRULE-T15-R001"/>
  <param value="(cac:Party/cac:PostalAddress/cbc:CityName and cac:Party/cac:PostalAddress/cbc:PostalZone) or (cac:Party/cac:PostalAddress/cbc:ID)" name="BIIRULE-T15-R002"/>
  <param value="((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID) and (cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and (following::cac:AccountingCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) = (following::cac:AccountingCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) or ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) != (following::cac:AccountingCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and cac:Party/cac:PartyTaxScheme/cac:TaxScheme/cbc:ID='VAT' and starts-with(cac:Party/cac:PartyTaxScheme/cbc:CompanyID,cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)))) or not((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID)) or not((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)) or not((following::cac:AccountingCustomerParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode))" name="BIIRULE-T15-R003"/>
  <param value="(cac:Party/cac:PostalAddress/cbc:CityName and cac:Party/cac:PostalAddress/cbc:PostalZone) or (cac:Party/cac:PostalAddress/cbc:ID)" name="BIIRULE-T15-R004"/>
  <param value="((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID) and (cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and (preceding::cac:AccountingSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and  ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) = (preceding::cac:AccountingSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) or ((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) != (preceding::cac:AccountingSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode) and cac:Party/cac:PartyTaxScheme/cac:TaxScheme/cbc:ID='VAT' and starts-with(cac:Party/cac:PartyTaxScheme/cbc:CompanyID,cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)))) or not((cac:Party/cac:PartyTaxScheme[cac:TaxScheme/cbc:ID='VAT']/cbc:CompanyID)) or not((cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode)) or not((preceding::cac:AccountingSupplierParty/cac:Party/cac:PostalAddress/cac:Country/cbc:IdentificationCode))" name="BIIRULE-T15-R005"/>
  <param value="(cbc:PaymentDueDate and /ubl:Invoice/cbc:IssueDate) and (number(translate(cbc:PaymentDueDate,'-','')) &gt;= number(translate(/ubl:Invoice/cbc:IssueDate,'-',''))) or (not(cbc:PaymentDueDate))" name="BIIRULE-T15-R006"/>
  <param value="(cbc:PaymentMeansCode = '31') and //cac:PayeeFinancialAccount/cbc:ID or (cbc:PaymentMeansCode != '31')" name="BIIRULE-T15-R007"/>
  <param value="(cac:PayeeFinancialAccount/cbc:ID/@schemeID and (cac:PayeeFinancialAccount/cbc:ID/@schemeID = 'IBAN') and cac:PayeeFinancialAccount/cac:FinancialInstitutionBranch/cac:FinancialInstitution/cbc:ID) or (cac:PayeeFinancialAccount/cbc:ID/@schemeID != 'IBAN') or (not(cac:PayeeFinancialAccount/cbc:ID/@schemeID))" name="BIIRULE-T15-R008"/>
  <param value="count(cac:TaxSubtotal)&gt;1 and (cac:TaxSubtotal[1]/cac:TaxCategory/cac:TaxScheme/cbc:ID) =(cac:TaxSubtotal[2]/cac:TaxCategory/cac:TaxScheme/cbc:ID) or count(cac:TaxSubtotal)&lt;=1" name="BIIRULE-T15-R009"/>
  <param value="number(cbc:TaxAmount) = number(round(sum(cac:TaxSubtotal/cbc:TaxAmount) * 100) div 100)" name="BIIRULE-T15-R010"/>
  <param value="number(cbc:LineExtensionAmount) = number(round(sum(//cac:InvoiceLine/cbc:LineExtensionAmount) * 100) div 100)" name="BIIRULE-T15-R011"/>
  <param value="((cbc:ChargeTotalAmount) and (cbc:AllowanceTotalAmount) and (number(cbc:TaxExclusiveAmount) = (number(cbc:LineExtensionAmount) + number(cbc:ChargeTotalAmount) - number(cbc:AllowanceTotalAmount)))) or (not(cbc:ChargeTotalAmount) and (cbc:AllowanceTotalAmount) and (number(cbc:TaxExclusiveAmount) = number(cbc:LineExtensionAmount) - number(cbc:AllowanceTotalAmount))) or ((cbc:ChargeTotalAmount) and not(cbc:AllowanceTotalAmount) and (number(cbc:TaxExclusiveAmount) = number(cbc:LineExtensionAmount) + number(cbc:ChargeTotalAmount))) or (not(cbc:ChargeTotalAmount) and not(cbc:AllowanceTotalAmount) and (number(cbc:TaxExclusiveAmount) = number(cbc:LineExtensionAmount)))" name="BIIRULE-T15-R012"/>
  <param value="((cbc:PayableRoundingAmount) and (number(cbc:TaxInclusiveAmount) = (round((number(cbc:TaxExclusiveAmount) + number(round(sum(/ubl:Invoice/cac:TaxTotal/cbc:TaxAmount) * 100) div 100) + number(cbc:PayableRoundingAmount)) * 100) div 100)))  or (number(cbc:TaxInclusiveAmount) = (round((number(cbc:TaxExclusiveAmount) + number(sum(/ubl:Invoice/cac:TaxTotal/cbc:TaxAmount))) * 100) div 100))" name="BIIRULE-T15-R013"/>
  <param value="number(cbc:TaxInclusiveAmount) &gt;= 0" name="BIIRULE-T15-R014"/>
  <param value="(cbc:AllowanceTotalAmount) and cbc:AllowanceTotalAmount = (round(sum(/ubl:Invoice/cac:AllowanceCharge[cbc:ChargeIndicator=&quot;false&quot;]/cbc:Amount) * 100) div 100) or not(cbc:AllowanceTotalAmount)" name="BIIRULE-T15-R015"/>
  <param value="(cbc:ChargeTotalAmount) and cbc:ChargeTotalAmount = (round(sum(/ubl:Invoice/cac:AllowanceCharge[cbc:ChargeIndicator=&quot;true&quot;]/cbc:Amount) * 100) div 100) or not(cbc:ChargeTotalAmount)" name="BIIRULE-T15-R016"/>
  <param value="(cbc:PrepaidAmount) and (number(cbc:PayableAmount) = number(cbc:TaxInclusiveAmount - cbc:PrepaidAmount)) or cbc:PayableAmount = cbc:TaxInclusiveAmount" name="BIIRULE-T15-R017"/>
  <param value="not(cbc:InvoicedQuantity) or not(cac:Price/cbc:PriceAmount) or (not(cac:Price/cbc:BaseQuantity) and  number(cbc:LineExtensionAmount) = (round(number(cac:Price/cbc:PriceAmount) *number(cbc:InvoicedQuantity) * 100) div 100) + ( sum(cac:AllowanceCharge[child::cbc:ChargeIndicator='true']/cbc:Amount) ) - ( sum(cac:AllowanceCharge[child::cbc:ChargeIndicator='false']/cbc:Amount) ) ) or ((cac:Price/cbc:BaseQuantity) and  number(cbc:LineExtensionAmount) = (round(number(cac:Price/cbc:PriceAmount) div (number(cac:Price/cbc:BaseQuantity)) * number(cbc:InvoicedQuantity) * 100) div 100)+ ( sum(cac:AllowanceCharge[child::cbc:ChargeIndicator='true']/cbc:Amount) ) -( sum(cac:AllowanceCharge[child::cbc:ChargeIndicator='false']/cbc:Amount)))" name="BIIRULE-T15-R018"/>
  <param value="string-length(string(cbc:Name)) &lt;= 50" name="BIIRULE-T15-R019"/>
  <param value="not((cac:StandardItemIdentification)) or (cac:StandardItemIdentification/cbc:ID/@schemeID)" name="BIIRULE-T15-R020"/>
  <param value="not((cac:CommodityClassification)) or (cac:CommodityClassification/cbc:ItemClassificationCode/@listID)" name="BIIRULE-T15-R021"/>
  <param value="number(.) &gt;=0" name="BIIRULE-T15-R022"/>
  <param value="(cac:BillingReference/cac:InvoiceDocumentReference/cbc:ID) or (cac:BillingReference/cac:CreditNoteDocumentReferene/cbc:ID)" name="BIIRULE-T15-R023"/>
  <param value="(cbc:IssueDate)" name="BIIRULE-T15-R024"/>
  <param value="(cbc:ID)" name="BIIRULE-T15-R025"/>
  <param value="(cac:Item/cbc:Name)" name="BIIRULE-T15-R026"/>
  <param value="(cac:AccountingSupplierParty/cac:Party/cac:PartyName/cbc:Name)" name="BIIRULE-T15-R027"/>
  <param value="(cac:AccountingCustomerParty/cac:Party/cac:PartyName/cbc:Name)" name="BIIRULE-T15-R028"/>
  <param value="((cac:TaxTotal[cac:TaxSubtotal/cac:TaxCategory/cac:TaxScheme/cbc:ID = 'VAT']/cbc:TaxAmount) and (sum(cac:TaxTotal//cac:TaxSubtotal/cbc:TaxableAmount) = number(cac:LegalMonetaryTotal/cbc:TaxExclusiveAmount))) or not((cac:TaxTotal[cac:TaxSubtotal/cac:TaxCategory/cac:TaxScheme/cbc:ID = 'VAT']))" name="BIIRULE-T15-R029"/>
  <param value="(cbc:UBLVersionID)" name="BIIRULE-T15-R030"/>
  <param value="(cbc:CustomizationID)" name="BIIRULE-T15-R031"/>
  <param value="(cbc:ProfileID)" name="BIIRULE-T15-R032"/>
  <param value="cbc:ID" name="BIIRULE-T15-R033"/>
  <param value="(cac:InvoiceLine)" name="BIIRULE-T15-R034"/>
  <param value="(cbc:DocumentCurrencyCode)" name="BIIRULE-T15-R035"/>
  <param value="(cac:OrderReference/cbc:ID) or not(cac:OrderReference)" name="BIIRULE-T15-R036"/>
  <param value="(cac:ContractDocumentReference/cbc:ID) or not(cac:ContractDocumentReference)" name="BIIRULE-T15-R037"/>
  <param value="cbc:ID" name="BIIRULE-T15-R038"/>
  <param value="(cac:LegalMonetaryTotal/cbc:PayableAmount)" name="BIIRULE-T15-R039"/>
  <param value="(cac:LegalMonetaryTotal/cbc:TaxInclusiveAmount)" name="BIIRULE-T15-R040"/>
  <param value="(cbc:IdentificationCode)" name="BIIRULE-T15-R041"/>
  <param value="(cbc:CompanyID)" name="BIIRULE-T15-R042"/>
  <param value="(cac:LegalMonetaryTotal/cbc:TaxExclusiveAmount)" name="BIIRULE-T15-R043"/>
  <param value="(cac:LegalMonetaryTotal/cbc:LineExtensionAmount)" name="BIIRULE-T15-R044"/>
  <param value="not(cac:PaymentMeans) or (cac:PaymentMeans/cbc:PaymentMeansCode)" name="BIIRULE-T15-R045"/>
  <param value="(cac:TaxTotal/cbc:TaxAmount)" name="BIIRULE-T15-R046"/>
  <param value="not(cac:TaxSubtotal) or (cac:TaxSubtotal/cbc:TaxableAmount)" name="BIIRULE-T15-R047"/>
  <param value="not(cac:TaxSubtotal) or (cac:TaxSubtotal/cbc:TaxAmount)" name="BIIRULE-T15-R048"/>
  <param value="cbc:ID" name="BIIRULE-T15-R049"/>
  <param value="cbc:ID" name="BIIRULE-T15-R050"/>
  <param value="cbc:LineExtensionAmount" name="BIIRULE-T15-R051"/>
  <param value="//cac:LegalMonetaryTotal" name="Total_Amounts"/>
  <param value="/ubl:Invoice/cac:TaxTotal" name="Tax_Total"/>
  <param value="//cac:TaxScheme" name="Tax_Scheme"/>
  <param value="//cac:TaxCategory" name="Tax_Category"/>
  <param value="//cac:AccountingSupplierParty" name="Supplier"/>
  <param value="//cac:PaymentMeans" name="Payment_Means"/>
  <param value="//cac:PartyLegalEntity" name="Party_Legal_Entity"/>
  <param value="//cac:InvoiceLine/cac:Price/cbc:PriceAmount" name="Item_Price"/>
  <param value="//cac:Item" name="Item"/>
  <param value="//cac:InvoicePeriod" name="Invoice_Period"/>
  <param value="//cac:InvoiceLine" name="Invoice_Line"/>
  <param value="/ubl:Invoice" name="Invoice"/>
  <param value="//cac:AccountingCustomerParty" name="Customer"/>
  <param value="//cac:Country" name="Country"/>
  <param value="//cac:AdditionalDocumentReference" name="Annex"/>
</pattern>
