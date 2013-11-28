package org.apache.stratos.cloud.controller.pojo;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Template;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Region extends IaasProvider {
    private String imageId;
    private String identity;
    private String credential;
    private String id;
    private String type;
    private List<Zone> listOfZones;

    private transient ComputeService computeService;

    private transient Template template;

    private Map<String, String> properties = new HashMap<String, String>();

    public String getProperty(String key) {
        if(getProperties().get(key) != null) {
            return getProperties().get(key);
        } else {
            return super.getProperty(key);
        }
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperty(String key, String value) {

        if (key != null && value != null) {
            getProperties().put(key, value);
        }
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }


    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getIdentity() {
        if(identity == null) {
            return super.getIdentity();
        }
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getCredential() {
        if(credential == null) {
            return super.getCredential();
        }
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
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

    public List<Zone> getListOfZones() {
        return listOfZones;
    }

    public void setListOfZones(List<Zone> listOfZones) {
        this.listOfZones = listOfZones;
    }

    public void addZone(Zone zone) {
        this.listOfZones.add(zone);
    }

    public void removeZone(Zone zone) {
        this.listOfZones.remove(zone);
    }
}
