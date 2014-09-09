package org.apache.stratos.messaging.domain.topology;

import java.util.List;



public interface Subscribable {
	

	
    public String getAlias();
	
	public Dependencies getDependencies();
	
	public List<Subscribable>  getAllDependencies();
	
	public void subscribe();
	
	public void unsubscribe();	
	
	public void setHomeGroup(GroupTemp homeGroupTemp);
	
	public GroupTemp getHomeGroup();

}
