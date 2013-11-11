package org.apache.stratos.cloud.controller.util;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Template;

import java.util.HashMap;
import java.util.Map;

public class Host extends Zone {
    private String id;
    private String type;
    private Map<String, String> properties = new HashMap<String, String>();

    private transient ComputeService computeService;

    private transient Template template;


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
        if(getProperties().get(key) != null) {
            return getProperties().get(key);
        } else {
            return super.getProperty(key);
        }
    }

     public ComputeService getComputeService() {
        return computeService;
    }

    public void setComputeService(ComputeService computeService) {
        this.computeService = computeService;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }
}
