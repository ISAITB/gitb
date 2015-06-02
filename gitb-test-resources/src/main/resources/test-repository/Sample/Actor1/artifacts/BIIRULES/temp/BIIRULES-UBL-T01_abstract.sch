<?xml version="1.0" encoding="utf-8"?><!-- 

        	UBL syntax binding to the T01   
        	Author: Oriol Bausà

     --><schema xmlns="http://purl.oclc.org/dsdl/schematron" xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2" xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2" xmlns:ubl="urn:oasis:names:specification:ubl:schema:xsd:Order-2" queryBinding="xslt2">
  <title>BIIRULES  T01 bound to UBL</title>
  <ns prefix="cbc" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"/>
  <ns prefix="cac" uri="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"/>
  <ns prefix="ubl" uri="urn:oasis:names:specification:ubl:schema:xsd:Order-2"/>
  <phase id="BIIRULEST01_phase">
    <active pattern="UBL-T01"/>
  </phase>
  <phase id="codelist_phase">
    <active pattern="CodesT01"/>
  </phase>
  <!-- Abstract CEN BII patterns -->
  <!-- ========================= -->
  <?DSDL_INCLUDE_START abstract/BIIRULES-T01.sch?><pattern abstract="true" id="T01">
  <rule context="$Order">
    <assert test="$BIIRULE-T01-R001" flag="fatal">[BIIRULE-T01-R001]-A conformant CEN BII order core data model MUST have a syntax identifier.</assert>
    <assert test="$BIIRULE-T01-R002" flag="fatal">[BIIRULE-T01-R002]-A conformant CEN BII order core data model MUST have a customization identifier.</assert>
    <assert test="$BIIRULE-T01-R003" flag="fatal">[BIIRULE-T01-R003]-A conformant CEN BII order core data model MUST have a profile identifier.</assert>
  </rule>
  <rule context="$Order_Note">
    <assert test="$BIIRULE-T01-R004" flag="warning">[BIIRULE-T01-R004]-Language SHOULD be defined for note field</assert>
  </rule>
  <rule context="$Line_Note">
    <assert test="$BIIRULE-T01-R005" flag="warning">[BIIRULE-T01-R005]-Language SHOULD be defined for note field at line level</assert>
  </rule>
  <rule context="$Originator_document">
    <assert test="$BIIRULE-T01-R006" flag="fatal">[BIIRULE-T01-R006]-For any originator document referred in an order, a textual explanation of the document MUST be provided.</assert>
  </rule>
  <rule context="$Annex">
    <assert test="$BIIRULE-T01-R007" flag="fatal">[BIIRULE-T01-R007]-For any document referred in an order, a textual explanation of the document MUST be provided.</assert>
  </rule>
  <rule context="$Contract">
    <assert test="$BIIRULE-T01-R008" flag="warning">[BIIRULE-T01-R008]-If Contract Identifier not specified SHOULD Contract Type text be used for Contract Reference (optional)</assert>
  </rule>
  <rule context="$Customer">
    <assert test="$BIIRULE-T01-R009" flag="fatal">[BIIRULE-T01-R009]-An order MUST contain the full name of the customer.</assert>
  </rule>
  <rule context="$Supplier">
    <assert test="$BIIRULE-T01-R010" flag="fatal">[BIIRULE-T01-R010]-An order MUST contain the full name of the supplier.</assert>
  </rule>
  <rule context="$Requested_delivery_period">
    <assert test="$BIIRULE-T01-R011" flag="warning">[BIIRULE-T01-R011]-A delivery period end date SHOULD be later or equal to a delivery period start date</assert>
  </rule>
  <rule context="$Supplier">
    <assert test="$BIIRULE-T01-R012" flag="warning">[BIIRULE-T01-R012]-A seller party address in an order SHOULD contain at least City and zip code or have one or more address lines.</assert>
    <assert test="$BIIRULE-T01-R013" flag="warning">[BIIRULE-T01-R013]-If the supplier tax identifier is provided and if supplier and customer country codes are provided and are not equal then supplier tax identifier must be prefixed with the supplier country code.</assert>
  </rule>
  <rule context="$Customer">
    <assert test="$BIIRULE-T01-R014" flag="warning">[BIIRULE-T01-R014]-A customer address in an invoice SHOULD contain at least city and zip code or have one or more address lines.</assert>
    <assert test="$BIIRULE-T01-R015" flag="warning">[BIIRULE-T01-R015]-If the customer tax identifier is provided and if supplier and customer country codes are provided and are not equal then customer tax identifier must be prefixed with the customer country code.</assert>
  </rule>
  <rule context="$AllowanceCharge">
    <assert test="$BIIRULE-T01-R016" flag="fatal">[BIIRULE-T01-R016]-AllowanceChargeReason text MUST be specified for all allowances and charges</assert>
  </rule>
  <rule context="$Tax_Total">
    <assert test="$BIIRULE-T01-R017" flag="fatal">[BIIRULE-T01-R017]-If an order has a tax total then each instance of a total MUST refer to a single tax schema.</assert>
  </rule>
  <rule context="$Total_Amounts">
    <assert test="$BIIRULE-T01-R018" flag="fatal">[BIIRULE-T01-R018]-Order total line amount MUST equal the sum of the line totals</assert>
    <assert test="$BIIRULE-T01-R019" flag="fatal">[BIIRULE-T01-R019]-If there is a total allowance it MUST be equal to the sum of allowances at document level</assert>
    <assert test="$BIIRULE-T01-R020" flag="fatal">[BIIRULE-T01-R020]-If there is a total charges it MUST be equal to the sum of document level charges.</assert>
    <assert test="$BIIRULE-T01-R021" flag="fatal">[BIIRULE-T01-R021]-In an order, payable amount due is the sum of order line totals minus document level allowances plus document level charges.</assert>
  </rule>
  <rule context="$Order_Line">
    <assert test="$BIIRULE-T01-R022" flag="fatal">[BIIRULE-T01-R022]-If price is specified Order line amount MUST be equal to the price amount multiplied by the quantity  plus charges minus allowances at line level</assert>
  </rule>
  <rule context="$Item">
    <assert test="$BIIRULE-T01-R023" flag="warning">[BIIRULE-T01-R023]-Product names SHOULD NOT exceed 50 characters long</assert>
    <assert test="$BIIRULE-T01-R024" flag="warning">[BIIRULE-T01-R024]-If standard identifiers are provided within an item description, an Schema Identifier SHOULD be provided (e.g. GTIN)</assert>
    <assert test="$BIIRULE-T01-R025" flag="warning">[BIIRULE-T01-R025]-Classification codes within an item description SHOULD have a List Identifier attribute (e.g. CPV or UNSPSC)</assert>
  </rule>
  <rule context="$Item_Price">
    <assert test="$BIIRULE-T01-R026" flag="fatal">[BIIRULE-T01-R026]-Prices of items MUST be positive or zero</assert>
  </rule>
