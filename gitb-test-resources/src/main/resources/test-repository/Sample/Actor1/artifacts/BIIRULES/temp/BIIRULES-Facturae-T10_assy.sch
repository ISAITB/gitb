<?xml version="1.0" encoding="utf-8"?><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:ubl="http://www.facturae.es/Facturae/2009/v3.2/Facturae" queryBinding="xslt2">
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
  
  
  <!--Suppressed abstract pattern T10 was here-->
  
  
  <!--Start pattern based on abstract T10--><pattern id="UBL-T10">
  <rule context="//InvoiceTotals">
    <assert test="number(TotalGrossAmount) = number(round(sum(following::Items/InvoiceLine/GrossAmount) * 100) div 100)" flag="fatal">[BIIRULE-T10-R011]-Invoice total line extension amount MUST equal the sum of the line totals</assert>
    <assert test="((TotalGeneralSurcharges) and (TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = (number(TotalGrossAmount) + number(TotalGeneralSurcharges) - number(TotalGeneralDiscounts)))) or (not(TotalGeneralSurcharges) and (TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = number(TotalGrossAmount) - number(TotalGeneralDiscounts))) or ((TotalGeneralSurcharges) and not(TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = number(TotalGrossAmount) + number(TotalGeneralSurcharges))) or (not(TotalGeneralSurcharges) and not(TotalGeneralDiscounts) and (number(TotalGrossAmountBeforeTaxes) = number(TotalGrossAmount)))" flag="fatal">[BIIRULE-T10-R012]-An invoice tax exclusive amount MUST equal the sum of lines plus allowances and charges on header level.</assert>
    <assert test="number(InvoiceTotal) = number(TotalGrossAmountBeforeTaxes) - number(TotalTaxesWithheld) + number(TotalTaxOutputs)" flag="fatal">[BIIRULE-T10-R013]-An invoice tax inclusive amount MUST equal the tax exclusive amount plus all tax total amounts and the rounding amount.</assert>
    <assert test="number(InvoiceTotal) &gt;= 0" flag="fatal">[BIIRULE-T10-R014]-Tax inclusive amount in an invoice MUST NOT be negative</assert>
    <assert test="(TotalGeneralDiscounts) and TotalGeneralDiscounts = (round(sum(//Discount/DiscountAmount) * 100) div 100) or not(TotalGeneralDiscounts)" flag="fatal">[BIIRULE-T10-R015]-If there is a total allowance it MUST be equal to the sum of allowances at document level</assert>
    <assert test="(TotalGeneralSurcharges) and TotalGeneralSurcharges = (round(sum(//Charge/ChargeAmount) * 100) div 100) or not(TotalGeneralSurcharges)" flag="fatal">[BIIRULE-T10-R016]-If there is a total charges it MUST be equal to the sum of document level charges.</assert>
    <assert test="(TotalPaymentsOnAccount) and (number(TotalExecutableAmount) = number(InvoiceTotal - TotalPaymentsOnAccount)) or TotalExecutableAmount = InvoiceTotal" flag="fatal">[BIIRULE-T10-R017]-In an invoice, amount due is the tax inclusive amount minus what has been prepaid.</assert>
  </rule>
  <rule context="/ubl:Facturae/Invoices/Invoice/TaxesOutputs">
    <assert test="count(Tax)&gt;1 or count(Tax) = 1" flag="fatal">[BIIRULE-T10-R009]-An invoice MUST have a tax total refering to a single tax scheme</assert>
    <assert test="number(following::InvoiceTotals/TotalTaxOutputs) = number(round(sum(child::Tax/TaxAmount/TotalAmount) * 100) div 100)" flag="fatal">[BIIRULE-T10-R010]-Each tax total MUST equal the sum of the subcategory amounts.</assert>
    <assert test="//Tax/TaxableBase" flag="fatal">[BIIRULE-T10-R046]-A conformant CEN BII invoice core data model MUST specify the taxable amount per tax subtotal.</assert>
    <assert test="//Tax/TaxAmount" flag="fatal">[BIIRULE-T10-R047]-A conformant CEN BII invoice core data model MUST specify the tax amount per tax subtotal.</assert>
  </rule>
  <rule context="//TaxesOutputs">
    <assert test="//TaxTypeCode" flag="fatal">[BIIRULE-T10-R049]-Every tax scheme in CEN BII MUST be defined through an identifier.</assert>
  </rule>
  <rule context="//TaxesOutputs">
    <assert test="false" flag="fatal">[BIIRULE-T10-R048]-Every tax category in CEN BII MUST be defined through an identifier.</assert>
  </rule>
  <rule context="//Parties/SellerParty">
    <assert test="((LegalEntity/AddressInSpain/Town and LegalEntity/AddressInSpain/PostCode) or (LegalEntity/OverseasAddress/PostCodeandTown) or (Individual/AddressInSpain/Town and Individual/AddressInSpain/PostCode) or (Individual/OverseasAddress/PostCodeandTown))" flag="warning">[BIIRULE-T10-R002]-A supplier address in an invoice SHOULD contain at least the city name and a zip code or have an address identifier.</assert>
    <assert test="((//CountryCode) and (following::BuyerParty//CountryCode) and ((//CountryCode) = (following::BuyerParty//CountryCode) or ((//CountryCode) != (following::BuyerParty//CountryCode) and starts-with(TaxIdentification/TaxIdentificationNumber, //CountryCode)))) or not(//CountryCode) or not(following::BuyerParty//CountryCode)" flag="warning">[BIIRULE-T10-R003]-If the supplier tax identifier is provided and if supplier and customer country codes are provided and are not equal then supplier tax identifier must be prefixed with the supplier country code.</assert>
  </rule>
  <rule context="//PaymentDetails/Installments">
    <assert test="(InstallmentDueDate and preceding::InvoiceIssueData/IssueDate) and not(number(translate(InstallmentDueDate,'-','')) &lt; number(translate(preceding::InvoiceIssueData/IssueDate,'-',''))) or number(translate(InstallmentDueDate,'-','')) = number(translate(preceding::InvoiceIssueData/IssueDate,'-',''))" flag="warning">[BIIRULE-T10-R006]-Payment means due date in an invoice SHOULD be later or equal than issue date.</assert>
    <assert test="((PaymentMeans = '31') and (//AccountToBeCredited/IBAN or //AccountToBeCredited/AccountNumber)) or (PaymentMeans != '31')" flag="warning">[BIIRULE-T10-R007]-If payment means is funds transfer, invoice MUST have a financial account </assert>
    <assert test="(//AccountToBeCredited/IBAN and //AccountToBeCredited/BIC) or not(//AccountToBeCredited/IBAN)" flag="warning">[BIIRULE-T10-R008]-If bank account is IBAN the BIC code SHOULD also be provided.</assert>
    <assert test="//PaymentMeans" flag="fatal">[BIIRULE-T10-R044]-When specifying payment means, a conformant CEN BII invoice core data model MUST specify the payment means coded.</assert>
  </rule>
  <rule context="//LegalEntity">
    <assert test="//RegistrationData" flag="fatal">[BIIRULE-T10-R041]-Company identifier MUST be specified when describing a company legal entity.</assert>
  </rule>
  <rule context="//Items/InvoiceLine/UnitPriceWithoutTax">
    <assert test="number(.) &gt;=0" flag="fatal">[BIIRULE-T10-R022]-Prices of items MUST be positive or zero</assert>
  </rule>
  <rule context="//Items/InvoiceLine">
    <assert test="string-length(string(//ItemDescription)) &lt;= 50" flag="warning">[BIIRULE-T10-R019]-Product names SHOULD NOT exceed 50 characters long</assert>
    <assert test="true=false" flag="warning">[BIIRULE-T10-R020]-If standard identifiers are provided within an item description, an Scheme Identifier SHOULD be provided (e.g. GTIN)</assert>
    <assert test="true=false" flag="warning">[BIIRULE-T10-R021]-Classification codes within an item description SHOULD have a List Identifier attribute (e.g. CPV or UNSPSC)</assert>
  </rule>
  <rule context="//InvoicingPeriod">
    <assert test="(EndDate and  StartDate) and not(number(translate(StartDate,'-','')) &gt; number(translate(EndDate,'-',''))) or number(translate(StartDate,'-','')) = number(translate(EndDate,'-',''))" flag="warning">[BIIRULE-T10-R001]-An invoice period end date SHOULD be later or equal to an invoice period start date</assert>
  </rule>
  <rule context="//Items/InvoiceLine">
    <assert test="not(Quantity) or not(UnitPriceWithoutTax) or number(GrossAmount) = (round(number(UnitPriceWithoutTax) * number(Quantity) * 100) div 100)" flag="fatal">[BIIRULE-T10-R018]-Invoice line amount MUST be equal to the price amount multiplied by the quantity</assert>
    <assert test="(//ItemDescription)" flag="fatal">[BIIRULE-T10-R025]-Each invoice line MUST contain the product/service name</assert>
    <assert test="//InvoiceHeader/InvoiceNumber" flag="fatal">[BIIRULE-T10-R032]-Invoice lines MUST have a line identifier.</assert>
  </rule>
  <rule context="/ubl:Facturae">
    <assert test="(//InvoiceIssueData/IssueDate)" flag="fatal">[BIIRULE-T10-R023]-A conformant CEN BII invoice core data model MUST have the date of issue.</assert>
    <assert test="(//InvoiceHeader/InvoiceNumber)" flag="fatal">[BIIRULE-T10-R024]-A conformant CEN BII invoice core data model MUST have an invoice number.</assert>
    <assert test="(//SellerParty/LegalEntity/CorporateName) or (//SellerParty/Individual/Name) " flag="fatal">[BIIRULE-T10-R026]-An invoice MUST contain the full name of the supplier.</assert>
    <assert test="(//BuyerParty/LegalEntity/CorporateName) or (//BuyerParty/Individual/Name) " flag="fatal">[BIIRULE-T10-R027]-An invoice MUST contain the full name of the customer.</assert>
    <assert test="true=false" flag="fatal">[BIIRULE-T10-R028]-If the VAT total amount in an invoice exists then the sum of taxable amount in sub categories MUST equal the sum of invoice tax exclusive amount.</assert>
    <assert test="//SchemaVersion" flag="fatal">[BIIRULE-T10-R029]-A conformant CEN BII invoice core data model MUST have a syntax identifier.</assert>
    <assert test="true=false" flag="fatal">[BIIRULE-T10-R030]-A conformant CEN BII invoice core data model MUST have a customization identifier.</assert>
    <assert test="true=false" flag="fatal">[BIIRULE-T10-R031]-A conformant CEN BII invoice core data model MUST have a profile identifier.</assert>
    <assert test="(//Items/InvoiceLine)" flag="fatal">[BIIRULE-T10-R033]-A conformant CEN BII invoice core data model MUST specify at least one line item.</assert>
    <assert test="(//InvoiceIssueData/InvoiceCurrencyCode)" flag="fatal">[BIIRULE-T10-R034]-A conformant CEN BII invoice core data model MUST specify the currency code for the document.</assert>
    <assert test="true=false" flag="fatal">[BIIRULE-T10-R035]-If the invoice refers an order, a conformant CEN BII invoice core data model MUST specify the order identifier.</assert>
    <assert test="true=false" flag="fatal">[BIIRULE-T10-R036]-If the invoice refers a contract, a conformant CEN BII invoice core data model MUST specify the contract identifier.</assert>
    <assert test="//TotalExecutableAmount" flag="fatal">[BIIRULE-T10-R038]-A conformant CEN BII invoice core data model MUST specify the total payable amount.</assert>
    <assert test="//InvoiceTotal" flag="fatal">[BIIRULE-T10-R039]-A conformant CEN BII invoice core data model MUST specify the total amount with taxes included.</assert>
    <assert test="//TotalGrossAmountBeforeTaxes" flag="fatal">[BIIRULE-T10-R042]-A conformant CEN BII invoice core data model MUST specify the total amount without taxes.</assert>
    <assert test="//TotalGrossAmount" flag="fatal">[BIIRULE-T10-R043]-A conformant CEN BII invoice core data model MUST specify the sum of the line amounts.</assert>
    <assert test="//TotalTaxOutputs" flag="fatal">[BIIRULE-T10-R045]-A conformant CEN BII invoice core data model MUST specify the tax total amount.</assert>
  </rule>
  <rule context="//Parties/BuyerParty">
    <assert test="((LegalEntity/AddressInSpain/Town and LegalEntity/AddressInSpain/PostCode) or (LegalEntity/OverseasAddress/PostCodeandTown) or (Individual/AddressInSpain/Town and Individual/AddressInSpain/PostCode) or (Individual/OverseasAddress/PostCodeandTown))" flag="warning">[BIIRULE-T10-R004]-A customer address in an invoice SHOULD contain at least city and zip code or have an address identifier.</assert>
    <assert test="((//CountryCode) and (preceding::SellerParty//CountryCode) and  ((//CountryCode) = (preceding::SellerParty//CountryCode) or ((//CountryCode) != (preceding::SellerParty//CountryCode) and starts-with(TaxIdentification/TaxIdentificationNumber, //CountryCode)))) or not((//CountryCode)) or not((preceding::SellerParty//CountryCode))" flag="warning">[BIIRULE-T10-R005]-If the customer tax identifier is provided and if supplier and customer country codes are provided and are not equal then customer tax identifier must be prefixed with the customer country code.</assert>
  </rule>
  <rule context="//CountryCode">
    <assert test="." flag="fatal">[BIIRULE-T10-R040]-Country in an address MUST be specified using the country code.</assert>
  </rule>
  <rule context="//cac:AdditionalDocumentReference">
    <assert test="true=false" flag="fatal">[BIIRULE-T10-R037]-For any document referred in an invoice, a conformant CEN BII invoice core data model MUST specify the document identifier.</assert>
  </rule>
</pattern>
  
  
  <pattern id="CodesT10">



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

</pattern>
</schema>