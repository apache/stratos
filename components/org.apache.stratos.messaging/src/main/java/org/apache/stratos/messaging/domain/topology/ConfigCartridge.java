package org.apache.stratos.messaging.domain.topology;

import java.io.Serializable;


public class ConfigCartridge  implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String alias;

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
}
