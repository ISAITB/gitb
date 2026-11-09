<?xml version="1.0"?>
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

<!--
  Archives exported before 1.29.0 hold the test session timeout (system configuration "session_alive_time") as a plain
  number of seconds. As of 1.29.0 it is a JSON object with separate timeouts (see Flyway migration
  V133__Session_timeouts_for_interactive_sessions): the plain number is used for all timeouts. A value that is not a
  plain number (e.g. already JSON) is copied as-is.
-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exp="http://www.gitb.com/export/v1/">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    <xsl:template match="@version[parent::exp:export]">
        <xsl:attribute name="version">
            <xsl:value-of select="'1.29.0'"/>
        </xsl:attribute>
    </xsl:template>
    <xsl:template match="exp:config[@name='session_alive_time']/exp:value[normalize-space(.) != '' and translate(normalize-space(.), '0123456789', '') = '']">
        <xsl:variable name="seconds" select="normalize-space(.)"/>
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:value-of select="concat('{ &quot;enabled&quot;: true, &quot;userPendingTimeout&quot;: ', $seconds, ', &quot;adminPendingTimeout&quot;: ', $seconds, ', &quot;otherTimeout&quot;: ', $seconds, ' }')"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
