package org.apache.stratos.metadata.service.definition;

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

    public String properties;

    @Override
    public String toString() {

        return "applicationName: " + applicationName + ", displayName: " + displayName +
                ", description: " + description + ", type: " + type + ", provider: " + provider +
                ", host: " + host + ", Version: " + version + ", properties: " + properties;
    }


}
