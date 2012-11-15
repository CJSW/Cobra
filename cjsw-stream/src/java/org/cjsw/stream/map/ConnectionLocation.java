package org.cjsw.stream.map;

import java.util.ArrayList;
import java.util.Collection;


import ca.benow.java.annotations.Required;
import ca.benow.java.annotations.xml.XMLNodeType;
import ca.benow.meta.gis.Location;


public class ConnectionLocation {

  @Required
  public Location location;
  @XMLNodeType(XMLNodeType.ATTRIBUTE)
  public int count = 0;
  private Collection<Connection> listeners = new ArrayList<Connection>();

  protected ConnectionLocation() {
    super();
  }

  public ConnectionLocation(Location location) {
    this.location = location;
  }

  public void add(Connection listener) {
    listeners.add(listener);
  }

}
