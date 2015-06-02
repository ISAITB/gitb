<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.gitb.com/tpl/v1/"
                xmlns:tdl="http://www.gitb.com/tdl/v1/"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:gitb="http://www.gitb.com/core/v1/"
                exclude-result-prefixes="xs"
                version="2.0">
    <xsl:strip-space elements="*"/>
    <xsl:output method="xml" indent="yes"/>


    <!--

    notes

    1. assign step?

    2. desc element could be an attribute in a TestStep

    3. loop element could be of type LoopStep, not Sequence

    -->

    <xsl:template match="/">
        <xsl:apply-templates />
    </xsl:template>

    <xsl:template match="tdl:testcase">
        <testcase xmlns="http://www.gitb.com/tpl/v1/"  xmlns:gitb="http://www.gitb.com/core/v1/">
            <xsl:attribute name="id">
                <xsl:value-of select="@id" />
            </xsl:attribute>

            <xsl:apply-templates  select="tdl:metadata"/>
            <xsl:apply-templates  select="tdl:actors"/>
            <xsl:apply-templates  select="tdl:preliminary"/>
            <xsl:apply-templates  select="tdl:steps"/>
        </testcase>
    </xsl:template>

    <xsl:template match="tdl:metadata">
        <metadata>
            <xsl:copy-of copy-namespaces="no" select="node()"/>
        </metadata>
    </xsl:template>

    <xsl:template match="tdl:actors">
        <actors>
            <xsl:copy-of copy-namespaces="no" select="node()"/>
        </actors>
    </xsl:template>

    <xsl:template match="tdl:preliminary">

    </xsl:template>

    <xsl:template match="tdl:steps">
        <steps>
            <xsl:apply-templates  />
        </steps>
    </xsl:template>

    <xsl:template match="tdl:assign">
        <assign />
    </xsl:template>

    <xsl:template match="tdl:send | tdl:receive | tdl:listen">
        <msg>
            <xsl:attribute name="from">
                <xsl:value-of select="@from" />
            </xsl:attribute>

            <xsl:attribute name="to">
                <xsl:value-of select="@to" />
            </xsl:attribute>

            <xsl:element name="desc">
                <xsl:value-of select="@desc" />
            </xsl:element>
        </msg>
    </xsl:template>

    <xsl:template match="tdl:if">
        <decision>
            <xsl:element name="desc">
                <xsl:value-of select="@desc" />
            </xsl:element>

            <xsl:apply-templates select="tdl:then" />
            <xsl:apply-templates select="tdl:else" />
        </decision>
    </xsl:template>

    <xsl:template match="tdl:then">
        <then>
            <xsl:apply-templates  />
        </then>
    </xsl:template>

    <xsl:template match="tdl:else">
        <else>
            <xsl:apply-templates  />
        </else>
    </xsl:template>

    <xsl:template match="tdl:while | tdl:repuntil | tdl:foreach">
        <loop>
            <xsl:element name="desc">
                <xsl:value-of select="@desc" />
            </xsl:element>

            <xsl:apply-templates />
        </loop>
    </xsl:template>

    <xsl:template match="tdl:flow">
        <flow>
            <xsl:element name="desc">
                <xsl:value-of select="@desc" />
            </xsl:element>

            <xsl:for-each select="tdl:thread">
                <thread>
                    <xsl:apply-templates />
                </thread>
            </xsl:for-each>
        </flow>
    </xsl:template>

    <xsl:template match="tdl:verify">
        <verify>
            <xsl:element name="desc">
                <xsl:value-of select="@desc" />
            </xsl:element>
        </verify>
    </xsl:template>

    <xsl:template match="tdl:exit">
        <exit>
            <xsl:element name="desc">
                <xsl:value-of select="@desc" />
            </xsl:element>
        </exit>
    </xsl:template>

    <xsl:template match="tdl:interact">
        <interact>
            <xsl:attribute name="with">
                <xsl:value-of select="tdl:instruct/@with" />
            </xsl:attribute>

            <xsl:element name="desc">
                <xsl:value-of select="@desc" />
            </xsl:element>
        </interact>
    </xsl:template>

    <!-- elements that will not be matched but affected by 
        xsl:apply-templates should go here -->
    <xsl:template match="tdl:cond | tdl:call" />

</xsl:stylesheet>
