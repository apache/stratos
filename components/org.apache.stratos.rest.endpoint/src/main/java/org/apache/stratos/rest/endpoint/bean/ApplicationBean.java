package org.apache.stratos.rest.endpoint.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by udara on 10/17/14.
 */
public class ApplicationBean {
    public List<GroupBean> groups = null;
    public String id;
    public ApplicationBean(){
        this.groups = new ArrayList<GroupBean>();
    }
    public void addGroup(GroupBean groupBean) {
        this.groups.add(groupBean);
    }
}
