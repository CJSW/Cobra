package org.cjsw.stream.map;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.cjsw.CJSW;

import ca.benow.deploy.service.ServiceApplication;
import ca.benow.java.annotations.GetMethod;
import ca.benow.java.annotations.SetMethod;
import ca.benow.java.annotations.xml.XMLNodeType;
import ca.benow.java.mapping.MapTo.XMLMapTo;
import ca.benow.java.notify.Listener;
import ca.benow.java.notify.event.Event;
import ca.benow.java.spec.argument.ArgumentContext;
import ca.benow.java.spec.argument.ArgumentSpecification;
import ca.benow.java.spec.argument.FileArgument;
import ca.benow.java.spec.argument.IntegerArgument;
import ca.benow.java.text.ParseContext;
import ca.benow.java.throwable.NotImplementedException;
import ca.benow.java.util.IOUtil;
import ca.benow.java.util.StackUtil;
import ca.benow.meta.gis.GeoIP;
import ca.benow.meta.gis.Location;
import ca.benow.meta.gis.Point;
import ca.benow.repository.ObjectRepository;
import ca.benow.repository.ObjectRepositoryConnection;
import ca.benow.repository.ObjectRepositoryException;
import ca.benow.repository.Repo;
import ca.benow.repository.Repository;
import ca.benow.repository.RepositoryRunnable;
import ca.benow.repository.Transaction;
import ca.benow.xml.io.ClassTransformer;
import ca.benow.xml.xsl.XSL;
import ca.benow.xml.xsl.XSL.StyleSheetReloadEvent;

@XMLMapTo("listeners")
public class LocationPoller extends ServiceApplication implements Listener {

  public class ShutdownMarkerThread extends Thread {

    private final File outFile;

    public ShutdownMarkerThread(File outFile) {
      this.outFile = outFile;
    }

    @Override
    public void run() {
      /*
       * try { Util.copyFile(incFile, outFile); log.info("Replaced contents of "
       * + outFile.getAbsolutePath() + " with that of " +
       * incFile.getAbsolutePath()); } catch (IOException e) {
       * System.out.println("Error shutting down."); e.printStackTrace(); }
       */
      try {
        outFile.delete();
        PrintWriter out = new PrintWriter(new FileWriter(outFile));
        out.println();
        out.close();
      } catch (IOException e1) {
        e1.printStackTrace();
      }

      CJSW.sendAdminMail("CJSW: poller is down", "Location poller is down");
    }
  }

  private static Logger log = Logger.getLogger(LocationPoller.class);
  public static Point POINT_CALGARY = new Point(51.083302, -114.083298);
  public static Point POINT_CJSW = new Point(51.078559, -114.130622);
  public static Point POINT_UNKNOWN_CANADA = new Point(60.000000, -95.000000);

  // ips to ignore
  private static Collection<String> ignoreIPs = new ArrayList<String>();
  // map of points to remap to other points. allows for more precise location
  // spec
  public static Map<Point, Point> remaps = new HashMap<Point, Point>();
  static {
    // ignore stream.cjsw.com
    ignoreIPs.add("64.141.103.165");
    // ignore cjsw.com
    ignoreIPs.add("64.141.102.27");
    // ignore eng.cjsw.com
    ignoreIPs.add("136.159.250.12");
    // ignore eng.cjsw.com
    ignoreIPs.add("76.73.3.122");

    // remap calgary position to be exactly at cjsw
    remaps.put(POINT_CALGARY, POINT_CJSW);
    remaps.put(POINT_UNKNOWN_CANADA, POINT_CJSW);

  }

  public transient Map<Point, ConnectionLocation> listenerLocationsByPoint = new HashMap<Point, ConnectionLocation>();
  @GetMethod("getLocations")
  @SetMethod("setLocations")
  public Collection<ConnectionLocation> locationConnections;
  @XMLNodeType(XMLNodeType.ATTRIBUTE)
  public int total = 0;
  public Point center = POINT_CJSW;
  @XMLNodeType(XMLNodeType.ATTRIBUTE)
  private int interval;

