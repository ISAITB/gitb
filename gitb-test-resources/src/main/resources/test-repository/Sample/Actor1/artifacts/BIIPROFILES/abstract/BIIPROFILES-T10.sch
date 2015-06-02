<!-- Schematron rules generated automatically. -->
<!-- Abstract rules for T10 -->
<!-- (2009). Invinet Sistemes -->
<pattern xmlns="http://purl.oclc.org/dsdl/schematron" abstract="true" id="T10">
  <rule context="$Invoice_Profile">
    <assert test="$BIIPROFILE-T10-R001" flag="fatal">[BIIPROFILE-T10-R001]-An invoice transaction T10 must only be used in Profiles 4, 5 or 6.</assert>
  </rule>
  <rule context="$Invoice">
    <assert test="$BIIPROFILE-T10-R002" flag="fatal">[BIIPROFILE-T10-R002]-An invoice transaction T10 in Profile 6 MUST have an order reference identifier.</assert>
  </rule>
</pattern>
