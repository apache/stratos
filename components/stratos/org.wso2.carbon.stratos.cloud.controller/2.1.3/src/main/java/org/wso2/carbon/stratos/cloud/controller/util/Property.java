package org.wso2.carbon.stratos.cloud.controller.util;
/**
 * Holds a property 
 */
public class Property {
	
	private String name;
	private String value;
	
	public Property(){
		
	}
	
	public Property(String name, String value){
		this.setName(name);
		this.setValue(value);
	}

	public String getName() {
	    return name;
    }

	public void setName(String name) {
	    this.name = name;
    }

	public String getValue() {
	    return value;
    }

	public void setValue(String value) {
	    this.value = value;
    }

}
