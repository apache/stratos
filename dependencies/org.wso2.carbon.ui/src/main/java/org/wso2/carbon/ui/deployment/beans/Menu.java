/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.ui.deployment.beans;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents a Menu item in UI framework
 */
public class Menu implements Comparable<Menu>, Serializable {
	private String id;
	private String i18nKey;
	private String i18nBundle;
	private String parentMenu;
	private String link;
	private String region;
	private String order;
	private String icon;
	private String styleClass;
	//authentication required to display this menu item
	private boolean requireAuthentication = true;
	private String[] requirePermission = new String[]{"*"};
    private boolean allPermissionsRequired = false;
    private boolean atLeastOnePermissionsRequired = false;
    private boolean requireSuperTenant = false;
    private boolean requireNotSuperTenant = false;
    private boolean requireCloudDeployment = false;

    private boolean requireNotLoggedIn = false;
	//eg: param1=Claims&action=add
	//These parameters will be appended to link & will be available in
	//generated menu link
	private String urlParameters;
	

	/**
	 * @deprecated
	 */
    private String name;
    /**
     * @deprecated
     */
    private int level;

    public Menu(){   	
    }
    
    public int getLevel() {
        return level;
    }

    public int compareTo(Menu o) {
        return o.getLevel() - level;
    }

    public boolean equals(Object ob){
        if((ob != null) && (this.id != null) && (ob instanceof Menu)){
            return (this.id.equals(((Menu)ob).id))? true : false;
        }
        return false;
    }

    public int hashCode(){
        return this.id.hashCode();
    }

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String key) {
		i18nKey = key;
	}

	public String getI18nBundle() {
		return i18nBundle;
	}

	public void setI18nBundle(String bundle) {
		i18nBundle = bundle;
	}

	public String getParentMenu() {
		return parentMenu;
	}

	public void setParentMenu(String parentMenu) {
		this.parentMenu = parentMenu;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}	

	public String getStyleClass() {
		return styleClass;
	}

	public void setStyleClass(String styleClass) {
		this.styleClass = styleClass;
	}

	
	public boolean getRequireAuthentication() {
		return requireAuthentication;
	}

	public void setRequireAuthentication(boolean requireAuthentication) {
		this.requireAuthentication = requireAuthentication;
	}	

	public String[] getRequirePermission() {
		return requirePermission;
	}

	public void setRequirePermission(String[] requirePermission) {
		this.requirePermission = Arrays.copyOf(requirePermission, requirePermission.length);
	}

    public boolean isAllPermissionsRequired() {
        return allPermissionsRequired;
    }

    public void setAllPermissionsRequired(boolean allPermissionsRequired) {
        this.allPermissionsRequired = allPermissionsRequired;
    }

    public boolean isAtLeastOnePermissionsRequired() {
        return atLeastOnePermissionsRequired && !isAllPermissionsRequired();
    }

    public void setAtLeastOnePermissionsRequired(boolean atLeastOnePermissionsRequired) {
        this.atLeastOnePermissionsRequired = atLeastOnePermissionsRequired;
    }

    /**
	 * eg: param1=Claims&action=add
	 * These parameters will be appended to link & will be available in
	 * generated menu link
	 * @return String
	 */
	public String getUrlParameters() {
		return urlParameters;
	}

	public void setUrlParameters(String urlParameters) {
		this.urlParameters = urlParameters;
	}

	public String toString() {
		return id+" : "
		+i18nKey+" : "
		+i18nBundle+" : "
		+parentMenu+" : "
		+link+" : "
		+region+" : "
		+order+" : "
		+icon+" : "
		+styleClass+" : "
		+requireAuthentication+" : "
		+urlParameters;
	}

    public boolean isRequireSuperTenant() {
        return requireSuperTenant;
    }

    public void setRequireSuperTenant(boolean requireSuperTenant) {
        this.requireSuperTenant = requireSuperTenant;
    }

    public void setRequireNotSuperTenant(boolean requireNotSuperTenant) {
        this.requireNotSuperTenant = requireNotSuperTenant;
    }

    public boolean isRequireNotSuperTenant() {
        return requireNotSuperTenant;
    }

    public boolean isRequireNotLoggedIn() {
        return requireNotLoggedIn;
    }

    public void setRequireNotLoggedIn(boolean requireNotLoggedIn) {
        this.requireNotLoggedIn = requireNotLoggedIn;
    }

    public boolean isRequireCloudDeployment() {
        return requireCloudDeployment;
    }

    public void setRequireCloudDeployment(boolean requireCloudDeployment) {
        this.requireCloudDeployment = requireCloudDeployment;
    }
}
