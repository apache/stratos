package org.apache.stratos.adc.mgt.dao;

import java.io.Serializable;

public class Cluster implements Serializable {

    private int id;
    private String clusterDomain;
    private String clusterSubDomain;
    private String mgtClusterDomain;
    private String mgtClusterSubDomain;
    private String hostName;
    private String serviceStatus;

    public Cluster() {
    }

    public String getHostName() {
        return hostName;
    }

    public String getClusterDomain() {
        return clusterDomain;
    }

    public void setClusterDomain(String clusterDomain) {
        this.clusterDomain = clusterDomain;
    }

    public String getClusterSubDomain() {
        return clusterSubDomain;
    }

    public void setClusterSubDomain(String clusterSubDomain) {
        this.clusterSubDomain = clusterSubDomain;
    }

    public String getMgtClusterDomain() {
        return mgtClusterDomain;
    }

    public void setMgtClusterDomain(String mgtClusterDomain) {
        this.mgtClusterDomain = mgtClusterDomain;
    }

    public String getMgtClusterSubDomain() {
        return mgtClusterSubDomain;
    }

    public void setMgtClusterSubDomain(String mgtClusterSubDomain) {
        this.mgtClusterSubDomain = mgtClusterSubDomain;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getServiceStatus() {
        return serviceStatus;
    }

    public void setServiceStatus(String serviceStatus) {
        this.serviceStatus = serviceStatus;
    }
}