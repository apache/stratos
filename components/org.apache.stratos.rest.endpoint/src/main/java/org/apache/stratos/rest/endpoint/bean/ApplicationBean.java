package org.apache.stratos.rest.endpoint.bean;

import org.apache.stratos.rest.endpoint.bean.topology.Cluster;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by udara on 10/17/14.
 */
@XmlRootElement(name="applications")
public class ApplicationBean {
    public List<GroupBean> groups = null;
    public List<Cluster> clusters = null;
    public String id;
    public ApplicationBean(){
        this.groups = new ArrayList<GroupBean>();
        this.clusters = new ArrayList<Cluster>();
    }
    public void addGroup(GroupBean groupBean) {
        this.groups.add(groupBean);
    }
}
