package org.apache.stratos.cloud.controller.pojo;

import java.io.Serializable;

public class CompositeApplicationDefinition  implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private String applicationId;
	private String alias;
	private ConfigGroup[] components;
	private ConfigCartridge [] cartridges;
	public ConfigCartridge[] getCartridges() {
		return cartridges;
	}
	public void setCartridges(ConfigCartridge[] cartridges) {
		this.cartridges = cartridges;
	}
	public String getApplicationId() {
		return applicationId;
	}
	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}
	public String getAlias() {
		return alias;
	}
	public void setAlias(String alias) {
		this.alias = alias;
	}
	public ConfigGroup[]  getComponents() {
		return components;
	}
	public void setComponents(ConfigGroup[]  components) {
		this.components = components;
	}
	

}
