package org.apache.stratos.autoscaler.util;

import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.session.UserRegistry;

public class ServiceReferenceHolder {
	
	private static ServiceReferenceHolder instance;
	private Registry registry;	

	private ServiceReferenceHolder() {
	}
	 
	public static ServiceReferenceHolder getInstance() {
	    if (instance == null) {
	        instance = new ServiceReferenceHolder();
	    }
	        return instance;
	}
	 
	public void setRegistry(UserRegistry governanceSystemRegistry) {
		registry = governanceSystemRegistry;
	}

    public Registry getRegistry() {
		return registry;
	}
}
