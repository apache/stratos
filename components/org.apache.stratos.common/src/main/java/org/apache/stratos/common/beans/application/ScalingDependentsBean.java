package org.apache.stratos.common.beans.application;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Scaling dependents bean.
 */
@XmlRootElement(name="scalingDependents")
public class ScalingDependentsBean implements Serializable {

    private static final long serialVersionUID = -3495705783732405914L;

    private List<String> aliases;

    public ScalingDependentsBean() {
        aliases = new ArrayList<String>();
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public void addAlias(String alias) {
        aliases.add(alias);
    }

    public void removeAlias(String alias) {
        if(aliases.contains(alias)) {
            aliases.remove(alias);
        }
    }

    public boolean containsAlias(String alias) {
        return aliases.contains(alias);
    }
}
