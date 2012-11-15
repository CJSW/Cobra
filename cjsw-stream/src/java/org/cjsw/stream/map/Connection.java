package org.cjsw.stream.map;

import java.io.IOException;


import ca.benow.java.annotations.Required;
import ca.benow.meta.gis.GeoIP;
import ca.benow.meta.gis.Location;
import ca.benow.repository.ObjectRepositoryException;
import ca.benow.repository.Transaction;
import ca.benow.security.user.User;

public class Connection {

	@Required
	public Location location;
	@Required
	public User user;
	public String ip;

	protected Connection() {
		super();
	}

	public Connection(String ip) {
		this.ip = ip;
	}

	public void resolveLocation(Transaction tx) throws IOException, ObjectRepositoryException {
		location = GeoIP.resolveIP(ip, tx);
	}

}
