package org.wso2.stratos.manager.feature.dashbord.ui.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Data {
	private Map<String, Service> service;

	public Data() {
		this.service = new HashMap<String, Service>();
	}

	public Map<String, Service> getServices() {
		return service;
	}

	public String [] getKeys() {
		Set<String>  temp = this.getServices().keySet();
		return  temp.toArray(new String[0]);
	}
	
	public  String [] getServiceNames() {
		List <String> keyList = new ArrayList<String>();
		for (Map.Entry<String, Service> entry : service.entrySet())
		{
			Service tempService = entry.getValue();
			keyList.add(tempService.getName());
		}
		return keyList.toArray(new String[keyList.size()]);
	}

	public Service getService(String key) {
		return this.getServices().get(key);
	}

	public void addService(Service service) {
		this.getServices().put(service.getKey(), service);
	}

}
