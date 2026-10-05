<?xml version="1.0" encoding="UTF-8"?>
<sch:pattern xmlns:sch="http://purl.oclc.org/dsdl/schematron">
    <sch:rule context="/invoice">
        <sch:assert test="total">Invoice must have a total element (from included rules)</sch:assert>
    </sch:rule>
</sch:pattern>
