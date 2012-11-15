package org.cjsw.stream.archive;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cjsw.API;
import org.cjsw.CJSW;
import org.json.JSONException;
import org.json.JSONObject;

import ca.benow.deploy.service.ServiceApplication;
import ca.benow.java.config.entry.IntegerConfigurationEntry;
import ca.benow.java.notify.Listener;
import ca.benow.java.notify.event.Event;
import ca.benow.java.run.SimpleRunner;
import ca.benow.java.spec.argument.Argument;
import ca.benow.java.spec.argument.ArgumentContext;
import ca.benow.java.spec.argument.ArgumentSpecification;
import ca.benow.java.spec.argument.FileArgument;
import ca.benow.java.util.StackUtil;
import ca.benow.java.util.TextUtil;
import ca.benow.java.util.Util;

/**
 * - staggered offset - tagging? - pre/post-amble
 * 
 * @author andy
 * 
 */
public class Archiver extends ServiceApplication implements Listener {

  private static final SimpleDateFormat SHOW_TIME_FORMATTER = new SimpleDateFormat("HH:mm:SS");
  private static final SimpleDateFormat SHOW_DATE_FORMATTER = new SimpleDateFormat("MMM-dd-yyyy");
  private static final Logger log = Logger.getLogger(Archiver.class);

  public static final String[] DAYS = new String[] { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
      "Saturday" };
  public static final List<Integer> CAL_DAYS = new ArrayList<Integer>();
  private static final String[] RESERVED_STRINGS = new String[] { "?", "\n", "\r", "'", "\"", ":", "/", ".." };
  private static final IntegerConfigurationEntry CFG_TRIM_BYTES = new IntegerConfigurationEntry("trimBytes",
      1024 * 1024 * 20, "Trim older files when less than this many bytes free in capture directory.");

  static {
    // calendar in cjsw order
    CAL_DAYS.add(Calendar.SUNDAY);
    CAL_DAYS.add(Calendar.MONDAY);
    CAL_DAYS.add(Calendar.TUESDAY);
    CAL_DAYS.add(Calendar.WEDNESDAY);
    CAL_DAYS.add(Calendar.THURSDAY);
    CAL_DAYS.add(Calendar.FRIDAY);
    CAL_DAYS.add(Calendar.SATURDAY);
  }

  public static void main(String[] args) {
    new Archiver().run(args);
  }

  private FileArgument baseDirArg;

  private final List<URLRecorder> recorders = new Vector<URLRecorder>();
  private File baseDir;
  private final API api = new API();
  private Argument baseURLArg;
  private Argument streamArg;
  private Argument trimArg;
  // populated with the 10 oldest deletable files, which are deleted 
  // when this disk gets full (free space < CFG_TRIM_BYTES
  private final List<File> nextToTrim = new ArrayList<File>();
  private static File lockFile = new File("archiver.lock");

  public Archiver() {
    super("CJSW Stream Archiver");
  }

  @Override
  protected void specifyArguments(ArgumentSpecification spec) {
    super.specifyArguments(spec);

    baseDirArg = (FileArgument) spec.specArg(new String[] { "--out", "-o" }, "Root output directory", File.class);
    baseDirArg.setMustBeDirectory();
    baseDirArg.setInstanceValue("/data/archive.cjsw.com");
    baseURLArg = spec.specArg(new String[] { "--url", "-u" }, "Base external URL", URL.class);
    baseURLArg.setInstanceValue("http://archive.cjsw.com");

    streamArg = spec.specArg("--stream", "One or more (in csv format) urls of streams to record");
    streamArg.setInstanceValue("http://localhost:8000/cjsw.mp3");

    trimArg = spec.specArg("--trim", "Trim oldest files from within output directory where there are less than "
        + CFG_TRIM_BYTES.getIntValue() + " bytes free.");
    trimArg.setIsUnary();
  }

