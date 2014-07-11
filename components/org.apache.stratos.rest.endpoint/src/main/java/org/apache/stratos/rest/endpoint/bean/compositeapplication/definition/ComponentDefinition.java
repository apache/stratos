package org.apache.stratos.rest.endpoint.bean.compositeapplication.definition;

import org.apache.stratos.manager.composite.application.beans.SubscribableInfo;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "components")
public class ComponentDefinition {
    public String group;
	public String alias;
	public List<SubscribableInfo> subscribables;
	public ConfigDependencies dependencies;
    public String deploymentPolicy;
    public String autoscalingPolicy;
	
	/*
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
	*/

	
}
