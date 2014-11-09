package org.apache.stratos.cloud.controller.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;



public class ConfigDependencies  implements Serializable  {
	private ConfigDependencyPair[] startup_order;
	private String kill_behavior;
	private static final long serialVersionUID = 1L;
	
	public ConfigDependencyPair[]  getStartup_order() {
		return startup_order;
	}
	public void setStartup_order(ConfigDependencyPair[]  startup_order) {
		this.startup_order = startup_order;
	}
	public String getKill_behavior() {
		return kill_behavior;
	}
	public void setKill_behavior(String kill_behavior) {
		this.kill_behavior = kill_behavior;
	}


	
}
