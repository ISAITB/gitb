<?xml version="1.0" encoding="utf-8"?><!-- 

        	FACTURAE syntax binding to the T10   
        	Author: Oriol Bausà

     --><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:ubl="http://www.facturae.es/Facturae/2009/v3.2/Facturae" queryBinding="xslt2">
  <title>BIIRULES  T10 bound to FACTURAE</title>
  <ns prefix="cbc" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"/>
  <ns prefix="cac" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"/>
  <ns prefix="ubl" uri="http://www.facturae.es/Facturae/2009/v3.2/Facturae"/>
  <phase id="BIIRULEST10_phase">
    <active pattern="UBL-T10"/>
  </phase>
  <phase id="codelist_phase">
    <active pattern="CodesT10"/>
  </phase>
  <!-- Abstract CEN BII patterns -->
  <!-- ========================= -->
  <?DSDL_INCLUDE_START abstract/BIIRULES-T10.sch?><pattern abstract="true" id="T10">
  <rule context="$Total_Amounts">
    <assert test="$BIIRULE-T10-R011" flag="fatal">[BIIRULE-T10-R011]-Invoice total line extension amount MUST equal the sum of the line totals</assert>
    <assert test="$BIIRULE-T10-R012" flag="fatal">[BIIRULE-T10-R012]-An invoice tax exclusive amount MUST equal the sum of lines plus allowances and charges on header level.</assert>
    <assert test="$BIIRULE-T10-R013" flag="fatal">[BIIRULE-T10-R013]-An invoice tax inclusive amount MUST equal the tax exclusive amount plus all tax total amounts and the rounding amount.</assert>
    <assert test="$BIIRULE-T10-R014" flag="fatal">[BIIRULE-T10-R014]-Tax inclusive amount in an invoice MUST NOT be negative</assert>
    <assert test="$BIIRULE-T10-R015" flag="fatal">[BIIRULE-T10-R015]-If there is a total allowance it MUST be equal to the sum of allowances at document level</assert>
    <assert test="$BIIRULE-T10-R016" flag="fatal">[BIIRULE-T10-R016]-If there is a total charges it MUST be equal to the sum of document level charges.</assert>
    <assert test="$BIIRULE-T10-R017" flag="fatal">[BIIRULE-T10-R017]-In an invoice, amount due is the tax inclusive amount minus what has been prepaid.</assert>
  </rule>
  <rule context="$Tax_Total">
    <assert test="$BIIRULE-T10-R009" flag="fatal">[BIIRULE-T10-R009]-An invoice MUST have a tax total refering to a single tax scheme</assert>
    <assert test="$BIIRULE-T10-R010" flag="fatal">[BIIRULE-T10-R010]-Each tax total MUST equal the sum of the subcategory amounts.</assert>
    <assert test="$BIIRULE-T10-R046" flag="fatal">[BIIRULE-T10-R046]-A conformant CEN BII invoice core data model MUST specify the taxable amount per tax subtotal.</assert>
    <assert test="$BIIRULE-T10-R047" flag="fatal">[BIIRULE-T10-R047]-A conformant CEN BII invoice core data model MUST specify the tax amount per tax subtotal.</assert>
  </rule>
  <rule context="$Tax_Scheme">
    <assert test="$BIIRULE-T10-R049" flag="fatal">[BIIRULE-T10-R049]-Every tax scheme in CEN BII MUST be defined through an identifier.</assert>
  </rule>
  <rule context="$Tax_Category">
    <assert test="$BIIRULE-T10-R048" flag="fatal">[BIIRULE-T10-R048]-Every tax category in CEN BII MUST be defined through an identifier.</assert>
  </rule>
  <rule context="$Supplier">
    <assert test="$BIIRULE-T10-R002" flag="warning">[BIIRULE-T10-R002]-A supplier address in an invoice SHOULD contain at least the city name and a zip code or have an address identifier.</assert>
    <assert test="$BIIRULE-T10-R003" flag="warning">[BIIRULE-T10-R003]-If the supplier tax identifier is provided and if supplier and customer country codes are provided and are not equal then supplier tax identifier must be prefixed with the supplier country code.</assert>
  </rule>
  <rule context="$Payment_Means">
    <assert test="$BIIRULE-T10-R006" flag="warning">[BIIRULE-T10-R006]-Payment means due date in an invoice SHOULD be later or equal than issue date.</assert>
    <assert test="$BIIRULE-T10-R007" flag="warning">[BIIRULE-T10-R007]-If payment means is funds transfer, invoice MUST have a financial account </assert>
    <assert test="$BIIRULE-T10-R008" flag="warning">[BIIRULE-T10-R008]-If bank account is IBAN the BIC code SHOULD also be provided.</assert>
    <assert test="$BIIRULE-T10-R044" flag="fatal">[BIIRULE-T10-R044]-When specifying payment means, a conformant CEN BII invoice core data model MUST specify the payment means coded.</assert>
  </rule>
  <rule context="$Party_Legal_Entity">
    <assert test="$BIIRULE-T10-R041" flag="fatal">[BIIRULE-T10-R041]-Company identifier MUST be specified when describing a company legal entity.</assert>
  </rule>
  <rule context="$Item_Price">
    <assert test="$BIIRULE-T10-R022" flag="fatal">[BIIRULE-T10-R022]-Prices of items MUST be positive or zero</assert>
  </rule>
  <rule context="$Item">
    <assert test="$BIIRULE-T10-R019" flag="warning">[BIIRULE-T10-R019]-Product names SHOULD NOT exceed 50 characters long</assert>
    <assert test="$BIIRULE-T10-R020" flag="warning">[BIIRULE-T10-R020]-If standard identifiers are provided within an item description, an Scheme Identifier SHOULD be provided (e.g. GTIN)</assert>
    <assert test="$BIIRULE-T10-R021" flag="warning">[BIIRULE-T10-R021]-Classification codes within an item description SHOULD have a List Identifier attribute (e.g. CPV or UNSPSC)</assert>
  </rule>
  <rule context="$Invoice_Period">
    <assert test="$BIIRULE-T10-R001" flag="warning">[BIIRULE-T10-R001]-An invoice period end date SHOULD be later or equal to an invoice period start date</assert>
  </rule>
  <rule context="$Invoice_Line">
    <assert test="$BIIRULE-T10-R018" flag="fatal">[BIIRULE-T10-R018]-Invoice line amount MUST be equal to the price amount multiplied by the quantity</assert>
    <assert test="$BIIRULE-T10-R025" flag="fatal">[BIIRULE-T10-R025]-Each invoice line MUST contain the product/service name</assert>
    <assert test="$BIIRULE-T10-R032" flag="fatal">[BIIRULE-T10-R032]-Invoice lines MUST have a line identifier.</assert>
  </rule>
  <rule context="$Invoice">
    <assert test="$BIIRULE-T10-R023" flag="fatal">[BIIRULE-T10-R023]-A conformant CEN BII invoice core data model MUST have the date of issue.</assert>
    <assert test="$BIIRULE-T10-R024" flag="fatal">[BIIRULE-T10-R024]-A conformant CEN BII invoice core data model MUST have an invoice number.</assert>
    <assert test="$BIIRULE-T10-R026" flag="fatal">[BIIRULE-T10-R026]-An invoice MUST contain the full name of the supplier.</assert>
    <assert test="$BIIRULE-T10-R027" flag="fatal">[BIIRULE-T10-R027]-An invoice MUST contain the full name of the customer.</assert>
    <assert test="$BIIRULE-T10-R028" flag="fatal">[BIIRULE-T10-R028]-If the VAT total amount in an invoice exists then the sum of taxable amount in sub categories MUST equal the sum of invoice tax exclusive amount.</assert>
    <assert test="$BIIRULE-T10-R029" flag="fatal">[BIIRULE-T10-R029]-A conformant CEN BII invoice core data model MUST have a syntax identifier.</assert>
    <assert test="$BIIRULE-T10-R030" flag="fatal">[BIIRULE-T10-R030]-A conformant CEN BII invoice core data model MUST have a customization identifier.</assert>
    <assert test="$BIIRULE-T10-R031" flag="fatal">[BIIRULE-T10-R031]-A conformant CEN BII invoice core data model MUST have a profile identifier.</assert>
    <assert test="$BIIRULE-T10-R033" flag="fatal">[BIIRULE-T10-R033]-A conformant CEN BII invoice core data model MUST specify at least one line item.</assert>
    <assert test="$BIIRULE-T10-R034" flag="fatal">[BIIRULE-T10-R034]-A conformant CEN BII invoice core data model MUST specify the currency code for the document.</assert>
    <assert test="$BIIRULE-T10-R035" flag="fatal">[BIIRULE-T10-R035]-If the invoice refers an order, a conformant CEN BII invoice core data model MUST specify the order identifier.</assert>
    <assert test="$BIIRULE-T10-R036" flag="fatal">[BIIRULE-T10-R036]-If the invoice refers a contract, a conformant CEN BII invoice core data model MUST specify the contract identifier.</assert>
    <assert test="$BIIRULE-T10-R038" flag="fatal">[BIIRULE-T10-R038]-A conformant CEN BII invoice core data model MUST specify the total payable amount.</assert>
    <assert test="$BIIRULE-T10-R039" flag="fatal">[BIIRULE-T10-R039]-A conformant CEN BII invoice core data model MUST specify the total amount with taxes included.</assert>
    <assert test="$BIIRULE-T10-R042" flag="fatal">[BIIRULE-T10-R042]-A conformant CEN BII invoice core data model MUST specify the total amount without taxes.</assert>
    <assert test="$BIIRULE-T10-R043" flag="fatal">[BIIRULE-T10-R043]-A conformant CEN BII invoice core data model MUST specify the sum of the line amounts.</assert>
    <assert test="$BIIRULE-T10-R045" flag="fatal">[BIIRULE-T10-R045]-A conformant CEN BII invoice core data model MUST specify the tax total amount.</assert>
  </rule>
  <rule context="$Customer">
    <assert test="$BIIRULE-T10-R004" flag="warning">[BIIRULE-T10-R004]-A customer address in an invoice SHOULD contain at least city and zip code or have an address identifier.</assert>
    <assert test="$BIIRULE-T10-R005" flag="warning">[BIIRULE-T10-R005]-If the customer tax identifier is provided and if supplier and customer country codes are provided and are not equal then customer tax identifier must be prefixed with the customer country code.</assert>
  </rule>
  <rule context="$Country">
    <assert test="$BIIRULE-T10-R040" flag="fatal">[BIIRULE-T10-R040]-Country in an address MUST be specified using the country code.</assert>
  </rule>
  <rule context="$Annex">
    <assert test="$BIIRULE-T10-R037" flag="fatal">[BIIRULE-T10-R037]-For any document referred in an invoice, a conformant CEN BII invoice core data model MUST specify the document identifier.</assert>
  </rule>
