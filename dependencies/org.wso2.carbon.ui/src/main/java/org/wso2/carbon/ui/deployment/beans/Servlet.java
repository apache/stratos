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

public class Servlet {
	private String id;
	private String name;
	private String displayName;
	private String servletClass;
	private String urlPatten;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getServletClass() {
		return servletClass;
	}
	public void setServletClass(String servletClass) {
		this.servletClass = servletClass;
	}
	
	public String getUrlPatten() {
		return urlPatten;
	}
	public void setUrlPatten(String urlPatten) {
		this.urlPatten = urlPatten;
	}
	public String toString(){
		return "ServletDefinition = "+id+" : "+name+" : "+displayName+" : "+servletClass+" : "+urlPatten;
	}
    public boolean equals(Object ob){
        if((ob !=null) && (this.id != null) && (ob instanceof Servlet)){
           return (this.id.equals(((Servlet)ob).id))? true : false;
        }
        return false;
    }
    public int hashCode(){
        return this.id.hashCode();
    }

}
