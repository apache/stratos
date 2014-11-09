package org.apache.stratos.cloud.controller.pojo;

import java.io.Serializable;


public class ConfigCartridge  implements Serializable {
	private String alias;
	private static final long serialVersionUID = 1L;

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
}
