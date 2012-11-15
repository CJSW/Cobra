/**
 * 
 */
package org.cjsw.stream.archive;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;

import org.apache.log4j.Logger;

import ca.benow.java.notify.Listener;
import ca.benow.java.notify.event.ErrorEvent;
import ca.benow.java.notify.event.Event;
import ca.benow.java.notify.event.FinishEvent;
import ca.benow.java.util.TextUtil;

public class URLRecorder extends Thread {

  private static final Logger log = Logger.getLogger(URLRecorder.class);
  private boolean running;
  public final URL url;
  public final File outFile;
  public final Listener listener;
  public final long endTime;
  public final String showTitle;
  private final String title;

  public URLRecorder(Listener listener, String title, URL srcURL, File outputFile, long endTime, String showTitle) {
    super();
    this.title = title;
    this.url = srcURL;
    this.outFile = outputFile;
    this.listener = listener;
    this.endTime = endTime;
    this.showTitle = showTitle;
    this.start();
  }

  @Override
  public void run() {
    this.running = true;
    try {
      boolean restart = false;
      do {
        restart = false;
        if (outFile.exists()) {
          String prefix = outFile.getName();
          String ext = "";
          int pos = outFile.getName().lastIndexOf(".");
          if (pos > -1) {
            prefix = outFile.getName().substring(0, pos);
            ext = outFile.getName().substring(pos + 1);
          }
          int count = 1;
          File moveFile = new File(outFile.getParentFile(), prefix + "-" + (count++) + "." + ext);
          while (moveFile.exists())
            moveFile = new File(outFile.getParentFile(), prefix + "-" + (count++) + "." + ext);
          log.info("Renamed existing to: " + moveFile.getAbsolutePath());
          outFile.renameTo(moveFile);
        }

        log.info("Starting recording of " + title + " from: " + url + " to: " + outFile.getAbsolutePath() + " until: "
            + new Date(endTime));
        InputStream in = new BufferedInputStream(url.openStream());
        OutputStream out = new BufferedOutputStream(new FileOutputStream(outFile, true));
        byte[] buff = new byte[1024];
        try {
          int numRead = in.read(buff);
          int count = 0;
          long lastTime = System.currentTimeMillis();
          while (numRead > 0 && running) {
            out.write(buff, 0, numRead);
            numRead = in.read(buff);
            if (running) {
              running = System.currentTimeMillis() < endTime;
              count++;
              if (count % 10000 == 0) {
                log.info("URLRecoder still left: " + TextUtil.millisToString(endTime - System.currentTimeMillis()));

                // this should occur roughly every 5mins.  If the time is unexpectedly
                // different from the last time, there's been a time change (DST most likely)
                // which has been known to screw up the recording, so stop and start a new file.
                long now = System.currentTimeMillis();
                if (now - lastTime > 10 * 60 * 1000 || lastTime > now) {
                  log.warn("Time drift detected, restarting.");
                  restart = true;
                  break;
                }
                lastTime = now;
              }
              if (!running)
                log.info(title + " is at an end.  Another great show, I'm sure.");
            }
          }
        } finally {
          in.close();
          out.close();
        }
        log.info("Finished recording from: " + url + " to: " + outFile.getAbsolutePath());
      } while (restart);

      log.info("Finished processing: " + outFile.getAbsolutePath());
    } catch (IOException e) {
      log.error(
          "Error while recording from: " + url + " to: " + outFile == null ? "unknown" : outFile.getAbsolutePath(), e);
      notify(new ErrorEvent(e));
    } finally {
      running = false;
      notify(new FinishEvent());
    }
  }

  private void notify(Event event) {
    if (listener != null)
      listener.onEvent(this, event);
  }

  public void quit() {
    this.running = false;
  }
}