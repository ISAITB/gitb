<!-- Schematron rules generated automatically. -->
<!-- Abstract rules for T15 -->
<!-- (2009). Invinet Sistemes -->
<pattern abstract="true" id="T15" xmlns="http://purl.oclc.org/dsdl/schematron">
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
</pattern>
