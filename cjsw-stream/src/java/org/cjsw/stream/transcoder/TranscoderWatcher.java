package org.cjsw.stream.transcoder;

import java.io.IOException;
import java.net.URL;

import ca.benow.deploy.service.ServiceApplication;
import ca.benow.java.run.SimpleRunner;
import ca.benow.java.spec.argument.Argument;
import ca.benow.java.spec.argument.ArgumentContext;
import ca.benow.java.spec.argument.ArgumentSpecification;
import ca.benow.java.util.IOUtil;

/**
 * Starts StreamTranscoderV3 and makes sure it stays up.  Checks
 * extenal source to make sure its up, restarting if not.  If
 * it goes down, restarts.
 * <p/>
 * Unfortunately, I don't think there's a better tool out there.
 * 
 * @author andy
 *
 */
public class TranscoderWatcher extends ServiceApplication {

  public class URLChecker extends Thread {
    private boolean running;
    private final Object mutex = "mutex";

    public URLChecker() {
      super("url-checker");
      setDaemon(true);
      start();
    }

    @Override
    public void run() {
      this.running = true;
      while (running) {
        // check
        try {
          String read = IOUtil.readFromURL(checkURL);
          for (String curr : checkValues) {
            if (!read.contains(curr)) {
              System.err.println("Could not find: " + curr + " at: " + checkURL + ".\nStopping process.");
              process.destroy();
              running = false;
            }
          }
        } catch (IOException e) {
          log.warn("Error contacting: " + checkURL + ": " + e.getMessage() + " (" + e.getClass().getName() + ")");
        }
        if (running) {
          synchronized (mutex) {
            try {
              mutex.wait(30 * 1000);
            } catch (InterruptedException e) {
              // eat
            }
          }
        }
      }
    }

    public void quit() {
      running = false;
      synchronized (mutex) {
        mutex.notifyAll();
      }
    }

  }

  private Argument appArg;
  private Argument checkURLArg;
  private Argument checkValueArg;
  private Process process;
  private String app;
  private URL checkURL;
  private String[] checkValues;
  private URLChecker urlChecker;

  public TranscoderWatcher() {
    super("Failsafe StreamTranscoderV3 wrapper");
  }

  @Override
  protected void specifyArguments(ArgumentSpecification spec) {
    super.specifyArguments(spec);

    this.appArg = spec.specArg("--app", "Path to application", "streamTranscoderV3");
    this.checkURLArg = spec.specArg("--check-url", "URL to check for values", "http://stream.cjsw.com/");
    this.checkValueArg = spec.specArg("--check-value", "Value(s) to find in URL, possibly separated by comma",
        "/cjsw-low.mp3,/cjsw-low.ogg");
  }

  @Override
  protected void run(ArgumentContext ctx) throws Throwable {
    super.run(ctx);

    this.app = appArg.getStringValue(ctx);
    this.checkURL = new URL(checkURLArg.getStringValue(ctx));
    this.checkValues = checkValueArg.getStringValues(ctx);

    startProcess();
    while (true) {
      int exit = process.waitFor();
      if (exit != 0) {
        System.err.println("Process exited with: " + exit + ".\nWaiting before restart");
        Thread.sleep(15 * 1000);
      }
      startProcess();
    }
  }

  private void startProcess() throws IOException, InterruptedException {
    if (urlChecker != null)
      urlChecker.quit();
    SimpleRunner r = new SimpleRunner(this.app);
    this.process = r.start();
    Thread.sleep(1000);
    if (!r.isRunning()) {
      System.out.println("Finished immediately.  Something is wrong, exiting.");
      System.exit(-1);
    }
    this.urlChecker = new URLChecker();
  }
}
