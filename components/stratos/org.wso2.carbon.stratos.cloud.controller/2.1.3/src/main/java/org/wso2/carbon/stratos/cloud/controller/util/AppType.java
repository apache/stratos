package org.wso2.carbon.stratos.cloud.controller.util;

import java.io.Serializable;

/**
 * domain mapping related data.
 *
 */
public class AppType implements Serializable{
	
    private static final long serialVersionUID = 3550489774139807168L;
	private String name;
	private boolean appSpecificMapping = true;
	
	public AppType(){
		
	}
	
	public AppType(String name){
		this.setName(name);
	}
	
	public AppType(String name, boolean appSpecificMapping){
		this.setName(name);
		this.setAppSpecificMapping(appSpecificMapping);
	}

	public String getName() {
	    return name;
    }

	public void setName(String name) {
	    this.name = name;
    }

	public boolean isAppSpecificMapping() {
	    return appSpecificMapping;
    }

	public void setAppSpecificMapping(boolean appSpecificMapping) {
	    this.appSpecificMapping = appSpecificMapping;
    }

}
