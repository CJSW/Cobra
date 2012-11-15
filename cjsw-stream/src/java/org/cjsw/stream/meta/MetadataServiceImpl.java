package org.cjsw.stream.meta;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;
import ca.benow.java.config.ConfigurationEntry.Relevance;
import ca.benow.java.config.entry.IntegerConfigurationEntry;
import ca.benow.java.config.entry.StringConfigurationEntry;
import ca.benow.java.util.IOUtil;

public class MetadataServiceImpl implements MetadataService {
  private static final Logger log = Logger.getLogger(MetadataServiceImpl.class);

  private static StringConfigurationEntry CFG_META_USER = new StringConfigurationEntry("user", "admin",
      "User for metadata update").setLevel(Relevance.Required);
  private static StringConfigurationEntry CFG_META_PASS = new StringConfigurationEntry("pass", "str3am",
      "Pass for metadata update").setLevel(Relevance.Required);
  private static StringConfigurationEntry CFG_META_HOST = new StringConfigurationEntry("host", "stream.cjsw.com",
      "Host running icecast for metadata update").setLevel(Relevance.Required);
  private static List<String> DEFAULT_MOUNTS = new ArrayList<String>();
  static {
    DEFAULT_MOUNTS.add("/cjsw.ogg");
    DEFAULT_MOUNTS.add("/cjsw-low.ogg");
    DEFAULT_MOUNTS.add("/cjsw.mp3");
    DEFAULT_MOUNTS.add("/cjsw-low.mp3");
  }
  private static StringConfigurationEntry CFG_META_STREAMS = new StringConfigurationEntry("mounts", DEFAULT_MOUNTS,
      "All mounts to be updated with blanket update").setLevel(Relevance.Required);
  private static IntegerConfigurationEntry CFG_META_PORT = new IntegerConfigurationEntry("port", 80, "Port for update")
      .setLevel(Relevance.Required);
  private static StringConfigurationEntry CFG_META_REALM = new StringConfigurationEntry("realm", "Icecast2 Server",
      "Http basic auth realm for icecast server").setLevel(Relevance.Required);

  private String metaUser = CFG_META_USER.getStringValue();
  private String metaPass = CFG_META_PASS.getStringValue();;
  private String metaHost = CFG_META_HOST.getStringValue();;
  private List<String> metaStreams = CFG_META_STREAMS.getStringValues();
  private int metaPort = CFG_META_PORT.getIntValue();
  private String metaRealm = CFG_META_REALM.getStringValue();;

  @Override
  public void setMetadata(String song) {
    // String urlPrefix = "http://" + metaHost + (metaPort != 80 ? ":" + metaPort : "")
    // + "/admin/metadata.xsl?song=" + meta + "&mode=updinfo&mount=%2F";

    System.out.print("Updating metadata.");
    System.out.flush();
    for (String mount : metaStreams) {
      setMetadata(mount, song);
      System.out.print(".");
      System.out.flush();
    }
    System.out.println("done.");
  }

  @Override
  public void setMetadata(String mount, String song) {
    String urlFile = "/admin/metadata.xsl";
    String urlStr = "http://" + metaHost + (metaPort != 80 ? ":" + metaPort : "") + urlFile;
    try {
      HTTPConnection hconn = new HTTPConnection(metaHost, metaPort);
      hconn.addBasicAuthorization(metaRealm, metaUser, metaPass);
      NVPair[] formParams = new NVPair[3];
      formParams[0] = new NVPair("mode", "updinfo");
      formParams[1] = new NVPair("song", song);
      formParams[2] = new NVPair("mount", mount);
      NVPair[] headers = new NVPair[] { new NVPair("User-Agent",
          "Mozilla/5.0 (X11; U; Linux x86_64; en-US; rv:1.9.0.10) Gecko/2009042523 Ubuntu/9.04 (jaunty) Firefox/3.0.10") };
      if (log.isDebugEnabled())
        log.debug("Updating metadata for " + mount + " at: " + urlStr);
      HTTPResponse resp = hconn.Get(urlFile, formParams, headers);
      if (!("" + resp.getStatusCode()).startsWith("2")) {
        InputStream inputStream = resp.getInputStream();
        String page = IOUtil.readFromStream(inputStream);
        log.error("Error updating metadata at: " + urlStr + ".  Code: " + resp.getStatusCode() + " Reply:\n" + page);
        inputStream.close();
      } else if (log.isDebugEnabled()) {
        InputStream inputStream = resp.getInputStream();
        String page = IOUtil.readFromStream(inputStream);
        // log.debug("Response from: " + urlStr + " code: " + resp.getStatusCode() + " body:\n" + page);
        inputStream.close();
      }
    } catch (Exception e) {
      log.error("Error updating metadata" + (urlStr != null ? " from " + urlStr : ""), e);
    }
  }

  public String getMetaUser() {
    return metaUser;
  }

  public void setMetaUser(String metaUser) {
    this.metaUser = metaUser;
  }

  public String getMetaPass() {
    return metaPass;
  }

  public void setMetaPass(String metaPass) {
    this.metaPass = metaPass;
  }

  public String getMetaHost() {
    return metaHost;
  }

  public void setMetaHost(String metaHost) {
    this.metaHost = metaHost;
  }

  public List<String> getMetaStreams() {
    return metaStreams;
  }

  public void setMetaStreams(List<String> metaStreams) {
    this.metaStreams = metaStreams;
  }

  public int getMetaPort() {
    return metaPort;
  }

  public void setMetaPort(int metaPort) {
    this.metaPort = metaPort;
  }

  public String getMetaRealm() {
    return metaRealm;
  }

  public void setMetaRealm(String metaRealm) {
    this.metaRealm = metaRealm;
  }

}
