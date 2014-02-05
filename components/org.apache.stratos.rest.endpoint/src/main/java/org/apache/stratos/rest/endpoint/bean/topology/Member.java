package org.apache.stratos.rest.endpoint.bean.topology;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Member {
    public String serviceName;
    public String clusterId;
    public String networkPartitionId;
    public String partitionId;
    public String memberId;

    public String status;
    public String memberIp;
    public String lbClusterId;
    public String memberPublicIp;

}