</pattern><?DSDL_INCLUDE_END abstract/BIIRULES-T10.sch?>
  <!-- Data Binding parameters -->
  <!-- ======================= -->
  <?DSDL_INCLUDE_START Facturae/BIIRULES-Facturae-T10.sch?><pattern id="UBL-T10" is-a="T10">
  <param value="(EndDate and  StartDate) and not(number(translate(StartDate,'-','')) &gt; number(translate(EndDate,'-',''))) or number(translate(StartDate,'-','')) = number(translate(EndDate,'-',''))" name="BIIRULE-T10-R001"/>
  <param value="((LegalEntity/AddressInSpain/Town and LegalEntity/AddressInSpain/PostCode) or (LegalEntity/OverseasAddress/PostCodeandTown) or (Individual/AddressInSpain/Town and Individual/AddressInSpain/PostCode) or (Individual/OverseasAddress/PostCodeandTown))" name="BIIRULE-T10-R002"/>
  <param value="((//CountryCode) and (following::BuyerParty//CountryCode) and ((//CountryCode) = (following::BuyerParty//CountryCode) or ((//CountryCode) != (following::BuyerParty//CountryCode) and starts-with(TaxIdentification/TaxIdentificationNumber, //CountryCode)))) or not(//CountryCode) or not(following::BuyerParty//CountryCode)" name="BIIRULE-T10-R003"/>
  <param value="((LegalEntity/AddressInSpain/Town and LegalEntity/AddressInSpain/PostCode) or (LegalEntity/OverseasAddress/PostCodeandTown) or (Individual/AddressInSpain/Town and Individual/AddressInSpain/PostCode) or (Individual/OverseasAddress/PostCodeandTown))" name="BIIRULE-T10-R004"/>
  <param value="((//CountryCode) and (preceding::SellerParty//CountryCode) and  ((//CountryCode) = (preceding::SellerParty//CountryCode) or ((//CountryCode) != (preceding::SellerParty//CountryCode) and starts-with(TaxIdentification/TaxIdentificationNumber, //CountryCode)))) or not((//CountryCode)) or not((preceding::SellerParty//CountryCode))" name="BIIRULE-T10-R005"/>
  <param value="(InstallmentDueDate and preceding::InvoiceIssueData/IssueDate) and not(number(translate(InstallmentDueDate,'-','')) &lt; number(translate(preceding::InvoiceIssueData/IssueDate,'-',''))) or number(translate(InstallmentDueDate,'-','')) = number(translate(preceding::InvoiceIssueData/IssueDate,'-',''))" name="BIIRULE-T10-R006"/>
  <param value="((PaymentMeans = '31') and (//AccountToBeCredited/IBAN or //AccountToBeCredited/AccountNumber)) or (PaymentMeans != '31')" name="BIIRULE-T10-R007"/>
  <param value="(//AccountToBeCredited/IBAN and //AccountToBeCredited/BIC) or not(//AccountToBeCredited/IBAN)" name="BIIRULE-T10-R008"/>
  <param value="count(Tax)&gt;1 or count(Tax) = 1" name="BIIRULE-T10-R009"/>
  <param value="number(following::InvoiceTotals/TotalTaxOutputs) = number(round(sum(child::Tax/TaxAmount/TotalAmount) * 100) div 100)" name="BIIRULE-T10-R010"/>
  <param value="number(TotalGrossAmount) = number(round(sum(following::Items/InvoiceLine/GrossAmount) * 100) div 100)" name="BIIRULE-T10-R011"/>
  <param value="((TotalGeneralSurcharges) and (TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = (number(TotalGrossAmount) + number(TotalGeneralSurcharges) - number(TotalGeneralDiscounts)))) or (not(TotalGeneralSurcharges) and (TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = number(TotalGrossAmount) - number(TotalGeneralDiscounts))) or ((TotalGeneralSurcharges) and not(TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = number(TotalGrossAmount) + number(TotalGeneralSurcharges))) or (not(TotalGeneralSurcharges) and not(TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = number(TotalGrossAmount)))" name="BIIRULE-T10-R012"/>
  <param value="number(InvoiceTotal) = number(TotalGrossAmountBeforeTaxes) - number(TotalTaxesWithheld) + number(TotalTaxOutputs)" name="BIIRULE-T10-R013"/>
  <param value="number(InvoiceTotal) &gt;= 0" name="BIIRULE-T10-R014"/>
  <param value="(TotalGeneralDiscounts) and TotalGeneralDiscounts = (round(sum(//Discount/DiscountAmount) * 100) div 100) or not(TotalGeneralDiscounts)" name="BIIRULE-T10-R015"/>
  <param value="(TotalGeneralSurcharges) and TotalGeneralSurcharges = (round(sum(//Charge/ChargeAmount) * 100) div 100) or not(TotalGeneralSurcharges)" name="BIIRULE-T10-R016"/>
  <param value="(TotalPaymentsOnAccount) and (number(TotalExecutableAmount) = number(InvoiceTotal - TotalPaymentsOnAccount)) or TotalExecutableAmount = InvoiceTotal" name="BIIRULE-T10-R017"/>
  <param value="not(Quantity) or not(UnitPriceWithoutTax) or number(GrossAmount) = (round(number(UnitPriceWithoutTax) * number(Quantity) * 100) div 100)" name="BIIRULE-T10-R018"/>
  <param value="string-length(string(//ItemDescription)) &lt;= 50" name="BIIRULE-T10-R019"/>
  <param value="true=false" name="BIIRULE-T10-R020"/>
  <param value="true=false" name="BIIRULE-T10-R021"/>
  <param value="number(.) &gt;=0" name="BIIRULE-T10-R022"/>
  <param value="(//InvoiceIssueData/IssueDate)" name="BIIRULE-T10-R023"/>
  <param value="(//InvoiceHeader/InvoiceNumber)" name="BIIRULE-T10-R024"/>
  <param value="(//ItemDescription)" name="BIIRULE-T10-R025"/>
  <param value="(//SellerParty/LegalEntity/CorporateName) or (//SellerParty/Individual/Name) " name="BIIRULE-T10-R026"/>
  <param value="(//BuyerParty/LegalEntity/CorporateName) or (//BuyerParty/Individual/Name) " name="BIIRULE-T10-R027"/>
  <param value="true=false" name="BIIRULE-T10-R028"/>
  <param value="//SchemaVersion" name="BIIRULE-T10-R029"/>
  <param value="true=false" name="BIIRULE-T10-R030"/>
  <param value="true=false" name="BIIRULE-T10-R031"/>
  <param value="//InvoiceHeader/InvoiceNumber" name="BIIRULE-T10-R032"/>
  <param value="(//Items/InvoiceLine)" name="BIIRULE-T10-R033"/>
  <param value="(//InvoiceIssueData/InvoiceCurrencyCode)" name="BIIRULE-T10-R034"/>
  <param value="true=false" name="BIIRULE-T10-R035"/>
  <param value="true=false" name="BIIRULE-T10-R036"/>
  <param value="true=false" name="BIIRULE-T10-R037"/>
  <param value="//TotalExecutableAmount" name="BIIRULE-T10-R038"/>
  <param value="//InvoiceTotal" name="BIIRULE-T10-R039"/>
  <param value="." name="BIIRULE-T10-R040"/>
  <param value="//RegistrationData" name="BIIRULE-T10-R041"/>
  <param value="//TotalGrossAmountBeforeTaxes" name="BIIRULE-T10-R042"/>
  <param value="//TotalGrossAmount" name="BIIRULE-T10-R043"/>
  <param value="//PaymentMeans" name="BIIRULE-T10-R044"/>
  <param value="//TotalTaxOutputs" name="BIIRULE-T10-R045"/>
  <param value="//Tax/TaxableBase" name="BIIRULE-T10-R046"/>
  <param value="//Tax/TaxAmount" name="BIIRULE-T10-R047"/>
  <param value="false" name="BIIRULE-T10-R048"/>
  <param value="//TaxTypeCode" name="BIIRULE-T10-R049"/>
  <param value="//Parties/BuyerParty" name="Customer"/>
  <param value="//Items/InvoiceLine" name="Invoice_Line"/>
  <param value="//InvoicingPeriod" name="Invoice_Period"/>
  <param value="/ubl:Facturae" name="Invoice"/>
  <param value="//Items/InvoiceLine/UnitPriceWithoutTax" name="Item_Price"/>
  <param value="//Items/InvoiceLine" name="Item"/>
  <param value="//PaymentDetails/Installments" name="Payment_Means"/>
  <param value="//Parties/SellerParty" name="Supplier"/>
  <param value="/ubl:Facturae/Invoices/Invoice/TaxesOutputs" name="Tax_Total"/>
  <param value="//InvoiceTotals" name="Total_Amounts"/>
  <param value="//LegalEntity" name="Party_Legal_Entity"/>
  <param value="//TaxesOutputs" name="Tax_Category"/>
  <param value="//TaxesOutputs" name="Tax_Scheme"/>
  <param value="//cac:AdditionalDocumentReference" name="Annex"/>
  <param value="//CountryCode" name="Country"/>
