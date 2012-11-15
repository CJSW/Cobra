package org.cjsw;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.json.JSONObject;

public class API {

  /**
   * Gets current show from: <a href="http://cjsw.com/api.php?method=now_playing">http://cjsw.com/api.php?method=now_playing</a>.
   * <pre>
   * {
   *    "method":"now_playing",
   *    "id":326,
   *    "permalink":"http:\/\/cjsw.com\/program\/road-pops\/",
   *    "title":"Road Pops",
   *    "description":"Bringing you music and opinion since 1984, featuring the best in dub, 4:20 reggae, gypsy, afrobeat, latin, electronica, breakbeat, soul and funk.\r\n\r\nSponsored by FFWD Weekly<\/a>.",
   *    "airings": {
   *        "0":{
   *            "pg_start":8160,
   *            "pg_end":8280,
   *            "start_time":"16:00",
   *            "end_time":"18:00",
   *            "duration":"120",
   *            "day":"5",
   *            "time":"960"
   *         },
   *         "current":{
   *            "pg_start":8160,
   *            "pg_end":8280,
   *            "start_time":"16:00",
   *            "end_time":"18:00",
   *            "duration":"120",
   *            "day":"5",
   *            "time":"960"
   *         }
   *     },
   *     "hosts":"Kevin Brooker & Grant Burns",
   *     "now":8208,
   *     "next":"November 9, 2012 @ 4:00 pm",
   *     "type":"Mixed"
   * }
   * </pre>
   * @return
   * @throws IOException
   */
  public JSONObject getCurrentShow() throws IOException {
    URL url = new URL("http://cjsw.com/api.php?method=now_playing");
    return readObj(url);
  }

  /**
   * Gets next show from <a href="http://cjsw.com/api.php?method=playing_next">http://cjsw.com/api.php?method=playing_next</a>
   * <pre>
   * {
   *    "method":"playing_next",
   *    "id":327,
   *    "permalink":"http:\/\/cjsw.com\/program\/the-musiquarium\/",
   *    "title":"The Musiquarium",
   *    "description":"",
   *    "airings":[
   *    {
   *        "pg_start":8280,
   *        "pg_end":8340,
   *        "start_time":"18:00",
   *        "end_time":"19:00",
   *        "duration":"60",
   *        "day":"5",
   *        "time":"1080"
   *    }],
   *    "hosts":"Sideshow Sid",
   *    "now":8204,
   *    "next":"November 9, 2012 @ 6:00 pm",
   *    "type":"Beats"
   * }
   * </pre>
   * @return
   * @throws IOException
   */
  public JSONObject getNextShow() throws IOException {
    URL url = new URL("http://cjsw.com/api.php?method=playing_next");
    return readObj(url);
  }

  private JSONObject readObj(URL url) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
    try {
      String wholeStr = "";
      String read = in.readLine();
      while (read != null) {
        wholeStr += read + "\n";
        read = in.readLine();
      }
      System.out.println(url + ":\n" + wholeStr);
      return new JSONObject(wholeStr);
    } finally {
      in.close();
    }
  }

}