</pattern><?DSDL_INCLUDE_END abstract/BIIRULES-T01.sch?>
  <!-- Data Binding parameters -->
  <!-- ======================= -->
  <?DSDL_INCLUDE_START UBL/BIIRULES-UBL-T01.sch?><pattern id="UBL-T01" is-a="T01">
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
  <param value="(cbc:AllowanceTotalAmount) and cbc:AllowanceTotalAmount = (round(sum(/ubl:Order/cac:AllowanceCharge[cbc:ChargeIndicator=&#34;false&#34;]/cbc:Amount) * 100) div 100) or not(cbc:AllowanceTotalAmount)" name="BIIRULE-T01-R019"/>
  <param value="(cbc:ChargeTotalAmount) and cbc:ChargeTotalAmount = (round(sum(/ubl:Order/cac:AllowanceCharge[cbc:ChargeIndicator=&#34;true&#34;]/cbc:Amount) * 100) div 100) or not(cbc:ChargeTotalAmount)" name="BIIRULE-T01-R020"/>
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
</pattern><?DSDL_INCLUDE_END UBL/BIIRULES-UBL-T01.sch?>
  <!-- Code Lists Binding rules -->
  <!-- ======================== -->
  <?DSDL_INCLUDE_START codelist/BIIRULESCodesT01.sch?><pattern id="CodesT01">
<!--
  This implementation supports genericode code lists with no instance
  meta data.
-->
<!--
    Start of synthesis of rules from code list context associations.
Version 0.3
-->

