package org.apache.stratos.rest.endpoint.bean.repositoryNotificationInfoBean;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "repository")
public class Repository {
    private String url;
    private String description;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
