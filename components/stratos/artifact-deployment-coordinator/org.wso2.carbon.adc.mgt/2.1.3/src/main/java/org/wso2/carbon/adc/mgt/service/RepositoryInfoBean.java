/**
 * 
 */
package org.wso2.carbon.adc.mgt.service;

import org.wso2.carbon.stratos.cloud.controller.util.xsd.CartridgeInfo;

/**
 * @author wso2
 *
 */
public class RepositoryInfoBean {

	private String repoURL;
	private String cartridgeAlias;
	private String tenantDomain;
	private String userName;
	private String password;
	private String[] dirArray;
	private CartridgeInfo cartridgeInfo;	
	
	
	public RepositoryInfoBean(String repoURL, String cartridgeAlias, String tenantDomain,
                              String userName, String password, String[] dirArray, CartridgeInfo cartridgeInfo) {
	    this.repoURL = repoURL;
	    this.cartridgeAlias = cartridgeAlias;
	    this.tenantDomain = tenantDomain;
	    this.userName = userName;
	    this.setPassword(password);
	    this.dirArray = dirArray;
	    this.cartridgeInfo = cartridgeInfo;
    }
	public String getRepoURL() {
    	return repoURL;
    }
	public void setRepoURL(String repoURL) {
    	this.repoURL = repoURL;
    }
	public String getCartridgeAlias() {
    	return cartridgeAlias;
    }
	public void setCartridgeAlias(String cartridgeAlias) {
    	this.cartridgeAlias = cartridgeAlias;
    }
	public String getTenantDomain() {
    	return tenantDomain;
    }
	public void setTenantDomain(String tenantDomain) {
    	this.tenantDomain = tenantDomain;
    }
	public String getUserName() {
    	return userName;
    }
	public void setUserName(String userName) {
    	this.userName = userName;
    }
	public String[] getDirArray() {
    	return dirArray;
    }
	public void setDirArray(String[] dirArray) {
    	this.dirArray = dirArray;
    }
	public CartridgeInfo getCartridgeInfo() {
    	return cartridgeInfo;
    }
	public void setCartridgeInfo(CartridgeInfo cartridgeInfo) {
    	this.cartridgeInfo = cartridgeInfo;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
	
	
	
}
