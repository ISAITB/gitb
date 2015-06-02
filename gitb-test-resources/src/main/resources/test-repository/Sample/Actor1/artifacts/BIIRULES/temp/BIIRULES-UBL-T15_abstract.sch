<?xml version="1.0" encoding="utf-8"?><!-- 

        	UBL syntax binding to the T15   
        	Author: Oriol Bausà

     --><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:ubl="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2" queryBinding="xslt2">
  <title>BIIRULES  T15 bound to UBL</title>
  <ns prefix="cbc" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"/>
  <ns prefix="cac" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"/>
  <ns prefix="ubl" uri="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"/>
  <phase id="BIIRULEST15_phase">
    <active pattern="UBL-T15"/>
  </phase>
  <phase id="codelist_phase">
    <active pattern="CodesT15"/>
  </phase>
  <!-- Abstract CEN BII patterns -->
  <!-- ========================= -->
  <?DSDL_INCLUDE_START abstract/BIIRULES-T15.sch?><pattern abstract="true" id="T15">
  <rule context="$Total_Amounts">
    <assert test="$BIIRULE-T15-R011" flag="fatal">[BIIRULE-T15-R011]-Invoice total line extension amount MUST equal the sum of the line totals</assert>
    <assert test="$BIIRULE-T15-R012" flag="fatal">[BIIRULE-T15-R012]-An invoice tax exclusive amount MUST equal the sum of lines plus allowances and charges on header level.</assert>
    <assert test="$BIIRULE-T15-R013" flag="fatal">[BIIRULE-T15-R013]-An invoice tax inclusive amount MUST equal the tax exclusive amount plus all tax total amounts and the rounding amount.</assert>
    <assert test="$BIIRULE-T15-R014" flag="fatal">[BIIRULE-T15-R014]-Tax inclusive amount in an invoice MUST NOT be negative</assert>
    <assert test="$BIIRULE-T15-R015" flag="fatal">[BIIRULE-T15-R015]-If there is a total allowance it MUST be equal to the sum of allowances at document level</assert>
    <assert test="$BIIRULE-T15-R016" flag="fatal">[BIIRULE-T15-R016]-If there is a total charges it MUST be equal to the sum of document level charges.</assert>
    <assert test="$BIIRULE-T15-R017" flag="fatal">[BIIRULE-T15-R017]-In an invoice, amount due is the tax inclusive amount minus what has been prepaid.</assert>
  </rule>
  <rule context="$Tax_Total">
    <assert test="$BIIRULE-T15-R009" flag="fatal">[BIIRULE-T15-R009]-An invoice MUST have a tax total refering to a single tax scheme</assert>
    <assert test="$BIIRULE-T15-R010" flag="fatal">[BIIRULE-T15-R010]-Each tax total MUST equal the sum of the subcategory amounts.</assert>
    <assert test="$BIIRULE-T15-R047" flag="fatal">[BIIRULE-T15-R047]-A conformant CEN BII invoice core data model MUST specify the taxable amount per tax subtotal.</assert>
    <assert test="$BIIRULE-T15-R048" flag="fatal">[BIIRULE-T15-R048]-A conformant CEN BII invoice core data model MUST specify the tax amount per tax subtotal.</assert>
  </rule>
  <rule context="$Tax_Scheme">
    <assert test="$BIIRULE-T15-R050" flag="fatal">[BIIRULE-T15-R050]-Every tax scheme in CEN BII MUST be defined through an identifier.</assert>
  </rule>
  <rule context="$Tax_Category">
    <assert test="$BIIRULE-T15-R049" flag="fatal">[BIIRULE-T15-R049]-Every tax category in CEN BII MUST be defined through an identifier.</assert>
  </rule>
  <rule context="$Supplier">
    <assert test="$BIIRULE-T15-R002" flag="warning">[BIIRULE-T15-R002]-A supplier address in an invoice SHOULD contain at least the city name and a zip code or have an address identifier.</assert>
    <assert test="$BIIRULE-T15-R003" flag="warning">[BIIRULE-T15-R003]-If the supplier tax identifier is provided and if supplier and customer country codes are provided and are not equal then supplier tax identifier must be prefixed with the supplier country code.</assert>
  </rule>
  <rule context="$Payment_Means">
    <assert test="$BIIRULE-T15-R006" flag="warning">[BIIRULE-T15-R006]-Payment means due date in an invoice SHOULD be later or equal than issue date.</assert>
    <assert test="$BIIRULE-T15-R007" flag="warning">[BIIRULE-T15-R007]-If payment means is funds transfer, invoice MUST have a financial account </assert>
    <assert test="$BIIRULE-T15-R008" flag="warning">[BIIRULE-T15-R008]-If bank account is IBAN the BIC code SHOULD also be provided.</assert>
    <assert test="$BIIRULE-T15-R045" flag="fatal">[BIIRULE-T15-R045]-When specifying payment means, a conformant CEN BII invoice core data model MUST specify the payment coded.</assert>
  </rule>
  <rule context="$Party_Legal_Entity">
    <assert test="$BIIRULE-T15-R042" flag="fatal">[BIIRULE-T15-R042]-Company identifier MUST be specified when describing a company legal entity.</assert>
  </rule>
  <rule context="$Item_Price">
    <assert test="$BIIRULE-T15-R022" flag="fatal">[BIIRULE-T15-R022]-Prices of items MUST be positive or zero</assert>
  </rule>
  <rule context="$Item">
    <assert test="$BIIRULE-T15-R019" flag="warning">[BIIRULE-T15-R019]-Product names SHOULD NOT exceed 50 characters long</assert>
    <assert test="$BIIRULE-T15-R020" flag="warning">[BIIRULE-T15-R020]-If standard identifiers are provided within an item description, an Scheme Identifier SHOULD be provided (e.g. GTIN)</assert>
    <assert test="$BIIRULE-T15-R021" flag="warning">[BIIRULE-T15-R021]-Classification codes within an item description SHOULD have a List Identifier attribute (e.g. CPV or UNSPSC)</assert>
  </rule>
  <rule context="$Invoice_Period">
    <assert test="$BIIRULE-T15-R001" flag="warning">[BIIRULE-T15-R001]-An invoice period end date SHOULD be later or equal to an invoice period start date</assert>
  </rule>
  <rule context="$Invoice_Line">
    <assert test="$BIIRULE-T15-R018" flag="fatal">[BIIRULE-T15-R018]-Invoice line amount MUST be equal to the price amount multiplied by the quantity plus charges minus allowances at line level</assert>
    <assert test="$BIIRULE-T15-R026" flag="fatal">[BIIRULE-T15-R026]-Each invoice line MUST contain the product/service name</assert>
    <assert test="$BIIRULE-T15-R033" flag="fatal">[BIIRULE-T15-R033]-Invoice lines MUST have a line identifier.</assert>
    <assert test="$BIIRULE-T15-R051" flag="fatal">[BIIRULE-T15-R051]-Invoice lines MUST have a line total amount.</assert>
  </rule>
  <rule context="$Invoice">
    <assert test="$BIIRULE-T15-R023" flag="fatal">[BIIRULE-T15-R023]-A reference to the corrected invoice MUST be defined.</assert>
    <assert test="$BIIRULE-T15-R024" flag="fatal">[BIIRULE-T15-R024]-A conformant CEN BII invoice core data model MUST have the date of issue.</assert>
    <assert test="$BIIRULE-T15-R025" flag="fatal">[BIIRULE-T15-R025]-A conformant CEN BII invoice core data model MUST have an invoice number.</assert>
    <assert test="$BIIRULE-T15-R027" flag="fatal">[BIIRULE-T15-R027]-An invoice MUST contain the full name of the supplier.</assert>
    <assert test="$BIIRULE-T15-R028" flag="fatal">[BIIRULE-T15-R028]-An invoice MUST contain the full name of the customer.</assert>
    <assert test="$BIIRULE-T15-R029" flag="fatal">[BIIRULE-T15-R029]-If the VAT total amount in an invoice exists then the sum of taxable amount in sub categories MUST equal the sum of invoice tax exclusive amount.</assert>
    <assert test="$BIIRULE-T15-R030" flag="fatal">[BIIRULE-T15-R030]-A conformant CEN BII invoice core data model MUST have a syntax identifier.</assert>
    <assert test="$BIIRULE-T15-R031" flag="fatal">[BIIRULE-T15-R031]-A conformant CEN BII invoice core data model MUST have a customization identifier.</assert>
    <assert test="$BIIRULE-T15-R032" flag="fatal">[BIIRULE-T15-R032]-A conformant CEN BII invoice core data model MUST have a profile identifier.</assert>
    <assert test="$BIIRULE-T15-R034" flag="fatal">[BIIRULE-T15-R034]-A conformant CEN BII invoice core data model MUST specify at least one line item.</assert>
    <assert test="$BIIRULE-T15-R035" flag="fatal">[BIIRULE-T15-R035]-A conformant CEN BII invoice core data model MUST specify the currency code for the document.</assert>
    <assert test="$BIIRULE-T15-R036" flag="fatal">[BIIRULE-T15-R036]-If the invoice refers an order, a conformant CEN BII invoice core data model MUST specify the order identifier.</assert>
    <assert test="$BIIRULE-T15-R037" flag="fatal">[BIIRULE-T15-R037]-If the invoice refers a contract, a conformant CEN BII invoice core data model MUST specify the contract identifier.</assert>
    <assert test="$BIIRULE-T15-R039" flag="fatal">[BIIRULE-T15-R039]-A conformant CEN BII invoice core data model MUST specify the total payable amount.</assert>
    <assert test="$BIIRULE-T15-R040" flag="fatal">[BIIRULE-T15-R040]-A conformant CEN BII invoice core data model MUST specify the total amount with taxes included.</assert>
    <assert test="$BIIRULE-T15-R043" flag="fatal">[BIIRULE-T15-R043]-A conformant CEN BII invoice core data model MUST specify the total amount without taxes.</assert>
    <assert test="$BIIRULE-T15-R044" flag="fatal">[BIIRULE-T15-R044]-A conformant CEN BII invoice core data model MUST specify the sum of the line amounts.</assert>
    <assert test="$BIIRULE-T15-R046" flag="fatal">[BIIRULE-T15-R046]-A conformant CEN BII invoice core data model MUST specify the tax total amount.</assert>
  </rule>
  <rule context="$Customer">
    <assert test="$BIIRULE-T15-R004" flag="warning">[BIIRULE-T15-R004]-A customer address in an invoice SHOULD contain at least city and zip code or have an address identifier.</assert>
    <assert test="$BIIRULE-T15-R005" flag="warning">[BIIRULE-T15-R005]-If the customer tax identifier is provided and if supplier and customer country codes are provided and are not equal then customer tax identifier must be prefixed with the customer country code.</assert>
  </rule>
  <rule context="$Country">
    <assert test="$BIIRULE-T15-R041" flag="fatal">[BIIRULE-T15-R041]-Country in an address MUST be specified using the country code.</assert>
  </rule>
  <rule context="$Annex">
    <assert test="$BIIRULE-T15-R038" flag="fatal">[BIIRULE-T15-R038]-For any document referred in an invoice, a conformant CEN BII invoice core data model MUST specify the document identifier.</assert>
  </rule>
