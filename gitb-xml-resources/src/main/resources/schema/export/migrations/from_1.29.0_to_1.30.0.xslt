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
  Archives exported before 1.30.0 may hold the following system configurations that changed in 1.30.0:
  - "welcome_title" (the welcome page title) was replaced by the JSON "welcome_texts" configuration (see Flyway
    migration V154__Welcome_page_texts): a defined title is carried over as the "title" property, otherwise it is dropped.
  - An empty "welcome" (welcome message) now means that the message is not displayed, whereas before it was ignored in
    favour of the built-in default (see Flyway migration V157__Optional_welcome_message): empty entries are dropped.
-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exp="http://www.gitb.com/export/v1/">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    <xsl:template match="@version[parent::exp:export]">
        <xsl:attribute name="version">
            <xsl:value-of select="'1.30.0'"/>
        </xsl:attribute>
    </xsl:template>
    <xsl:template match="exp:config[@name='welcome_title']">
        <xsl:if test="exp:value">
            <xsl:copy>
                <xsl:apply-templates select="@*[local-name() != 'name']"/>
                <xsl:attribute name="name">welcome_texts</xsl:attribute>
                <exp:value>
                    <xsl:text>{"title":"</xsl:text>
                    <xsl:call-template name="json-escape">
                        <xsl:with-param name="text" select="string(exp:value)"/>
                    </xsl:call-template>
                    <xsl:text>"}</xsl:text>
                </exp:value>
            </xsl:copy>
        </xsl:if>
    </xsl:template>
    <xsl:template match="exp:config[@name='welcome'][not(exp:value) or exp:value = '']"/>
    <!-- Escapes a text to be used within a JSON string. -->
    <xsl:template name="json-escape">
        <xsl:param name="text"/>
        <xsl:variable name="backslashes">
            <xsl:call-template name="replace">
                <xsl:with-param name="text" select="$text"/>
                <xsl:with-param name="from" select="'\'"/>
                <xsl:with-param name="to" select="'\\'"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="quotes">
            <xsl:call-template name="replace">
                <xsl:with-param name="text" select="string($backslashes)"/>
                <xsl:with-param name="from" select="'&quot;'"/>
                <xsl:with-param name="to" select="'\&quot;'"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="lineFeeds">
            <xsl:call-template name="replace">
                <xsl:with-param name="text" select="string($quotes)"/>
                <xsl:with-param name="from" select="'&#10;'"/>
                <xsl:with-param name="to" select="'\n'"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="carriageReturns">
            <xsl:call-template name="replace">
                <xsl:with-param name="text" select="string($lineFeeds)"/>
                <xsl:with-param name="from" select="'&#13;'"/>
                <xsl:with-param name="to" select="'\r'"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:call-template name="replace">
            <xsl:with-param name="text" select="string($carriageReturns)"/>
            <xsl:with-param name="from" select="'&#9;'"/>
            <xsl:with-param name="to" select="'\t'"/>
        </xsl:call-template>
    </xsl:template>
    <xsl:template name="replace">
        <xsl:param name="text"/>
        <xsl:param name="from"/>
        <xsl:param name="to"/>
        <xsl:choose>
            <xsl:when test="contains($text, $from)">
                <xsl:value-of select="substring-before($text, $from)"/>
                <xsl:value-of select="$to"/>
                <xsl:call-template name="replace">
                    <xsl:with-param name="text" select="substring-after($text, $from)"/>
                    <xsl:with-param name="from" select="$from"/>
                    <xsl:with-param name="to" select="$to"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