</pattern><?DSDL_INCLUDE_END Facturae/BIIRULES-Facturae-T10.sch?>
  <!-- Code Lists Binding rules -->
  <!-- ======================== -->
  <?DSDL_INCLUDE_START codelist/BIIRULESCodesT10.sch?><pattern id="CodesT10">
<!--
  This implementation supports genericode code lists with no instance
  meta data.
-->
<!--
    Start of synthesis of rules from code list context associations.
Version 0.3
-->

<rule context="cbc:InvoiceTypeCode" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' 380 393 ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-010-001]-An Invoice MUST be tipified with the InvoiceTypeCode code list</assert>
</rule>

<rule context="cbc:DocumentCurrencyCode" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AED AFN ALL AMD ANG AOA ARS AUD AWG AZN BAM BBD BDT BGN BHD BIF BMD BND BOB BOV BRL BSD BTN BWP BYR BZD CAD CDF CHE CHF CHW CLF CLP CNY COP COU CRC CUP CVE CZK DJF DKK DOP DZD EEK EGP ERN ETB EUR FJD FKP GBP GEL GHS GIP GMD GNF GTQ GWP GYD HKD HNL HRK HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW KRW KWD KYD KZT LAK LBP LKR LRD LSL LTL LVL LYD MAD MDL MGA MKD MMK MNT MOP MRO MUR MVR MWK MXN MXV MYR MZN NAD NGN NIO NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR SDG SEK SGD SHP SKK SLL SOS SRD STD SVC SYP SZL THB TJS TMM TND TOP TRY TTD TWD TZS UAH UGX USD USN USS UYI UYU UZS VEF VND VUV WST XAF XAG XAU XBA XBB XBC XBD XCD XDR XFU XOF XPD XPF XTS XXX YER ZAR ZMK ZWR ZWD ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-010-002]-Currencies in an invoice MUST be coded using ISO currency code</assert>
</rule>