</pattern><?DSDL_INCLUDE_END abstract/BIIRULES-T15.sch?>
  <!-- Data Binding parameters -->
  <!-- ======================= -->
  <?DSDL_INCLUDE_START UBL/BIIRULES-UBL-T15.sch?><pattern id="UBL-T15" is-a="T15">
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
  <param value="(cbc:AllowanceTotalAmount) and cbc:AllowanceTotalAmount = (round(sum(/ubl:Invoice/cac:AllowanceCharge[cbc:ChargeIndicator=&#34;false&#34;]/cbc:Amount) * 100) div 100) or not(cbc:AllowanceTotalAmount)" name="BIIRULE-T15-R015"/>
  <param value="(cbc:ChargeTotalAmount) and cbc:ChargeTotalAmount = (round(sum(/ubl:Invoice/cac:AllowanceCharge[cbc:ChargeIndicator=&#34;true&#34;]/cbc:Amount) * 100) div 100) or not(cbc:ChargeTotalAmount)" name="BIIRULE-T15-R016"/>
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
</pattern><?DSDL_INCLUDE_END UBL/BIIRULES-UBL-T15.sch?>
  <!-- Code Lists Binding rules -->
  <!-- ======================== -->
  <?DSDL_INCLUDE_START codelist/BIIRULESCodesT15.sch?><pattern id="CodesT15">
