package org.apache.stratos.messaging.domain.topology;


import java.io.Serializable;
import java.util.List;



public class ConfigCompositeApplication  implements Serializable {
	
	/* grouping_poc */
	private String applicationId;
	private String alias;
	private List<ConfigGroup> components;
	private List<ConfigCartridge> cartridges;
	
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
	public List<ConfigGroup> getComponents() {
		return components;
	}
	public void setComponents(List<ConfigGroup> components) {
		this.components = components;
	}
	public List<ConfigCartridge> getCartridges() {
		return cartridges;
	}
	public void setCartridges(List<ConfigCartridge> cartridges) {
		this.cartridges = cartridges;
	}
	
}
