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

public class BreadCrumbItem implements Serializable{
	private static final long serialVersionUID = 1181334740216034786L;
	private String id;
	private String i18nKey;
	private String i18nBundle;
	private String link;
	private String convertedText;
	private int order;	
	
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
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
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public String getConvertedText() {
		return convertedText;
	}
	public void setConvertedText(String convertedText) {
		this.convertedText = convertedText;
	}
	public String toString() {
		return id +":"+i18nKey+":"+i18nBundle+":"+link+":"+convertedText;
	}	
}
