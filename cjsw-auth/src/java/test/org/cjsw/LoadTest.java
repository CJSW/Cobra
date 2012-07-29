package test.org.cjsw;

import org.apache.log4j.Logger;

import ca.benow.java.log.LogManager;

public class LoadTest {

	public static Logger log = LogManager.declareLogger();

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			Class.forName("ca.benow.web.path.page.StyleXMLPlugin");
			log.info("loaded");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
