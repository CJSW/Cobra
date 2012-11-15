package org.cjsw.stream.meta;

import java.util.Date;

import org.apache.log4j.Logger;
import org.cjsw.API;
import org.cjsw.CJSW;
import org.json.JSONObject;

import ca.benow.deploy.service.ServiceApplication;
import ca.benow.java.spec.argument.ArgumentContext;
import ca.benow.java.util.StackUtil;

/**
 * - staggered offset - tagging? - pre/post-amble
 * 
 * @author andy
 * 
 */
public class MetadataSetter extends ServiceApplication {

  public static void main(String[] args) {
    new MetadataSetter().run(args);
  }

  private static final Logger log = Logger.getLogger(MetadataSetter.class);
  private MetadataService msvc;
  private final API api = new API();

  public MetadataSetter() {
    super("CJSW Stream Metadata Setter");
  }

  @Override
  protected void run(ArgumentContext ctx) throws Throwable {
    super.run(ctx);

    while (true) {
      try {
        msvc = new MetadataServiceImpl();
        JSONObject currentShow = api.getCurrentShow();
        JSONObject currBlock = currentShow.getJSONObject("airings").getJSONObject("current");
        int pgEnd = currBlock.getInt("pg_end");
        int now = currentShow.getInt("now");
        long waitUntil = System.currentTimeMillis() + ((pgEnd - now) * 60 * 1000);

        JSONObject nextShow = api.getNextShow();

        setMetadata(currentShow);
        log.info("Waiting until next update at: " + new Date(waitUntil));
        while (System.currentTimeMillis() < waitUntil) {
          Thread.sleep(5000);
        }

      } catch (Throwable t) {

        log.warn("Error setting metadata: ", t);
        CJSW.sendAdminMail("CJSW: metadata setter is down",
            "Metadata setter error at " + new Date() + "\n" + StackUtil.getStackTrace(t));
        Thread.sleep(5 * 60 * 1000);
      }
    }
  }

  /**
   * Set the metadata to the current show
   * 
   * @param currentShow
   */
  private void setMetadata(JSONObject currentShow) {
    String meta = getShowNameMetadata(currentShow);
    msvc.setMetadata(meta);
  }

  private String getShowNameMetadata(JSONObject currentShow) {
    String title = currentShow.getString("title");
    String programmer = currentShow.getString("hosts");
    return "CJSW - " + title + (programmer != null ? " (" + programmer + ")" : "") + " - cjsw.com";
  }

}