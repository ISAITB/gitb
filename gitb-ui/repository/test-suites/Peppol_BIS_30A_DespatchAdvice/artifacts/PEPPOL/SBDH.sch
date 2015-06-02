<schema queryBinding="xslt2" xmlns="http://purl.oclc.org/dsdl/schematron">
  <title>OpenPEPPOL Envelope Specification (SBDH) Test Schematron</title>
  <pattern name="Check Standard Business Document Header">
    <rule context="/StandardBusinessDocumentHeader">
        <assert flag="fatal" test="(HeaderVersion)">A Standart Business Document Header MUST have 'HeaderVersion' element.</assert>
        <assert flag="fatal" test="(Sender)">A Standart Business Document Header MUST have 'Sender' element.</assert>
        <assert flag="fatal" test="(Receiver)">A Standart Business Document Header MUST have 'Receiver' element.</assert>
        <assert flag="fatal" test="(DocumentIdentification)">A Standart Business Document Header MUST have 'DocumentIdentification' element.</assert>
        <assert flag="fatal" test="(BusinessScope)">A Standart Business Document Header MUST have 'BusinessScope' element.</assert>
    </rule>
    <rule context="/StandardBusinessDocumentHeader/HeaderVersion">
        <assert flag="fatal" test=". = '1.0'">'HeaderVersion' value MUST be 1.0</assert>
    </rule>
    <rule context="/StandardBusinessDocumentHeader/Sender">
        <assert flag="fatal" test="(Identifier)">A 'Sender' element MUST have 'Identifier' element</assert>
        <assert flag="fatal" test="contains(Identifier, ':') and string-length(substring-before(Identifier, ':')) = 4 and string-length(substring-after(Identifier, ':')) &gt; 0 and string-length(substring-after(Identifier, ':')) &lt; 51">'Identifier' value MUST include a 4-digit Issuing Agency code, a colon (:) and participant identifier which MUST be at least 1 and at most 50 characters long.</assert>
        <assert flag="fatal" test="Identifier = '${sender_participant_identifier}'">'Identifier' value should match with the value (${sender_participant_identifier}) set through GITB-ENGINE interface.</assert>
        <assert flag="fatal" test="Identifier/@Authority and Identifier/@Authority = 'iso6523-actorid-upis'">'Identifier' element MUST have attribute 'Authority' and its value MUST be 'iso6523-actorid-upis'.</assert>
    </rule>
    <rule context="/StandardBusinessDocumentHeader/Receiver">
        <assert flag="fatal" test="(Identifier)">A 'Receiver' element MUST have 'Identifier' element</assert>
        <assert flag="fatal" test="contains(Identifier, ':') and string-length(substring-before(Identifier, ':')) = 4 and string-length(substring-after(Identifier, ':')) &gt; 0 and string-length(substring-after(Identifier, ':')) &lt; 51">'Identifier' value MUST include a 4-digit Issuing Agency code, a colon (:) and participant identifier which MUST be at least 1 and at most 50 characters long.</assert>
        <assert flag="fatal" test="Identifier = '${receiver_participant_identifier}'">'Identifier' value should match with the value (${receiver_participant_identifier}).</assert>
        <assert flag="fatal" test="Identifier/@Authority = 'iso6523-actorid-upis'">'Authority' attribute of 'Identifier' element MUST be 'iso6523-actorid-upis'.</assert>
    </rule>
    <rule context="/StandardBusinessDocumentHeader/DocumentIdentification">
        <assert flag="fatal" test="(Standard)">A 'DocumentIdentification' element MUST have 'Standard' element.</assert>
        <assert flag="fatal" test="Standard = '${document_identifier_ns}'">'Standard' element MUST have value '${document_identifier_ns}'.</assert>
        <assert flag="fatal" test="(TypeVersion) and (TypeVersion != '')">A 'DocumentIdentification' element MUST have 'TypeVersion' element and its value can not be empty.</assert>
        <assert flag="fatal" test="(InstanceIdentifier) and (InstanceIdentifier != '')">A 'DocumentIdentification' element MUST have 'InstanceIdentifier' element and its value can not be empty.</assert>
        <assert flag="fatal" test="(Type)">A 'DocumentIdentification' element MUST have 'Type' element.</assert>
        <assert flag="fatal" test="Type = '${business_message_type}'">'Type' element MUST have value '${business_message_type}'.</assert>
        <assert flag="fatal" test="(CreationDateAndTime)">A 'DocumentIdentification' element MUST have 'CreationDateAndTime' element.</assert>
        <assert flag="fatal" test="string(timezone-from-dateTime(CreationDateAndTime)) != ''">'CreationDateAndTime' element MUST be a dateTime and have time zone information.</assert>
    </rule>
    <rule context="/StandardBusinessDocumentHeader/BusinessScope">
        <assert flag="fatal" test="count(Scope) &gt;= 2">There must be at least 2 'Scope' elements; repeated twice - once for 'DocumentID' once for 'ProcessID'.</assert>
        <assert flag="fatal" test="count(Scope[Type='DOCUMENTID']) &gt;=1 and count(Scope[Type='PROCESSID']) &gt;=1 and count(Scope[Type='DOCUMENTID']) = count(Scope[Type='PROCESSID'])">'Type' value of 'Scope' element MUST be either 'DOCUMENTID' or 'PROCESSID' and they must be equal in count.</assert>
        <assert flag="fatal" test="count(Scope[Type='DOCUMENTID'][InstanceIdentifier='${document_identifier}']) &gt; 0">There MUST be a 'Scope' element, whose 'Type' value is 'DOCUMENTID', with an 'InstanceIdentifier' value '${document_identifier}'.</assert>
        <assert flag="fatal" test="count(Scope[Type='PROCESSID'][InstanceIdentifier='${process_identifier}']) &gt; 0">There MUST be a 'Scope' element, whose 'Type' value is 'PROCESSID', with an 'InstanceIdentifier' value '${process_identifier}'.</assert>
    </rule>
  </pattern>
</schema>