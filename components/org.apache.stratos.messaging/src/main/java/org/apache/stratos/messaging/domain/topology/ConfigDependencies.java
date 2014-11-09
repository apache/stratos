package org.apache.stratos.messaging.domain.topology;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class ConfigDependencies  implements Serializable {
	private static final long serialVersionUID = 1L;
	private List<Pair> startup_order = new ArrayList<Pair>();
	private String kill_behavior;
		
	
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
	
	

	public static class Pair  implements Serializable {
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
