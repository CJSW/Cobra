<?xml version="1.0"?> 
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  version="1.0"
  xmlns:java="http://xml.apache.org/xslt/java"
  exclude-result-prefixes="java"
>
	<xsl:output method="html" omit-xml-declaration="yes" indent="yes"/>

	<xsl:template match="/">
	  <table border="0" cellpadding="0" cellspacing="0">
	    <tr>
	      <td><xsl:value-of select="airing/show/name"/></td>
            </tr>
	    <xsl:if test="airing/show/genre">
            <tr>
              <td><xsl:value-of select="airing/show/genre/name"/></td>
            </tr>
            </xsl:if>
            <tr>
              <td><xsl:value-of select="airing/display-start"/> <xsl:value-of select="airing/display-end"/></td>
            </tr>
	  </table>
	</xsl:template>

</xsl:stylesheet>
