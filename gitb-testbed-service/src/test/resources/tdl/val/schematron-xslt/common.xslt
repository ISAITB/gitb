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

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
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
</xsl:stylesheet>