<rule context="@currencyID" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AED AFN ALL AMD ANG AOA ARS AUD AWG AZN BAM BBD BDT BGN BHD BIF BMD BND BOB BOV BRL BSD BTN BWP BYR BZD CAD CDF CHE CHF CHW CLF CLP CNY COP COU CRC CUP CVE CZK DJF DKK DOP DZD EEK EGP ERN ETB EUR FJD FKP GBP GEL GHS GIP GMD GNF GTQ GWP GYD HKD HNL HRK HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW KRW KWD KYD KZT LAK LBP LKR LRD LSL LTL LVL LYD MAD MDL MGA MKD MMK MNT MOP MRO MUR MVR MWK MXN MXV MYR MZN NAD NGN NIO NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR SDG SEK SGD SHP SKK SLL SOS SRD STD SVC SYP SZL THB TJS TMM TND TOP TRY TTD TWD TZS UAH UGX USD USN USS UYI UYU UZS VEF VND VUV WST XAF XAG XAU XBA XBB XBC XBD XCD XDR XFU XOF XPD XPF XTS XXX YER ZAR ZMK ZWR ZWD ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-010-003]-Currencies in an invoice MUST be coded using ISO currency code</assert>
</rule>

<rule context="cac:Country//cbc:IdentificationCode" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AD AE AF AG AI AL AM AN AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BL BJ BM BN BO BR BS BT BV BW BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CX CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR ST SV SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-010-004]-Country codes in an invoice MUST be coded using ISO code list 3166-1</assert>
</rule>

