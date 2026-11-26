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

<!-- XSLT that includes a module located next to it (the module provides the location templates). -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                version="2.0">
    <xsl:include href="common.xslt"/>
    <xsl:output method="xml" indent="yes"/>
    <xsl:template match="/">
        <svrl:schematron-output schemaVersion="" title="">
            <xsl:apply-templates mode="M1" select="/"/>
        </svrl:schematron-output>
    </xsl:template>
    <xsl:template match="item" mode="M1" priority="1000">
        <svrl:fired-rule context="item"/>
        <xsl:choose>
            <xsl:when test="@code = ('A', 'B')"/>
            <xsl:otherwise>
                <svrl:failed-assert test="@code = ('A', 'B')">
                    <xsl:attribute name="location">
                        <xsl:apply-templates mode="schematron-select-full-path" select="."/>
                    </xsl:attribute>
                    <svrl:text>Code must be A or B (from included module)</svrl:text>
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
