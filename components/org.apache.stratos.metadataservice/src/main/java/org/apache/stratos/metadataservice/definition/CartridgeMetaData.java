package org.apache.stratos.metadataservice.definition;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "cartridgeMetaData")
public class CartridgeMetaData {
	public String applicationName;

	public String displayName;

	public String description;

	public String type;

	public String provider;

	public String host;

	public String version;

	public List<PropertyBean> properties;

	@Override
	public String toString() {

		return "applicationName: " + applicationName + ", displayName: " + displayName +
		       ", description: " + description + ", type: " + type + ", provider: " + provider +
		       ", host: " + host + ", Version: " + version + ", properties: " + getProperties();
	}

	private String getProperties() {

		StringBuilder propertyBuilder = new StringBuilder();
		if (properties != null) {
			for (PropertyBean propertyBean : properties) {
				propertyBuilder.append(propertyBean.toString());
			}
		}
		return propertyBuilder.toString();
	}
}
