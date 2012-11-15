package org.cjsw.stream.archive;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.sun.syndication.feed.rss.Enclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

public class RSSMaker {

  private static final Logger log = Logger.getLogger(RSSMaker.class);

  /**
   * @param args
   */
  public static void main(String[] args) {
    BasicConfigurator.configure();

    File baseDir;
    if (args.length == 0) {
      baseDir = new File("/data/archive.cjsw.com/");
    } else
      baseDir = new File(args[0]);

    if (!baseDir.exists()) {
      System.out.println("Base directory does not exist: " + baseDir.getAbsolutePath());
      System.out.println("Usage: java RSSMaker <dir>");
      System.exit(-1);
    }

    try {
      URL baseURL = new URL("http://archive.cjsw.com");
      File showsDir = new File(baseDir, "shows");
      for (File curr : showsDir.listFiles()) {
        if (curr.isDirectory()) {
          createRSSs(curr, new URL(baseURL, "/shows/" + curr.getName()));
        }
      }
      File daysDir = new File(baseDir, "days");
      for (File curr : daysDir.listFiles()) {
        if (curr.isDirectory()) {
          createRSSs(curr, new URL(baseURL, "/days/" + curr.getName()));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-2);
    }

  }

  public static void createRSSs(File currDir, URL currURL) throws IOException, FeedException {
    Map<Long, File> oggFilesByTimestamp = new TreeMap<Long, File>();
    Map<Long, File> mp3FilesByTimestamp = new TreeMap<Long, File>();

    for (File curr : currDir.listFiles()) {
      if (curr.isFile()) {
        if (curr.getName().endsWith(".ogg"))
          oggFilesByTimestamp.put(curr.lastModified(), curr);
        else if (curr.getName().endsWith(".mp3"))
          mp3FilesByTimestamp.put(curr.lastModified(), curr);
      }
    }

    // invert for descending time
    List<Long> keys = new ArrayList<Long>(oggFilesByTimestamp.keySet());
    List<File> oggFilesInOrder = new ArrayList<File>();
    for (int i = keys.size() - 1; i >= 0; i--)
      oggFilesInOrder.add(oggFilesByTimestamp.get(keys.get(i)));

    keys = new ArrayList<Long>(oggFilesByTimestamp.keySet());
    List<File> mp3FilesInOrder = new ArrayList<File>();
    for (int i = keys.size() - 1; i >= 0; i--)
      mp3FilesInOrder.add(oggFilesByTimestamp.get(keys.get(i)));

    URL showURL = new URL(currURL, currDir.getName() + "/");
    File feedFile = new File(currDir, "feed-ogg.rss");
    dumpFeed(feedFile, oggFilesInOrder, currDir.getName() + " (OGG Feed)", showURL, "audio/ogg");
    feedFile = new File(currDir, "feed-mp3.rss");
    dumpFeed(feedFile, mp3FilesInOrder, currDir.getName() + " (MP3 Feed)", showURL, "audio/mpeg");
  }

  private static void dumpFeed(File feedFile, List<File> filesInOrder, String title, URL baseURL, String type)
      throws IOException, FeedException {
    SyndFeed feed = new SyndFeedImpl();
    // (rss_0.90, rss_0.91, rss_0.92, rss_0.93, rss_0.94, rss_1.0 rss_2.0 or atom_0.3)
    feed.setFeedType("rss_2.0");
    feed.setDescription("RSS feed of archived CJSW broadcasts.");
    feed.setTitle(title);
    feed.setLink(baseURL.toString());

    List<SyndEntry> entries = new ArrayList<SyndEntry>();
    for (File curr : filesInOrder) {
      SyndEntry entry = new SyndEntryImpl();
      entry.setTitle(curr.getName());
      entry.setAuthor("CJSW, University of Calgary Radio");
      entry.setPublishedDate(new Date(curr.lastModified()));
      entry.setLink(new URL(baseURL, curr.getName()).toString());
      List<Enclosure> enclosures = new ArrayList<Enclosure>();
      Enclosure enc = new Enclosure();
      enc.setLength(curr.length());
      enc.setType(type);
      enc.setUrl(new URL(baseURL, curr.getName()).toString());
      entry.setEnclosures(enclosures);
      entries.add(entry);
    }
    feed.setEntries(entries);
    SyndFeedOutput output = new SyndFeedOutput();
    output.output(feed, new FileWriter(feedFile));
    log.info("Wrote feed: " + feedFile.getAbsolutePath());
  }

}