  private transient File xslFile;
  private transient XSL xsl;
  private transient File outFile;
  private final Map<String, Long> errorMsgToTime = new HashMap<String, Long>();
  private FileArgument xslArg;
  private FileArgument htmlArg;
  private IntegerArgument intervalArg;

  public LocationPoller() {
    super("Polls stream for ips and resolves to listener locations on a map");
  }

  public Collection<ConnectionLocation> getLocations() {
    return listenerLocationsByPoint.values();
  }

  public void setLocations(Collection<ConnectionLocation> coll) {
    throw new NotImplementedException();
  }

  @Override
  protected void specifyArguments(ArgumentSpecification spec) {
    super.specifyArguments(spec);
    xslArg = (FileArgument) spec.specArg(new String[] { "--xsl" }, "XSL file transform to produce html output.",
        File.class);
    xslArg.setInstanceValue("var/xsl/toGMap.xsl");
    htmlArg = (FileArgument) spec.specArg(new String[] { "--out" }, "Name of destination file.", File.class);
    htmlArg.setInstanceValue("html/listenerMap.html");
    intervalArg = (IntegerArgument) spec.specArg("--interval", "Time (in s) to wait between rechecks", Integer.class);
    intervalArg.setInstanceValue(2 * 60);
  }

  @Override
  protected void run(ArgumentContext ctx) throws Throwable {
    super.run(ctx);

    this.interval = intervalArg.getIntValue(ctx);
    this.xslFile = xslArg.getFileValue(ctx);
    this.outFile = htmlArg.getFileValue(ctx);
    xsl = XSL.getXSL(xslFile, this);

    ObjectRepository repo = Repo.get();
    ObjectRepositoryConnection conn = repo.takeConnection();
    Transaction tx = conn.getTransaction();
    try {
      run(conn, tx);
    } catch (Throwable t) {
      CJSW.sendAdminMail("CJSW: poller is down: " + t.getMessage(), "Location poller is down after error:\n"
          + StackUtil.getStackTrace(t));
      throw t;
    } finally {
      tx.commitTransaction();
      conn.returnToRepository();
    }
  }

  protected void run(ObjectRepositoryConnection conn, Transaction tx) throws Exception {
    log.info("Outputting to: " + outFile.getAbsolutePath());

    Runtime.getRuntime().addShutdownHook(new ShutdownMarkerThread(outFile));
    /*
     * fails? if (!outFile.getParentFile().exists())
     * outFile.getParentFile().mkdirs();
     */
    while (true) {
      dumpMap(outFile, tx);

      Thread.sleep(interval * 1000);
    }
  }

