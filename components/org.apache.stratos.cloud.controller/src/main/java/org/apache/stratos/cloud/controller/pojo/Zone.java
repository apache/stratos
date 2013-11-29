package org.apache.stratos.cloud.controller.pojo;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Zone extends Region {
    private String id;
    private String type;
    private List<Host> listOfHosts;

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

    public List<Host> getListOfHosts() {
        return listOfHosts;
    }

    public void setListOfHosts(List<Host> listOfHosts) {
        this.listOfHosts = listOfHosts;
    }

    public void addHost(Host host) {
        this.listOfHosts.add(host);
    }

    public void removeHost(Host host) {
        this.listOfHosts.remove(host);
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
