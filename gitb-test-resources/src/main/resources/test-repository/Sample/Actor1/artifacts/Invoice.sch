<?xml version="1.0" ?>
<sch:schema queryBinding="xslt2"
	xmlns:sch="http://purl.oclc.org/dsdl/schematron"
	xmlns:hl7 ="urn:hl7-org:v3">
	<sch:title>Hasta Cikis MSVS Gonderim Kurallari</sch:title>
	<!--  assumes that effectiveTime value has at least 8 digits -->
	<sch:ns prefix="hl7" uri="urn:hl7-org:v3" />
	<sch:ns prefix="srdc" uri="java:com.srdc.testframework.testengine.expr.SRDCDateFunctions" />

	<sch:pattern name="rule 4 validation">
		<sch:rule context="hl7:section[hl7:templateId[@root='2.16.840.1.113883.3.129.3.2.11']]//hl7:observation[hl7:templateId[@root='2.16.840.1.113883.3.129.3.4.315']]">
			<sch:assert test="(not(./hl7:value/@code = ../preceding-sibling::*//hl7:observation[hl7:templateId[@root='2.16.840.1.113883.3.129.3.4.315']]/hl7:value/@code))">
				Kural 8 geçerli değil (Sevk tanısı alanı aynı veri seti içinde aynı değerle birden fazla veri gönderilemez.)
				Sevk tanısı alanı değeri, kodu = <value-of select="./hl7:value/@code"/>
			</sch:assert>

			<sch:report test="(not(./hl7:value/@code = ../preceding-sibling::*//hl7:observation[hl7:templateId[@root='2.16.840.1.113883.3.129.3.4.315']]/hl7:value/@code)) and (not(./hl7:value/@code = ../following-sibling::*//hl7:observation[hl7:templateId[@root='2.16.840.1.113883.3.129.3.4.315']]/hl7:value/@code))">
				Kural 8 geçerli (Sevk tanısı alanı aynı veri seti içinde aynı değerle birden fazla veri gönderilemez.)
				Sevk tanısı alanı değeri, kodu = <value-of select="./hl7:value/@code"/>
			</sch:report>
		</sch:rule>
	</sch:pattern>
</sch:schema>