<!--
  This implementation supports genericode code lists with no instance
  meta data.
-->
<!--
    Start of synthesis of rules from code list context associations.
Version 0.3
-->

<rule context="cbc:InvoiceTypeCode" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' 380 393 ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-015-001]-An Invoice MUST be tipified with the InvoiceTypeCode code list</assert>
</rule>

<rule context="cbc:DocumentCurrencyCode" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AED AFN ALL AMD ANG AOA ARS AUD AWG AZN BAM BBD BDT BGN BHD BIF BMD BND BOB BOV BRL BSD BTN BWP BYR BZD CAD CDF CHE CHF CHW CLF CLP CNY COP COU CRC CUP CVE CZK DJF DKK DOP DZD EEK EGP ERN ETB EUR FJD FKP GBP GEL GHS GIP GMD GNF GTQ GWP GYD HKD HNL HRK HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW KRW KWD KYD KZT LAK LBP LKR LRD LSL LTL LVL LYD MAD MDL MGA MKD MMK MNT MOP MRO MUR MVR MWK MXN MXV MYR MZN NAD NGN NIO NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR SDG SEK SGD SHP SKK SLL SOS SRD STD SVC SYP SZL THB TJS TMM TND TOP TRY TTD TWD TZS UAH UGX USD USN USS UYI UYU UZS VEF VND VUV WST XAF XAG XAU XBA XBB XBC XBD XCD XDR XFU XOF XPD XPF XTS XXX YER ZAR ZMK ZWR ZWD ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-015-002]-Currencies in an invoice MUST be coded using ISO currency code</assert>
</rule>

