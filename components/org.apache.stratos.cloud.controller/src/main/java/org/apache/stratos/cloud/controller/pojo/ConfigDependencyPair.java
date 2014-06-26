package org.apache.stratos.cloud.controller.pojo;

import java.io.Serializable;

public class ConfigDependencyPair  implements Serializable {
	private String key;
	private String value;
	private static final long serialVersionUID = 1L;
	
	public ConfigDependencyPair() {}
	
	public ConfigDependencyPair(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}