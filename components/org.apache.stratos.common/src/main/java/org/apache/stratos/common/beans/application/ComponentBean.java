package org.apache.stratos.common.beans.application;

import org.apache.stratos.common.beans.cartridge.CartridgeReferenceBean;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "components")
public class ComponentBean implements Serializable {

    private static final long serialVersionUID = -5932265453191494386L;

	private List<GroupReferenceBean> groups;
	private DependencyBean dependencies;
    private List<CartridgeReferenceBean> cartridges;

    public List<GroupReferenceBean> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupReferenceBean> groups) {
        this.groups = groups;
    }

	public DependencyBean getDependencies() {
		return dependencies;
	}

	public void setDependencies(DependencyBean dependencies) {
		this.dependencies = dependencies;
	}

	public List<CartridgeReferenceBean> getCartridges() {
		return cartridges;
	}

	public void setCartridges(List<CartridgeReferenceBean> cartridges) {
		this.cartridges = cartridges;
	}
}
