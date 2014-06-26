package org.apache.stratos.rest.endpoint.bean.compositeapplication.definition;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "dependencies")
public class ConfigDependencies {
	public List<Pair> startup_order = new ArrayList<Pair>();
	public String kill_behavior;
		
	/*
	public String getKill_behavior() {
		return kill_behavior;
	}

	public void setKill_behavior(String kill_behavior) {
		this.kill_behavior = kill_behavior;
	}

	public List<Pair> getStartup_order() {
		return startup_order;
	}

	public void setStartup_order(List<Pair> startup_order) {
		this.startup_order = startup_order;
	}
	*/
	

	public static class Pair {
		private String key;
		private String value;
		
		private Pair() {}
		
		public Pair(String key, String value) {
			this.key = key;
			this.value = value;
		}
		
		public String getKey() {
			return key;
		}
		public void setKey(String key) {
			this.key = key;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}
	
}
