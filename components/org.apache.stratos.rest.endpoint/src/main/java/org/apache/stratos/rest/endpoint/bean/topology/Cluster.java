package org.apache.stratos.rest.endpoint.bean.topology;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class Cluster {

    public String serviceName;

    public String clusterId;

    public List<Member> member;

    public String tenantRange;

    public List<String> hostNames;

    public boolean isLbCluster;
}
