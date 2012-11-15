package org.cjsw;

import javax.mail.MessagingException;

import ca.benow.java.config.ConfigurationEntry.Relevance;
import ca.benow.java.config.entry.StringConfigurationEntry;
import ca.benow.mail.MailUtil;

public class CJSW {
  public static StringConfigurationEntry CFG_ADMIN_EMAIL = new StringConfigurationEntry("adminEmail", "andy@benow.ca",
      "Administrator email, where run error mails are sent").setLevel(Relevance.Required);

  public static void sendAdminMail(String title, String body) {
    try {
      MailUtil.sendMail(org.cjsw.CJSW.CFG_ADMIN_EMAIL.getStringValue(), "CJSW: " + title, body);
    } catch (MessagingException ee) {
      ee.printStackTrace();
    }
  }

}