<rule context="cbc:DocumentCurrencyCode" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AED AFN ALL AMD ANG AOA ARS AUD AWG AZN BAM BBD BDT BGN BHD BIF BMD BND BOB BOV BRL BSD BTN BWP BYR BZD CAD CDF CHE CHF CHW CLF CLP CNY COP COU CRC CUP CVE CZK DJF DKK DOP DZD EEK EGP ERN ETB EUR FJD FKP GBP GEL GHS GIP GMD GNF GTQ GWP GYD HKD HNL HRK HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW KRW KWD KYD KZT LAK LBP LKR LRD LSL LTL LVL LYD MAD MDL MGA MKD MMK MNT MOP MRO MUR MVR MWK MXN MXV MYR MZN NAD NGN NIO NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR SDG SEK SGD SHP SKK SLL SOS SRD STD SVC SYP SZL THB TJS TMM TND TOP TRY TTD TWD TZS UAH UGX USD USN USS UYI UYU UZS VEF VND VUV WST XAF XAG XAU XBA XBB XBC XBD XCD XDR XFU XOF XPD XPF XTS XXX YER ZAR ZMK ZWR ZWD ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-001-001]-Currencies in an invoice MUST be coded using ISO currency code</assert>
</rule>

<rule context="@currencyID" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AED AFN ALL AMD ANG AOA ARS AUD AWG AZN BAM BBD BDT BGN BHD BIF BMD BND BOB BOV BRL BSD BTN BWP BYR BZD CAD CDF CHE CHF CHW CLF CLP CNY COP COU CRC CUP CVE CZK DJF DKK DOP DZD EEK EGP ERN ETB EUR FJD FKP GBP GEL GHS GIP GMD GNF GTQ GWP GYD HKD HNL HRK HTG HUF IDR ILS INR IQD IRR ISK JMD JOD JPY KES KGS KHR KMF KPW KRW KWD KYD KZT LAK LBP LKR LRD LSL LTL LVL LYD MAD MDL MGA MKD MMK MNT MOP MRO MUR MVR MWK MXN MXV MYR MZN NAD NGN NIO NOK NPR NZD OMR PAB PEN PGK PHP PKR PLN PYG QAR RON RSD RUB RWF SAR SBD SCR SDG SEK SGD SHP SKK SLL SOS SRD STD SVC SYP SZL THB TJS TMM TND TOP TRY TTD TWD TZS UAH UGX USD USN USS UYI UYU UZS VEF VND VUV WST XAF XAG XAU XBA XBB XBC XBD XCD XDR XFU XOF XPD XPF XTS XXX YER ZAR ZMK ZWR ZWD ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-001-002]-Currencies in an invoice MUST be coded using ISO currency code</assert>
</rule>

<rule context="cac:Country//cbc:IdentificationCode" flag="fatal">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AD AE AF AG AI AL AM AN AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BL BJ BM BN BO BR BS BT BV BW BY BZ CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CX CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT MU MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RO RS RU RW SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR ST SV SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG UM US UY UZ VA VC VE VG VI VN VU WF WS YE YT ZA ZM ZW ',concat(' ',normalize-space(.),' ') ) ) )" flag="fatal">[CL-001-003]-Country codes in a credit note MUST be coded using ISO code list 3166-1</assert>
</rule>

<rule context="cac:TaxScheme//cbc:ID" flag="warning">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' AAA AAB AAC AAD AAE AAF AAG AAH AAI AAJ AAK AAL ADD BOL CAP CAR COC CST CUD CVD ENV EXC EXP FET FRE GCN GST ILL IMP IND LAC LCN LDP LOC LST MCA MCD OTH PDB PDC PRF SCN SSS STT SUP SUR SWT TAC TOT TOX TTA VAD VAT ',concat(' ',normalize-space(.),' ') ) ) )" flag="warning">[CL-001-004]-Invoice tax schemes MUST be coded using UN/ECE 5153 code list</assert>
</rule>

<rule context="cac:TaxCategory//cbc:ID" flag="warning">
  <assert test="( ( not(contains(normalize-space(.),' ')) and contains( ' A AA AB AC AD AE B C E G H O S Z ',concat(' ',normalize-space(.),' ') ) ) )" flag="warning">[CL-001-005]-Invoice tax categories MUST be coded using UN/ECE 5305 code list</assert>
</rule>
<!--
    End of synthesis of rules from code list context associations.
-->
</pattern><?DSDL_INCLUDE_END codelist/BIIRULESCodesT01.sch?>
</schema>