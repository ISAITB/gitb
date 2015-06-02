<?xml version="1.0" encoding="utf-8"?><!-- 

        	UBL syntax binding to the T14   
        	Author: Oriol Bausà

     --><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:ubl="urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2" queryBinding="xslt2">
  <title>BIIRULES  T14 bound to UBL</title>
  <ns prefix="cbc" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"/>
  <ns prefix="cac" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"/>
  <ns prefix="ubl" uri="urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2"/>
  <phase id="BIIRULEST14_phase">
    <active pattern="UBL-T14"/>
  </phase>
  <phase id="codelist_phase">
    <active pattern="CodesT14"/>
  </phase>
  <!-- Abstract CEN BII patterns -->
  <!-- ========================= -->
  <?DSDL_INCLUDE_START abstract/BIIRULES-T14.sch?><pattern abstract="true" id="T14">
  <rule context="$Total_Amounts">
    <assert test="$BIIRULE-T14-R011" flag="fatal">[BIIRULE-T14-R011]-Credit note total line extension amount MUST equal the sum of the line totals</assert>
    <assert test="$BIIRULE-T14-R012" flag="fatal">[BIIRULE-T14-R012]-A credit note tax exclusive amount MUST equal the sum of lines plus allowances and charges on header level.</assert>
    <assert test="$BIIRULE-T14-R013" flag="fatal">[BIIRULE-T14-R013]-A credit note tax inclusive amount MUST equal the tax exclusive amount plus all tax total amounts and the rounding amount.</assert>
    <assert test="$BIIRULE-T14-R014" flag="fatal">[BIIRULE-T14-R014]-Tax inclusive amount in a credit note MUST NOT be negative</assert>
    <assert test="$BIIRULE-T14-R015" flag="fatal">[BIIRULE-T14-R015]-If there is a total allowance it MUST be equal to the sum of allowances at document level</assert>
    <assert test="$BIIRULE-T14-R016" flag="fatal">[BIIRULE-T14-R016]-If there is a total charges it MUST be equal to the sum of document level charges.</assert>
    <assert test="$BIIRULE-T14-R017" flag="fatal">[BIIRULE-T14-R017]-In a credit note, amount due is the tax inclusive amount minus what has been prepaid.</assert>
  </rule>
  <rule context="$Tax_Total">
    <assert test="$BIIRULE-T14-R009" flag="fatal">[BIIRULE-T14-R009]-A credit note MUST have a tax total refering to a single tax scheme</assert>
    <assert test="$BIIRULE-T14-R010" flag="fatal">[BIIRULE-T14-R010]-Each tax total MUST equal the sum of the subcategory amounts.</assert>
    <assert test="$BIIRULE-T14-R043" flag="fatal">[BIIRULE-T14-R043]-A conformant CEN BII Credit Note core data model MUST specify the taxable amount per tax subtotal.</assert>
    <assert test="$BIIRULE-T14-R044" flag="fatal">[BIIRULE-T14-R044]-A conformant CEN BII Credit Note core data model MUST specify the tax amount per tax subtotal.</assert>
  </rule>
  <rule context="$Tax_Scheme">
    <assert test="$BIIRULE-T14-R046" flag="fatal">[BIIRULE-T14-R046]-Every tax scheme in CEN BII MUST be defined through an identifier.</assert>
  </rule>
  <rule context="$Tax_Category">
    <assert test="$BIIRULE-T14-R045" flag="fatal">[BIIRULE-T14-R045]-Every tax category in CEN BII MUST be defined through an identifier.</assert>
  </rule>
  <rule context="$Supplier">
    <assert test="$BIIRULE-T14-R002" flag="warning">[BIIRULE-T14-R002]-A supplier address in a credit note SHOULD contain at least the city name and a zip code or have an address identifier.</assert>
    <assert test="$BIIRULE-T14-R003" flag="warning">[BIIRULE-T14-R003]-If the supplier tax identifier is provided and if supplier and customer country codes are provided and are not equal then supplier tax identifier must be prefixed with the supplier country code.</assert>
  </rule>
  <rule context="$Party_Legal_Entity">
    <assert test="$BIIRULE-T14-R039" flag="fatal">[BIIRULE-T14-R039]-Company identifier MUST be specified when describing a company legal entity.</assert>
  </rule>
  <rule context="$Item_Price">
    <assert test="$BIIRULE-T14-R022" flag="fatal">[BIIRULE-T14-R022]-Prices of items MUST be positive or zero</assert>
  </rule>
  <rule context="$Item">
    <assert test="$BIIRULE-T14-R019" flag="warning">[BIIRULE-T14-R019]-Product names SHOULD NOT exceed 50 characters long</assert>
    <assert test="$BIIRULE-T14-R020" flag="warning">[BIIRULE-T14-R020]-If standard identifiers are provided within an item description, an Scheme Identifier SHOULD be provided (e.g. GTIN)</assert>
    <assert test="$BIIRULE-T14-R021" flag="warning">[BIIRULE-T14-R021]-Classification codes within an item description SHOULD have a List Identifier attribute (e.g. CPV or UNSPSC)</assert>
  </rule>
  <rule context="$Invoice_Period">
    <assert test="$BIIRULE-T14-R001" flag="warning">[BIIRULE-T14-R001]-An invoice period end date SHOULD be later or equal to an invoice period start date</assert>
  </rule>
  <rule context="$Customer">
    <assert test="$BIIRULE-T14-R004" flag="warning">[BIIRULE-T14-R004]-A customer address in a credit note SHOULD contain at least city and zip code or have an address identifier.</assert>
    <assert test="$BIIRULE-T14-R005" flag="warning">[BIIRULE-T14-R005]-If the customer tax identifier is provided and if supplier and customer country codes are provided and are not equal then customer tax identifier must be prefixed with the customer country code.</assert>
  </rule>
  <rule context="$Credit_Note_Line">
    <assert test="$BIIRULE-T14-R027" flag="fatal">[BIIRULE-T14-R027]-Each credit note line MUST contain the product/service name</assert>
    <assert test="$BIIRULE-T14-R034" flag="fatal">[BIIRULE-T14-R034]-Credit note lines MUST have a line identifier.</assert>
    <assert test="$BIIRULE-T14-R050" flag="fatal">[BIIRULE-T14-R050]-Credit note lines MUST have a line total amount.</assert>
  </rule>
  <rule context="$Credit_Note">
    <assert test="$BIIRULE-T14-R025" flag="fatal">[BIIRULE-T14-R025]-A conformant CEN BII Credit Note core data model MUST have the date of issue.</assert>
    <assert test="$BIIRULE-T14-R026" flag="fatal">[BIIRULE-T14-R026]-A conformant CEN BII Credit Note core data model MUST have a Credit Note number.</assert>
    <assert test="$BIIRULE-T14-R028" flag="fatal">[BIIRULE-T14-R028]-An Credit Note MUST contain the full name of the supplier.</assert>
    <assert test="$BIIRULE-T14-R029" flag="fatal">[BIIRULE-T14-R029]-An Credit Note MUST contain the full name of the customer.</assert>
    <assert test="$BIIRULE-T14-R030" flag="fatal">[BIIRULE-T14-R030]-If the VAT total amount in a Credit Note exists then the sum of taxable amount in sub categories MUST equal the sum of Credit Note tax exclusive amount.</assert>
    <assert test="$BIIRULE-T14-R031" flag="fatal">[BIIRULE-T14-R031]-A conformant CEN BII Credit Note core data model MUST have a syntax identifier.</assert>
    <assert test="$BIIRULE-T14-R032" flag="fatal">[BIIRULE-T14-R032]-A conformant CEN BII Credit Note core data model MUST have a customization identifier.</assert>
    <assert test="$BIIRULE-T14-R033" flag="fatal">[BIIRULE-T14-R033]-A conformant CEN BII Credit Note core data model MUST have a profile identifier.</assert>
    <assert test="$BIIRULE-T14-R035" flag="fatal">[BIIRULE-T14-R035]-A conformant CEN BII Credit Note core data model MUST specify at least one line item.</assert>
    <assert test="$BIIRULE-T14-R036" flag="fatal">[BIIRULE-T14-R036]-A conformant CEN BII Credit Note core data model MUST specify the currency code for the document.</assert>
    <assert test="$BIIRULE-T14-R037" flag="fatal">[BIIRULE-T14-R037]-A conformant CEN BII Credit Note core data model MUST specify the total payable amount.</assert>
    <assert test="$BIIRULE-T14-R038" flag="fatal">[BIIRULE-T14-R038]-A conformant CEN BII Credit Note core data model MUST specify the total amount with taxes included.</assert>
    <assert test="$BIIRULE-T14-R040" flag="fatal">[BIIRULE-T14-R040]-A conformant CEN BII Credit Note core data model MUST specify the total amount without taxes.</assert>
    <assert test="$BIIRULE-T14-R041" flag="fatal">[BIIRULE-T14-R041]-A conformant CEN BII Credit Note core data model MUST specify the sum of the line amounts.</assert>
    <assert test="$BIIRULE-T14-R042" flag="fatal">[BIIRULE-T14-R042]-A conformant CEN BII Credit Note core data model MUST specify the tax total amount.</assert>
    <assert test="$BIIRULE-T14-R047" flag="fatal">[BIIRULE-T14-R047]-A conformant CEN BII Credit Note core data model MUST specify either or both of an invoice reference and a credit note reference.</assert>
  </rule>
  <rule context="$Allowance_Percentage">
    <assert test="$BIIRULE-T14-R023" flag="fatal">[BIIRULE-T14-R023]-An allowance percentage MUST NOT be negative.</assert>
  </rule>
  <rule context="$Allowance">
    <assert test="$BIIRULE-T14-R024" flag="warning">[BIIRULE-T14-R024]-In allowances, both or none of percentage and base amount SHOULD be provided</assert>
  </rule>
