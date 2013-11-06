package org.apache.stratos.cloud.controller.util;

import java.util.HashMap;
import java.util.Map;

public class Zone {
    private String id;
    private String type;

    private Map<String, String> properties = new HashMap<String, String>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

     public void setProperty(String key, String value) {

        if (key != null && value != null) {
            getProperties().put(key, value);
        }
    }

    public String getProperty(String key) {
        return getProperties().get(key);
    }
}
