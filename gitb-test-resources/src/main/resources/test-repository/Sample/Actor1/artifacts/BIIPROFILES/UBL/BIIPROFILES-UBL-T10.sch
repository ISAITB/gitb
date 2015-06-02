<!-- Schematron binding rules generated automatically. -->
<!-- Data binding to UBL syntax for T10 -->
<!-- (2009). Invinet Sistemes -->
<pattern xmlns="http://purl.oclc.org/dsdl/schematron" id="UBL-T10" is-a="T10">
  <param value=". = 'urn:www.cenbii.eu:profile:bii04:ver1.0' or . = 'urn:www.cenbii.eu:profile:bii05:ver1.0' or . = 'urn:www.cenbii.eu:profile:bii06:ver1.0'" name="BIIPROFILE-T10-R001"/>
  <param value="//cbc:ProfileID" name="Invoice_Profile"/>
  <param value="local-name(/*) = 'Invoice' and (//cac:OrderReference/cbc:ID) and //cbc:ProfileID = 'urn:www.cenbii.eu:profile:bii06:ver1.0' or not(//cbc:ProfileID = 'urn:www.cenbii.eu:profile:bii06:ver1.0')" name="BIIPROFILE-T10-R002"/>
  <param value="/ubl:Invoice" name="Invoice"/>
</pattern>