</pattern><?DSDL_INCLUDE_END abstract/BIIRULES-T14.sch?>
  <!-- Data Binding parameters -->
  <!-- ======================= -->
  <?DSDL_INCLUDE_START UBL/BIIRULES-UBL-T14.sch?><pattern id="UBL-T14" is-a="T14">
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
  <param value="(cbc:AllowanceTotalAmount) and cbc:AllowanceTotalAmount = (round(sum(preceding::cac:AllowanceCharge[cbc:ChargeIndicator=&#34;false&#34;]/cbc:Amount) * 100) div 100) or not(cbc:AllowanceTotalAmount)" name="BIIRULE-T14-R015"/>
  <param value="(cbc:ChargeTotalAmount) and cbc:ChargeTotalAmount = (round(sum(preceding::cac:AllowanceCharge[cbc:ChargeIndicator=&#34;true&#34;]/cbc:Amount) * 100) div 100) or not(cbc:ChargeTotalAmount)" name="BIIRULE-T14-R016"/>
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
</pattern><?DSDL_INCLUDE_END UBL/BIIRULES-UBL-T14.sch?>
  <!-- Code Lists Binding rules -->
  <!-- ======================== -->
  <?DSDL_INCLUDE_START codelist/BIIRULESCodesT14.sch?><pattern id="CodesT14">
