package org.apache.stratos.common.beans.application;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Startup dependency order bean.
 */
@XmlRootElement(name = "startupOrders")
public class StartupOrderBean implements Serializable {

    private static final long serialVersionUID = 735897654230989762L;

    private List<String> aliases;

    public StartupOrderBean() {
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
