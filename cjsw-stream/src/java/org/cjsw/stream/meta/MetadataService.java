package org.cjsw.stream.meta;

import ca.benow.service.Service;

public interface MetadataService extends Service {
	/**
	 * Sets metadata across all streams
	 * 
	 * @param meta
	 */
	public void setMetadata(String song);

	public void setMetadata(String mount, String song);
}
