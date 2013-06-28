package org.wso2.carbon.lb.common.util;

/**
 *
 */
public class DomainMapping {
    private String mapping;
    private String actualHost;

    public DomainMapping(String mapping) {
        this.mapping = mapping;
    }

    public String getActualHost() {
        return actualHost;
    }

    public void setActualHost(String actualHost) {
        this.actualHost = actualHost;
    }

}