<rule context="@currencyID" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AED AFN ALL AMD ANG AOA ARS AUD AWG AZN BAM BBD BDT BGN BHD BIF BMD BND BOB BOV BRL BSD BTN BWP BYR BZD CAD CDF CHE CHF CHW CLF CLP CNY COP COU CRC CUP CVE CZK DJF DKK DOP DZD EEK EGP ERN ETB EUR FJD FKP GBP GEL GHS GIP GMD GNF GTQ GWP GYD HKD HNL HRK HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW KRW KWD KYD KZT LAK LBP LKR LRD LSL LTL LVL LYD MAD MDL MGA MKD MMK MNT MOP MRO MUR MVR MWK MXN MXV MYR MZN NAD NGN NIO NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR SDG SEK SGD SHP SKK SLL SOS SRD STD SVC SYP SZL THB TJS TMM TND TOP TRY TTD TWD TZS UAH UGX USD USN USS UYI UYU UZS VEF VND VUV WST XAF XAG XAU XBA XBB XBC XBD XCD XDR XFU XOF XPD XPF XTS XXX YER ZAR ZMK ZWR ZWD ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-015-003]-Currencies in an invoice MUST be coded using ISO currency code</assert>
</rule>

<rule context="cac:Country//cbc:IdentificationCode" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AD AE AF AG AI AL AM AN AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BL BJ BM BN BO BR BS BT BV BW BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CX CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR ST SV SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-015-004]-Country codes in an invoice MUST be coded using ISO code list 3166-1</assert>
</rule>

<rule context="cac:TaxScheme//cbc:ID" flag="warning">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AAA AAB AAC AAD AAE AAF AAG AAH AAI AAJ AAK AAL ADD BOL CAP CAR COC CST CUD CVD ENV EXC EXP FET FRE GCN GST ILL IMP IND LAC LCN LDP LOC LST MCA MCD OTH PDB PDC PRF SCN SSS STT SUP SUR SWT TAC TOT TOX TTA VAD VAT ',concat(' ',normalize-space(.),' ') ) ) )" flag="warning">[CL-015-005]-Invoice tax schemes MUST be coded using UN/ECE 5153 code list</assert>
</rule>

<rule context="cac:PaymentMeans//cbc:PaymentMeansCode" flag="warning">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 60 61 62 63 64 65 66 67 70 74 75 76 77 78 91 92 93 94 95 96 97 ',concat(' ',normalize-space(.),' ') ) ) )" flag="warning">[CL-015-006]-Payment means in an invoice MUST be coded using CEFACT code list 4461</assert>
</rule>

<rule context="cac:TaxCategory//cbc:ID" flag="warning">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' A AA AB AC AD AE B C E G H O S Z ',concat(' ',normalize-space(.),' ') ) ) )" flag="warning">[CL-015-007]-Invoice tax categories MUST be coded using UN/ECE 5305 code list</assert>
</rule>
<!--
    End of synthesis of rules from code list context associations.
-->
</pattern><?DSDL_INCLUDE_END codelist/BIIRULESCodesT15.sch?>
</schema>