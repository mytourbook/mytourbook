<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet
  version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output indent="yes" />
    
    <!-- Copy all elements with their attributes -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Copy <required> elements belonging to nl feature groups, adding "optional" attribute -->
    <xsl:template match="unit[contains(@id,'.nl') and contains(@id,'.feature.group')]/requires/required[not(contains(@name,'.feature.jar'))]">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:attribute name="optional">true</xsl:attribute>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

    <!-- Copy <required> elements belonging to nl plugins, adding "greedy" attribute -->
    <xsl:template match="unit[contains(@id,'.nl') and not(contains(@id,'.feature.group'))]/requires/required">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:attribute name="greedy">false</xsl:attribute>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
