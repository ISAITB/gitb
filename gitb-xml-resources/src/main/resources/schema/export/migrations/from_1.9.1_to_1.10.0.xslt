<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exp="http://www.gitb.com/export/v1/">
    <xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
    <xsl:template match="@version[parent::exp:export]">
        <xsl:attribute name="version">
            <xsl:value-of select="'1.10.0'"/>
        </xsl:attribute>
    </xsl:template>
    <xsl:template match="exp:testSuite|exp:testCase">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <exp:identifier><xsl:value-of select="./exp:shortName/text()"/></exp:identifier>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>