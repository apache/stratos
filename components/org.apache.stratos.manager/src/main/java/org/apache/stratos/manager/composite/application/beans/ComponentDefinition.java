package org.apache.stratos.manager.composite.application.beans;

import java.util.List;

/**
 * Created by udara on 7/11/14.
 */
public class ComponentDefinition {
    private String group;
    private String alias;
    private List<SubscribableInfo> subscribables;
    private String deploymentPolicy;
    private String autoscalingPolicy;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public List<SubscribableInfo> getSubscribables() {
        return subscribables;
    }

    public void setSubscribables(List<SubscribableInfo> subscribables) {
        this.subscribables = subscribables;
    }

    public String getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(String deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public String getAutoscalingPolicy() {
        return autoscalingPolicy;
    }

    public void setAutoscalingPolicy(String autoscalingPolicy) {
        this.autoscalingPolicy = autoscalingPolicy;
    }
}
