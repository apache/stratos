package org.apache.stratos.messaging.domain.topology;

import java.io.Serializable;
import java.util.List;


public class ConfigGroup   implements Serializable {
	private static final long serialVersionUID = 1L;
	private  String alias;
	private  List<String> subscribables;
	private ConfigDependencies dependencies;
	
	
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	public List<String> getSubscribables() {
		return subscribables;
	}
	public void setSubscribables(List<String> subscribables) {
		this.subscribables = subscribables;
	}
	public ConfigDependencies getDependencies() {
		return dependencies;
	}
	public void setDependencies(ConfigDependencies dependencies) {
		this.dependencies = dependencies;
	}
	
}