  private void dumpMap(File outFile, Transaction tx) throws IOException, ObjectRepositoryException,
      TransformerException {
    List<String> ips = getIPs();
    loadLocations(ips, tx);

    StringWriter buff = new StringWriter();
    new ClassTransformer(this).to(buff);
    OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile));
    String xml = buff.toString();
    if (log.isDebugEnabled())
      log.debug("XML:\n" + xml + "\ntransforming with: " + xslFile.getAbsolutePath());
    xsl.transform(new StringReader(xml), out);
    log.info("Dumped to: " + outFile.getAbsolutePath());
    out.flush();
    out.close();
  }

  private void loadLocations(List<String> ips, Transaction tx) throws ObjectRepositoryException, IOException {
    total = 0;
    listenerLocationsByPoint.clear();
    for (Iterator<String> i = ips.iterator(); i.hasNext();) {
      String ip = i.next();

      if (ip != null && !ignoreIPs.contains(ip)) {
        Connection listener = new Connection(ip);
        // get location from ip, loading from db, if possible.
        Location location = GeoIP.resolveIP(ip, tx);
        for (Iterator<Point> r = remaps.keySet().iterator(); r.hasNext();) {
          Point key = r.next();
          if (location.where.equals(key))
            location.where = remaps.get(key);
        }

        ConnectionLocation listenerLoc;
        if (listenerLocationsByPoint.containsKey(location.where))
          listenerLoc = listenerLocationsByPoint.get(location.where);
        else {
          listenerLoc = new ConnectionLocation(location);
          listenerLocationsByPoint.put(location.where, listenerLoc);
        }
        listenerLoc.add(listener);
        listenerLoc.count++;

        total++;
      } else
        log.info("Ignoring: " + ip);
    }
    // remember ips
    tx.commitTransaction();
    tx = tx.getConnection().getTransaction();
  }

  private List<String> getIPs() throws IOException {
    List<String> ips = new ArrayList<String>();
    getIPs(ips, "http://stream1.cjsw.com/admin/listclients.xsl?mount=/cjsw.mp3");
    getIPs(ips, "http://stream1.cjsw.com/admin/listclients.xsl?mount=/cjsw.ogg");
    getIPs(ips, "http://stream1.cjsw.com/admin/listclients.xsl?mount=/cjsw-backup.mp3");
    getIPs(ips, "http://stream1.cjsw.com/admin/listclients.xsl?mount=/cjsw-backup.ogg");
    getIPs(ips, "http://stream1.cjsw.com/admin/listclients.xsl?mount=/cjsw-low.mp3");
    getIPs(ips, "http://stream1.cjsw.com/admin/listclients.xsl?mount=/cjsw-low.ogg");
    getIPs(ips, "http://stream2.cjsw.com:8000/admin/listclients.xsl?mount=/cjsw.mp3");
    getIPs(ips, "http://stream2.cjsw.com:8000/admin/listclients.xsl?mount=/cjsw.ogg");
    log.info("Fetched: " + ips.size());
    return ips;
  }

  private void getIPs(List<String> ips, String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    String encoding = new sun.misc.BASE64Encoder().encode("admin:str3am".getBytes());
    log.info("Adding header: Authorization: Basic " + encoding);
    conn.setRequestProperty("Authorization", "Basic " + encoding);
    try {
      log.debug("Reading ips from: " + urlStr);
      String read = IOUtil.readFromStream(conn.getInputStream());
      ParseContext ctx = new ParseContext(read);
      if (ctx.ifCanThenRepositionToAfter("IP")) {
        while (ctx.ifCanThenRepositionToAfter("<tr>")) {
          ctx.repositionToAfter("<td align=\"center\">");
          String ip = ctx.parseUntil("</td");
          log.debug("IP: " + ip);
          ips.add(ip);
        }
      }
      conn.disconnect();
    } catch (ParseException e) {
      log.warn("Error parsing from: " + urlStr, e);
    } catch (IOException e) {
      onDeadURL(urlStr, e.getMessage());
    }
  }

  protected void onDeadURL(String urlStr, String msg) {
    log.warn("Error connecting to: " + urlStr + "\n" + msg);
    Long time = errorMsgToTime.get(urlStr + msg);
    if (time == null) {
      CJSW.sendAdminMail("CJSW: stream is down: " + urlStr, "Stream expected at <a href=\"" + urlStr + "\">" + urlStr
          + "</a> could not be contacted.\nMessage: " + msg);
      errorMsgToTime.put(urlStr + msg, System.currentTimeMillis());
    } else if ((time.longValue() + (5 * 60 * 1000)) < System.currentTimeMillis()) {
      CJSW.sendAdminMail("CJSW: stream is still down: " + urlStr, "Stream expected at <a href=\"" + urlStr + "\">"
          + urlStr + "</a> still could not be contacted.\nMessage: " + msg);
      errorMsgToTime.put(urlStr + msg, System.currentTimeMillis());
    }
  }

  @Override
  public boolean onEvent(Object notifier, Event event) {
    log.info("Event: " + event.getClass().getName());
    if (event instanceof XSL.StyleSheetReloadEvent) {
      XSL.StyleSheetReloadEvent evt = (StyleSheetReloadEvent) event;
      try {
        Repository.exec(new RepositoryRunnable() {
          @Override
          public boolean run(Transaction tx) throws Throwable {
            dumpMap(outFile, tx);
            return true;
          }
        });
        log.info("Regenerated due to transformer change.");
      } catch (ObjectRepositoryException e) {
        log.error("Error regenerating after stylesheet change: " + evt.file.getAbsolutePath(), e);
      }
    } else
      log.warn("Unknown event:" + event.getClass().getName() + " from " + notifier);
    return true; // keep on keepin on
  }

  public static void main(String[] args) {
    new LocationPoller().run(args);
  }
}
