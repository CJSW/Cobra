<?xml version="1.0"?> 
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  version="1.0"
  xmlns:java="http://xml.apache.org/xslt/java"
  exclude-result-prefixes="java"
>
  <xsl:param name="debug"></xsl:param>
  <xsl:param name="page"/>
  <xsl:param name="user"/>
  <xsl:param name="style"/>
  <xsl:param name="xslRoot"/>

  <xsl:output method="html"/>

  <!-- http://localhost:1999/web/ -->
  <!--
  <xsl:param name="gmapKey">ABQIAAAA4IkGUae9pv9k9O1Ax3yJtBT0bybpYqzqVzCiWXuChP47J82LHRSjflELiJ4TokCoa9tk12De1yRAqA</xsl:param>
  http://feralcoder:1999/web/
  <xsl:param name="gmapKey">ABQIAAAA4IkGUae9pv9k9O1Ax3yJtBSYCRLNe5ReAtdngaXHMHFHD2NUEhTv-prSoyf1DHTrrEUCrqb2dwDRRw</xsl:param>
  feralcoder
  <xsl:param name="gmapKey">ABQIAAAA4IkGUae9pv9k9O1Ax3yJtBRshyjd1aaGUYQwkNSd-Y28Z54eYxQXnrqaJBFn7K4cr5jB8Di13RGIHQ</xsl:param>
localhost
  <xsl:param name="gmapKey">ABQIAAAA4IkGUae9pv9k9O1Ax3yJtBRshyjd1aaGUYQwkNSd-Y28Z54eYxQXnrqaJBFn7K4cr5jB8Di13RGIHQ</xsl:param>
  cjsw.com: ABQIAAAA4IkGUae9pv9k9O1Ax3yJtBQkNZsALgOW3Cu1DiQ0F1czLfaJWhQn7ZurhZrs8fZnVpV_A07-RtuJfg
  admin.cjsw.com: ABQIAAAA4IkGUae9pv9k9O1Ax3yJtBSYCRLNe5ReAtdngaXHMHFHD2NUEhTv-prSoyf1DHTrrEUCrqb2dwDRRw
  -->
  <xsl:param name="gmapKey">ABQIAAAA4IkGUae9pv9k9O1Ax3yJtBQkNZsALgOW3Cu1DiQ0F1czLfaJWhQn7ZurhZrs8fZnVpV_A07-RtuJfg</xsl:param>
  <xsl:template match="listeners">
<html>
  <head>
    <title>Google Maps JavaScript API Example</title>
  <link rel="stylesheet" type="text/css" href="/css/stylesheet.css" />
    <script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key={$gmapKey}"
      type="text/javascript"></script>
    <script type="text/javascript">

	    
      function createMarker(point,icon,html) {
        var marker;
        if (icon)
          marker=new GMarker(point,icon);
        else
          marker=new GMarker(point);

        // The new marker "mouseover" listener        
        GEvent.addListener(marker,"mouseover", function() {
          document.getElementById("listenerInfo").innerHTML=html;
          setTimeout("summary()",5000);
        });        
        

        return marker;
      }

    function summary() {
  <xsl:variable name="listenerStr"><xsl:for-each select="location-connections/item"><xsl:for-each select="listeners/item">1</xsl:for-each></xsl:for-each></xsl:variable>
          document.getElementById("listenerInfo").innerHTML="<xsl:value-of select="string-length($listenerStr)"/> &lt;a href='http://cjsw.com/listen.html'&gt;online&lt;/a&gt; listener<xsl:if test="string-length($listenerStr)&gt;1">s</xsl:if> from <xsl:value-of select="count(location-connections/item)"/> location<xsl:if test="count(location-connections/item)&gt;1">s around the world.</xsl:if>";
    }

    function load() {
      if (GBrowserIsCompatible()) {
        var map = new GMap2(document.getElementById("map"));
        
        var baseIcon = new GIcon();
        baseIcon.iconSize=new GSize(32,32);
        baseIcon.shadowSize=new GSize(56,32);
        baseIcon.iconAnchor=new GPoint(16,32);
        baseIcon.infoWindowAnchor=new GPoint(16,0);
          
	    var cjswIcon = new GIcon(baseIcon, "http://cjsw.com/cjsw_marker.png", null, "http://maps.google.com/mapfiles/kml/pal3/icon28s.png");
		cjswIcon.iconSize = new GSize(20, 34);
		cjswIcon.shadowSize = new GSize(37, 34);	    
	    var listenerIcon = new GIcon(baseIcon, "http://www.google.com/mapfiles/marker.png", null, "http://www.google.com/mapfiles/shadow50.png");
		listenerIcon.iconSize = new GSize(20, 34);
		listenerIcon.shadowSize = new GSize(37, 34);	    
        var calgary=new GLatLng(<xsl:value-of select="center/latitude"/>,<xsl:value-of select="center/longitude"/>);
		
		map.addControl(new GSmallMapControl());
        map.setCenter(calgary,2);
		map.setMapType(G_HYBRID_MAP);

		var bounds = new GLatLngBounds();
	<!-- dump all but cjsw marker -->
	<xsl:for-each select="location-connections/item">
	  <xsl:if test="../../center/latitude!=location/where/latitude and ../../center/longitude!=location/where/longitude">
		var where<xsl:value-of select="position()"/>=new GLatLng(<xsl:value-of select="location/where/latitude"/>,<xsl:value-of select="location/where/longitude"/>);
		<xsl:variable name="html"><xsl:value-of select="count(listeners/item)"/> Listening from <xsl:value-of select="location/city"/>, <xsl:value-of select="location/region"/>&amp;#160; <xsl:value-of select="location/country"/></xsl:variable>
		var marker<xsl:value-of select="position()"/> = createMarker(where<xsl:value-of select="position()"/>,listenerIcon,'<xsl:value-of select="$html"/>');
		bounds.extend(where<xsl:value-of select="position()"/>); 
 		map.addOverlay(marker<xsl:value-of select="position()"/>);
	  </xsl:if>
	</xsl:for-each>       
	<!-- dump cjsw marker, having it in front -->
	<xsl:for-each select="location-connections/item">
	  <xsl:if test="../../center/latitude=location/where/latitude and ../../center/longitude=location/where/longitude">
		var where<xsl:value-of select="position()"/>=new GLatLng(<xsl:value-of select="location/where/latitude"/>,<xsl:value-of select="location/where/longitude"/>);
		<xsl:variable name="html"><xsl:value-of select="count(listeners/item)"/> Listening from <xsl:value-of select="location/city"/>, <xsl:value-of select="location/region"/>&amp;#160; <xsl:value-of select="location/country"/></xsl:variable>
		var marker<xsl:value-of select="position()"/> = createMarker(where<xsl:value-of select="position()"/>,cjswIcon,'<xsl:value-of select="$html"/>');
		bounds.extend(where<xsl:value-of select="position()"/>); 
 		map.addOverlay(marker<xsl:value-of select="position()"/>);
	  </xsl:if>
	</xsl:for-each>
		map.setZoom(map.getBoundsZoomLevel(bounds));
		map.setCenter(bounds.getCenter());
	
       }
       summary();
    }

setTimeout('window.location="'+document.location.href+'"',2*60*1000); // 2min refresh
    </script>
  </head>
  <body onload="load()" onunload="GUnload()" style="margin: 0px">
  <center>
    <div id="map" style="width: 450px; height: 200px; border: 1px solid black"></div>
    <div id="listenerInfo"><b></b></div>
  </center>
  </body>
</html>
  </xsl:template>

</xsl:stylesheet>