<!--
  This implementation supports genericode code lists with no instance
  meta data.
-->
<!--
    Start of synthesis of rules from code list context associations.
Version 0.3
-->

<rule context="cbc:DocumentCurrencyCode" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AED AFN ALL AMD ANG AOA ARS AUD AWG AZN BAM BBD BDT BGN BHD BIF BMD BND BOB BOV BRL BSD BTN BWP BYR BZD CAD CDF CHE CHF CHW CLF CLP CNY COP COU CRC CUP CVE CZK DJF DKK DOP DZD EEK EGP ERN ETB EUR FJD FKP GBP GEL GHS GIP GMD GNF GTQ GWP GYD HKD HNL HRK HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW KRW KWD KYD KZT LAK LBP LKR LRD LSL LTL LVL LYD MAD MDL MGA MKD MMK MNT MOP MRO MUR MVR MWK MXN MXV MYR MZN NAD NGN NIO NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR SDG SEK SGD SHP SKK SLL SOS SRD STD SVC SYP SZL THB TJS TMM TND TOP TRY TTD TWD TZS UAH UGX USD USN USS UYI UYU UZS VEF VND VUV WST XAF XAG XAU XBA XBB XBC XBD XCD XDR XFU XOF XPD XPF XTS XXX YER ZAR ZMK ZWR ZWD ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-014-001]-Currencies in an credit note MUST be coded using ISO currency code</assert>
</rule>

<rule context="@currencyID" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AED AFN ALL AMD ANG AOA ARS AUD AWG AZN BAM BBD BDT BGN BHD BIF BMD BND BOB BOV BRL BSD BTN BWP BYR BZD CAD CDF CHE CHF CHW CLF CLP CNY COP COU CRC CUP CVE CZK DJF DKK DOP DZD EEK EGP ERN ETB EUR FJD FKP GBP GEL GHS GIP GMD GNF GTQ GWP GYD HKD HNL HRK HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW KRW KWD KYD KZT LAK LBP LKR LRD LSL LTL LVL LYD MAD MDL MGA MKD MMK MNT MOP MRO MUR MVR MWK MXN MXV MYR MZN NAD NGN NIO NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR SDG SEK SGD SHP SKK SLL SOS SRD STD SVC SYP SZL THB TJS TMM TND TOP TRY TTD TWD TZS UAH UGX USD USN USS UYI UYU UZS VEF VND VUV WST XAF XAG XAU XBA XBB XBC XBD XCD XDR XFU XOF XPD XPF XTS XXX YER ZAR ZMK ZWR ZWD ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-014-002]-Currencies in an credit note MUST be coded using ISO currency code</assert>
</rule>

<rule context="cac:Country//cbc:IdentificationCode" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AD AE AF AG AI AL AM AN AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BL BJ BM BN BO BR BS BT BV BW BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CX CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR ST SV SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-014-003]-Country codes in a credit note MUST be coded using ISO code list 3166-1</assert>
</rule>

<rule context="cac:TaxScheme//cbc:ID" flag="warning">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AAA AAB AAC AAD AAE AAF AAG AAH AAI AAJ AAK AAL ADD BOL CAP CAR COC CST CUD CVD ENV EXC EXP FET FRE GCN GST ILL IMP IND LAC LCN LDP LOC LST MCA MCD OTH PDB PDC PRF SCN SSS STT SUP SUR SWT TAC TOT TOX TTA VAD VAT ',concat(' ',normalize-space(.),' ') ) ) )" flag="warning">[CL-014-004]-Invoice tax schemes MUST be coded using UN/ECE 5153 code list</assert>
</rule>

<rule context="cac:TaxCategory//cbc:ID" flag="warning">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' A AA AB AC AD AE B C E G H O S Z ',concat(' ',normalize-space(.),' ') ) ) )" flag="warning">[CL-014-005]-Invoice tax categories MUST be coded using UN/ECE 5305 code list</assert>
</rule>
<!--
    End of synthesis of rules from code list context associations.
-->
</pattern><?DSDL_INCLUDE_END codelist/BIIRULESCodesT14.sch?>
</schema>