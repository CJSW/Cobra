<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet exclude-result-prefixes="java" version="1.0"
    xmlns:java="http://xml.apache.org/xslt/java" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://www.w3.org/1999/xhtml" 
>
    <xsl:include href="libpage.xsl"/>
    <xsl:param name="user"/>
    <!-- used to control rendering -->
    <xsl:param name="render"/>
    <!-- styles in include in csv format -->
    <xsl:param name="styles"/>
    
    <xsl:template match="/">
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
      <xsl:call-template name="page.head"/>
        
        <xsl:variable name="hideHeader"><xsl:value-of select="contains($render,'suppressHeader')"/></xsl:variable>
				<xsl:choose>
        <xsl:when test="$hideHeader='false'">
					<body class="styled home-page home page page-id-27 page-template-default site-cjsw-com theme-cjsw-main-fd2011">
        	<xsl:call-template name="page.header"/>
					<div id="header-wrap">
						<div id="cjsw-cloud"><a href="http://cjsw.com">CJSW</a></div>
					        <div id="content">
        <!-- if a title has been set, show it in an H1.  
        	Title is also included as a part of the <head><title> block in page.head
        	To set the title when constructing the body, do a 
        		<xsl:value-of select="java:setTitle($render,'A Page Title')"/>
        -->
		    <xsl:variable name="title"><xsl:value-of select="java:getString($render,'pageTitle')"/></xsl:variable>
		    <xsl:if test="$title">
		    <h1><xsl:value-of select="$title"/></h1>
		    </xsl:if>

          <!-- 
            The body of the page as previously constructed is inserted at page comment
            ** DO NOT REMOVE, THIS TAG NEEDS TO BE HERE FOR THE CONTENT TO BE INCLUDED ** 
          -->
          <xsl:comment>page</xsl:comment>
          <div id="contact">Having problems? <a href="mailto:andy@benow.ca?subject=CJSW Security Help (login.cjsw.com)">Get help</a>.</div>
        </div>
</div>
      </body>

        </xsl:when>
        <xsl:otherwise>
        <body>
          <xsl:comment>page</xsl:comment>
          </body>
        </xsl:otherwise>
        </xsl:choose>
        
        <!-- uncomment this to show page XML and render status
		    <xsl:call-template name="util.xml"/>
        Render: <xsl:value-of select="$render"/><p/>
        Hide: <xsl:value-of select="$hideHeader"/><p/>
          -->

    </html>
    </xsl:template>
</xsl:stylesheet>

