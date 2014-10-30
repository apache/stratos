package org.apache.stratos.cloud.controller.pojo;

import java.io.Serializable;
import java.util.List;



public class ConfigGroup  implements Serializable {
	private  String alias;
	private  String[] subscribables;
	private ConfigDependencies dependencies;
	private static final long serialVersionUID = 1L;
	
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	public String[] getSubscribables() {
		return subscribables;
	}
	public void setSubscribables(String[] subscribables) {
		this.subscribables = subscribables;
	}
	public ConfigDependencies getDependencies() {
		return dependencies;
	}
	public void setDependencies(ConfigDependencies dependencies) {
		this.dependencies = dependencies;
	}


	
}
