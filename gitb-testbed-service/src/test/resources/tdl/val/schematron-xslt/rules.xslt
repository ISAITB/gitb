<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright (C) 2026 European Union
  ~
  ~ Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
  ~ versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
  ~
  ~ You may obtain a copy of the Licence at:
  ~
  ~ https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
  ~
  ~ Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
  ~ "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
  ~ the specific language governing permissions and limitations under the Licence.
  -->

<!-- Compiled-Schematron-style XSLT that loads a value set file located next to it via document(). -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                version="2.0">
    <xsl:variable name="valuesets" select="document('valuesets.xml')/ValueSets/ValueSet"/>
    <xsl:output method="xml" indent="yes"/>
    <xsl:template match="*" mode="schematron-select-full-path">
        <xsl:apply-templates mode="schematron-get-full-path" select="."/>
    </xsl:template>
    <xsl:template match="*" mode="schematron-get-full-path">
        <xsl:apply-templates mode="schematron-get-full-path" select="parent::*"/>
        <xsl:text>/</xsl:text>
        <xsl:value-of select="name()"/>
        <xsl:text>[</xsl:text>
        <xsl:value-of select="1 + count(preceding-sibling::*[name() = name(current())])"/>
        <xsl:text>]</xsl:text>
    </xsl:template>
    <xsl:template match="/">
        <svrl:schematron-output schemaVersion="" title="">
            <xsl:apply-templates mode="M1" select="/"/>
        </svrl:schematron-output>
    </xsl:template>
    <xsl:template match="item" mode="M1" priority="1000">
        <svrl:fired-rule context="item"/>
        <xsl:choose>
            <xsl:when test="@code = $valuesets[xs:string(./@id) eq &quot;urn:test:codes&quot;]/ConceptList/Concept/@code"/>
            <xsl:otherwise>
                <svrl:failed-assert test="@code = $valuesets[xs:string(./@id) eq &quot;urn:test:codes&quot;]/ConceptList/Concept/@code">
                    <xsl:attribute name="location">
                        <xsl:apply-templates mode="schematron-select-full-path" select="."/>
                    </xsl:attribute>
                    <svrl:text>Code must be part of the value set</svrl:text>
                </svrl:failed-assert>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:apply-templates mode="M1" select="*|comment()|processing-instruction()"/>
    </xsl:template>
    <xsl:template match="text()" mode="M1" priority="-1"/>
    <xsl:template match="@*|node()" mode="M1" priority="-2">
        <xsl:apply-templates mode="M1" select="*|comment()|processing-instruction()"/>
    </xsl:template>
</xsl:stylesheet>