  @Override
  protected void run(ArgumentContext ctx) throws Throwable {
    super.run(ctx);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        for (URLRecorder rec : recorders) {
          rec.quit();
        }
        System.out.println("Archiver shutdown at: " + new Date());
      }
    });

    if (lockFile.exists()) {
      if (System.currentTimeMillis() - lockFile.lastModified() > 10 * 1000) {
        System.out.println("Stale lockfile found, running anyway");
        lockFile.delete();
      } else {
        System.err.println("Lock file exists and is recent: " + lockFile.getAbsolutePath()
            + "  Please shutdown other instances first or delete lock file.");
        System.exit(-1);
      }
    }

    boolean trim = trimArg.isProvided(ctx);

    CJSW.sendAdminMail("Archiver started", "Archiver began running at " + new Date());

    PrintWriter out = new PrintWriter(new FileWriter(lockFile));
    out.println("Locked at: " + new Date());
    out.flush();
    out.close();
    lockFile.deleteOnExit();

    String[] streamsStr = streamArg.getStringValues(ctx);
    URL[] streamURLs = new URL[streamsStr.length];
    for (int i = 0; i < streamURLs.length; i++)
      streamURLs[i] = new URL(streamsStr[i]);

    this.baseDir = baseDirArg.getFileValue(ctx);
    final URL baseURL = new URL(baseURLArg.getStringValue(ctx));

    while (true) {
      try {
        if (trim && baseDir.getFreeSpace() < CFG_TRIM_BYTES.getIntValue())
          performTrim();

        if (baseDir.getFreeSpace() < CFG_TRIM_BYTES.getIntValue())
          failDiskFull();

        JSONObject currentShow = api.getCurrentShow();
        String title = currentShow.getString("title");

        long nextStartIn = getRemainingMillis(currentShow);
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MILLISECOND, (int) nextStartIn);

        long localStartTime = cal.getTimeInMillis();
        long localEndTime = localStartTime;
        // add 2mins to show end
        localEndTime += 2 * 60 * 1000;

        // add 30s to prevent metadata change throwing off ignorant clients
        localStartTime += 30 * 1000;

        final File dayBaseDir = getDayBaseDir();
        if (!dayBaseDir.canWrite()) {
          System.err.println("Cant write to: " + dayBaseDir.getAbsolutePath() + "!");
          System.exit(-1);
        }

        File showBaseDir = null;
        if (canLink())
          showBaseDir = getShowBaseDir(currentShow);
        final File fShowBaseDir = showBaseDir;
        String prefix = getFilePrefix(currentShow);

        for (URL streamURL : streamURLs) {
          String suffix = "mp3";
          int pos = streamURL.getFile().indexOf(".");
          if (pos == -1)
            System.err.println("Could not find file type in stream url: " + streamURL + ".  Assuming " + suffix);
          else
            suffix = streamURL.getFile().substring(pos + 1);
          File outFile = new File(dayBaseDir, prefix + "." + suffix);
          if (canLink())
            doLink(outFile.getAbsolutePath(), showBaseDir.getAbsolutePath());
          recorders.add(new URLRecorder(this, title, streamURL, outFile, localEndTime, prefix));
        }

        // sleep just for display sync
        Thread.sleep(5000);
        log.info(title + " is recording.  Give'r "
            + (currentShow.getString("hosts") != null ? currentShow.getString("hosts") : "DJ") + "!");

        log.info("Waiting for next show at: " + new Date(localStartTime));
        int count = 0;
        while (System.currentTimeMillis() < localStartTime) {
          lockFile.setLastModified(System.currentTimeMillis());
          Thread.sleep(5000);
          count++;
          // every 5 min, spew status
          if (count % 60 == 0) {
            log.info("To wait: " + TextUtil.millisToString(localStartTime - System.currentTimeMillis()));
          }
        }
        log.info("show ended");

        // in a thread to avoid delay
        new Thread() {
          @Override
          public void run() {
            try {
              if (canLink())
                RSSMaker.createRSSs(fShowBaseDir, new URL(baseURL, "/shows/" + fShowBaseDir.getName()));
              RSSMaker.createRSSs(dayBaseDir, new URL(baseURL, "/shows/" + dayBaseDir.getName()));
              log.info("wrote rss");
            } catch (Exception e) {
              log.warn("Error generating feed.", e);
              CJSW.sendAdminMail("error generating feed",
                  "Archiver error at " + new Date() + "\n" + StackUtil.getStackTrace(e));
            }
          }
        }.start();

        log.info("getting next show");
        while (true) {
          try {
            currentShow = api.getCurrentShow();
            break;
          } catch (Throwable t) {
            log.warn("Error getting show, retrying in 5s");
            Thread.sleep(5000);
          }
        }
        if (currentShow == null)
          log.info("no next show??!");
        else
          log.info("Next show is: " + currentShow.getString("title"));
      } catch (Throwable t) {

        log.warn("Error on archiver: ", t);
        CJSW.sendAdminMail("Archiver had error",
            "Archiver error at " + new Date() + ".  Sleeping\n" + StackUtil.getStackTrace(t));
        Thread.sleep(5 * 60 * 1000);
      }
    }// while
  }

  private long getRemainingMillis(JSONObject currentShow) {
    try {
      JSONObject currentBlock = currentShow.getJSONObject("airings").getJSONObject("current");
      int pgEnd = currentBlock.getInt("pg_end");
      int now = currentShow.getInt("now");
      return (pgEnd - now) * 60 * 1000;
    } catch (JSONException e) {
      log.error("Error parsing airings.current from: " + currentShow.toString(), e);
      log.warn("Defaulting to 30m show length");
      return 30 * 60 * 1000;
    }
  }

  private void failDiskFull() {
    String subject = "CRITICAL: Archiver is out of space";
    String body = "Archiver out of space at " + new Date() + ".  There are " + baseDir.getFreeSpace()
        + " bytes free in " + baseDir.getAbsolutePath() + " (<" + CFG_TRIM_BYTES.getIntValue() + ") .\nExited!";
    log.error(subject + ": " + body);
    CJSW.sendAdminMail(subject, body);
    System.exit(-128);
  }

  private void performTrim() throws IOException {
    if (nextToTrim.isEmpty()) {
      findNextToTrim();
      if (nextToTrim.isEmpty()) {
        log.error("Could not find any files to trim!");
        failDiskFull();
      }
    }
    File toDel = nextToTrim.remove(0);
    log.info("Removed oldest file: " + toDel.getAbsolutePath() + " (" + new Date(toDel.lastModified()) + ", "
        + toDel.length() + " bytes)");
    toDel.delete();
    deleteIfEmpty(toDel.getCanonicalFile().getParentFile());
  }

  private void deleteIfEmpty(File parentFile) {
    if (parentFile.listFiles().length == 0) {
      log.info("Deleted empty directory: " + parentFile.getAbsolutePath());
      parentFile.delete();
      deleteIfEmpty(parentFile.getParentFile());
    }
  }

  private void findNextToTrim() {
    nextToTrim.clear();
    // find the oldest 10 files in the output directory
    findNextToTrim(baseDir);

    // sort by date
    Map<Long, File> oldestSort = new TreeMap<Long, File>();
    for (File curr : nextToTrim)
      oldestSort.put(curr.lastModified(), curr);
    nextToTrim.clear();
    nextToTrim.addAll(oldestSort.values());

    String msg = "";
    for (File curr : nextToTrim)
      msg += "\t" + new Date(curr.lastModified()) + " " + curr.getAbsolutePath() + "\n";
    System.out.println("Found oldest files to trim:\n" + msg);
  }

  private void findNextToTrim(File currDir) {
    for (File currFile : currDir.listFiles()) {
      if (currFile.isFile()) {
        if (nextToTrim.size() <= 10) {
          nextToTrim.add(currFile);
        } else {
          for (File inFile : nextToTrim) {
            if (inFile.lastModified() > currFile.lastModified()) {
              nextToTrim.remove(inFile);
              nextToTrim.add(currFile);
              break;
            }
          }
        }
      }
    }

    for (File currFile : currDir.listFiles()) {
      if (currFile.isDirectory()) {
        findNextToTrim(currFile);
      }
    }
  }

  private void doLink(String src, String dest) {
    if (canLink()) {
      String[] cmdAry = new String[] { "ln", "-sf", src, dest };
      int exit = -1;
      try {
        log.info("Linking into show dir with: " + Util.concat(cmdAry, " "));

        exit = new SimpleRunner(cmdAry).run();
      } catch (Exception e) {
        log.warn("Error linking to show dir with: " + Util.concat(cmdAry, " ") + "  Exited with: " + exit);
      }
    }
  }

  private boolean canLink() {
    return Util.isLinux();
  }

  private File getShowBaseDir(JSONObject currentShow) {
    File showBase = new File(baseDir, "shows/" + getShowAsFileName(currentShow));
    if (!showBase.exists())
      showBase.mkdirs();
    return showBase;
  }

  private String getShowAsFileName(JSONObject currentShow) {
    String programmer = currentShow.getString("hosts");
    if (programmer != null) {
      programmer = programmer.trim();
      if (programmer.length() == 0)
        programmer = null;
    }
    String name = currentShow.getString("title") + (programmer != null ? " (" + programmer + ")" : "");
    try {
      name = new String(name.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    for (String curr : RESERVED_STRINGS)
      name = name.replace(curr, "");
    return name;
  }

  private File getDayBaseDir() {
    File dayBaseDir = new File(baseDir, "days/" + getCurrentDay());
    if (!dayBaseDir.exists())
      dayBaseDir.mkdirs();
    return dayBaseDir;
  }

  protected String getFilePrefix(JSONObject show) {
    String dateFMT = SHOW_DATE_FORMATTER.format(new Date());
    return getShowAsFileName(show) + " - " + dateFMT + " (CJSW-90.9FM-Calgary-cjsw.com)";
  }

  @Override
  public boolean onEvent(Object notifier, Event event) {
    log.debug("Event: " + event + " from: " + notifier);
    if (notifier instanceof URLRecorder) {
      URLRecorder recorder = ((URLRecorder) notifier);
      recorders.remove(recorder);
    }
    return true; // keep getting notified
  }

  public String getCurrentDay() {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    String day = DAYS[CAL_DAYS.indexOf(cal.get(Calendar.DAY_OF_WEEK))];
    return day;
  }
}