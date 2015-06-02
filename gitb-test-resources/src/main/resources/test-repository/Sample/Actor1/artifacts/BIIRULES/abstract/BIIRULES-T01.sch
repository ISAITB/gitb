<!-- Schematron rules generated automatically. -->
<!-- Abstract rules for T01 -->
<!-- (2009). Invinet Sistemes -->
<pattern abstract="true" id="T01" xmlns="http://purl.oclc.org/dsdl/schematron">
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
</pattern>
