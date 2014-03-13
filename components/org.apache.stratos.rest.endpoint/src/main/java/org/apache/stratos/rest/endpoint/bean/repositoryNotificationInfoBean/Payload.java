package org.apache.stratos.rest.endpoint.bean.repositoryNotificationInfoBean;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "payload")
public class Payload {
    private Repository repository;

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }
}