<rule context="cac:TaxScheme//cbc:ID" flag="warning">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AAA AAB AAC AAD AAE AAF AAG AAH AAI AAJ AAK AAL ADD BOL CAP CAR COC CST CUD CVD ENV EXC EXP FET FRE GCN GST ILL IMP IND LAC LCN LDP LOC LST MCA MCD OTH PDB PDC PRF SCN SSS STT SUP SUR SWT TAC TOT TOX TTA VAD VAT ',concat(' ',normalize-space(.),' ') ) ) )" flag="warning">[CL-010-005]-Invoice tax schemes MUST be coded using UN/ECE 5153 code list</assert>
</rule>

<rule context="cac:PaymentMeans//cbc:PaymentMeansCode" flag="warning">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 60 61 62 63 64 65 66 67 70 74 75 76 77 78 91 92 93 94 95 96 97 ',concat(' ',normalize-space(.),' ') ) ) )" flag="warning">[CL-010-006]-Payment means in an invoice MUST be coded using CEFACT code list 4461</assert>
</rule>

<rule context="cac:TaxCategory//cbc:ID" flag="warning">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' A AA AB AC AD AE B C E G H O S Z ',concat(' ',normalize-space(.),' ') ) ) )" flag="warning">[CL-010-007]-Invoice tax categories MUST be coded using UN/ECE 5305 code list</assert>
</rule>
<!--
    End of synthesis of rules from code list context associations.
-->
</pattern><?DSDL_INCLUDE_END codelist/BIIRULESCodesT10.sch?>
</schema>