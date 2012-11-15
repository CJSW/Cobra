<?xml version="1.0"?> 
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  version="1.0"
  xmlns:java="http://xml.apache.org/xslt/java"
  exclude-result-prefixes="java"
>
        <xsl:output method="html" omit-xml-declaration="yes" indent="yes"/>

        <xsl:template match="/">
<xsl:value-of select="airing/show/name"/>
        </xsl:template>

</xsl:stylesheet>

