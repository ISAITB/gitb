<!-- Schematron rules generated automatically. -->
<!-- Abstract rules for T03 -->
<!-- (2009). Invinet Sistemes -->
<pattern abstract="true" id="T03" xmlns="http://purl.oclc.org/dsdl/schematron">
  <rule context="$Order_Response">
    <assert test="$BIIRULE-T03-R001" flag="fatal">[BIIRULE-T03-R001]-A conformant CEN BII order response core data model MUST have a syntax identifier.</assert>
    <assert test="$BIIRULE-T03-R002" flag="fatal">[BIIRULE-T03-R002]-A conformant CEN BII order response  core data model MUST have a syntax identifier.</assert>
    <assert test="$BIIRULE-T03-R003" flag="fatal">[BIIRULE-T03-R003]-A conformant CEN BII order response  core data model MUST have a profile identifier.</assert>
    <assert test="$BIIRULE-T03-R004" flag="warning">[BIIRULE-T03-R004]-Only one note field must be specified </assert>
  </rule>
  <rule context="$Order_Response_Note">
    <assert test="$BIIRULE-T03-R005" flag="warning">[BIIRULE-T03-R005]-Language SHOULD be defined for note field</assert>
  </rule>
  <rule context="$Customer">
    <assert test="$BIIRULE-T03-R006" flag="fatal">[BIIRULE-T03-R006]-An order response  MUST contain the full name of the customer.</assert>
  </rule>
  <rule context="$Supplier">
    <assert test="$BIIRULE-T03-R007" flag="fatal">[BIIRULE-T03-R007]-An order response  MUST contain the full name of the supplier.</assert>
  </rule>
</pattern>
