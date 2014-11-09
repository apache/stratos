package org.apache.stratos.manager.composite.application.beans;

import org.apache.stratos.manager.grouping.definitions.DependencyDefinitions;
import org.apache.stratos.manager.grouping.definitions.StartupOrderDefinition;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "components")
public class ComponentDefinition {

    private List<GroupDefinition> groups;

    private List<SubscribableDefinition> subscribables;

    private DependencyDefinitions dependencies;

    public List<SubscribableDefinition> getSubscribables() {
        return subscribables;
    }

    public void setSubscribables(List<SubscribableDefinition> subscribables) {
        this.subscribables = subscribables;
    }

    public List<GroupDefinition> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupDefinition> groups) {
        this.groups = groups;
    }

    public DependencyDefinitions getDependencies() {
        return dependencies;
    }

    public void setDependencies(DependencyDefinitions dependencies) {
        this.dependencies = dependencies;
    }
}
