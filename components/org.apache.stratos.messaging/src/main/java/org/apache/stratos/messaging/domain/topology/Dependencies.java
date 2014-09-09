package org.apache.stratos.messaging.domain.topology;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Dependencies implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Map<String, List<Subscribable>> dependencies = new HashMap<String, List<Subscribable>>();
	private String kill_behavior = KILL_UNDEFINED;
	private boolean isKillAllEnabled = false;
	private GroupTemp groupTemp;
	private static final Log log = LogFactory.getLog(Dependencies.class);
	public static String KILL_NONE = "kill-none";
	public static String KILL_ALL = "kill-all";
	public static String KILL_DEPENDENTS = "kill-dependents";
	public static String KILL_UNDEFINED = "kill-undefined";
	
	
	public String getKill_behavior() {
		return kill_behavior;
	}
	
	public void  enableKillAll() {
		isKillAllEnabled = true;
	}
	
	public void  disableKillAll() {
		isKillAllEnabled = false;
	}
	
	public boolean isKillAllEnabled() {
		return isKillAllEnabled;
	}
	
	public String getKill_behavior_for_alias(String alias) {
		if (dependencies.containsKey(alias)) {
			return kill_behavior;
		}
		return KILL_UNDEFINED;
	}

	public void setKill_behavior(String kill_behavior) {
		this.kill_behavior = kill_behavior;
	}

	public void addDependency (String alias, Subscribable dependency) {
		List<Subscribable> my_dep = dependencies.get(alias);
		if (my_dep != null) {
			log.debug(" adding another dependency " + dependency.getAlias() + " for " + alias);
			my_dep.add(dependency);
		} else {
			log.debug("initializing dependency list for " + alias + " with dependency " + dependency.getAlias() );
			my_dep = new ArrayList<Subscribable>();
			my_dep.add(dependency);
			dependencies.put(alias, my_dep);
		}
	}

	
	public List<Subscribable> getDependencies(Subscribable subscribable, boolean kill_flag) {
		String alias = subscribable.getAlias();		
		return getDependencies(alias, kill_flag);
	}
	
	public List<Subscribable> getDependencies(String alias, boolean kill_flag) {
		if (log.isDebugEnabled()) {
			log.debug("checking dependency tree for " + alias + " kill_flag is " + kill_flag);
		}

		if (kill_flag) {
			if (kill_behavior.equals("kill-none")) {
				if (log.isDebugEnabled()) {
					log.debug(KILL_NONE + ": returning none dependencies for " + alias);
				}
				return new ArrayList<Subscribable>(0);
			} else if (kill_behavior.equals("KILL_DEPENDENTS")) {
				if (log.isDebugEnabled()) {
					log.debug(KILL_DEPENDENTS + ": returning all dependencies for " + alias);
				}
				return getDependents (alias, kill_flag);
			} 
				log.error("invalid kill option:" + kill_behavior + "rwhile checking dependencies for " + alias);
			} 
		// kill_flag false, get "startup dependencies"
		return getDependents (alias, false);
	}
	
	
	public List<Subscribable> getDownStreamDependents (String alias) {
		List<Subscribable> results = new ArrayList<Subscribable>();
		if (log.isDebugEnabled()) {
			log.debug("in dependency tree for " + alias  + " / " + dependencies.size());
		}
		if (dependencies.containsKey(alias)) {
			log.debug("adding the list of dependencies for :" + alias);
			List<Subscribable> my_dep = dependencies.get(alias);
			Iterator<Subscribable> it = my_dep.iterator();
			while (it.hasNext()) {
				Subscribable obj = it.next();
				if (obj instanceof Scalable) {
					log.debug("adding scalable " + obj.getAlias() + " as dependency for " + alias);
					results.add(obj);
					log.debug("adding subsequent downstream dependencies for " + obj.getAlias() + " as dependency for " + alias);
					List<Subscribable> sub2 = this.getDownStreamDependents(obj.getAlias());
					results.addAll(sub2);
					if (log.isDebugEnabled()) {
						log.debug("added " + sub2.size() + " elements as subsequent downstream dependencies for " + obj.getAlias() + 
								" as dependency for " + alias);
					}
				} else {
					log.debug("adding nested dependencies from " + obj.getAlias() + " for " + alias);
					List<Subscribable> sub_results = obj.getAllDependencies();
					results.addAll(sub_results);		
				}
			}
		} else {
			log.debug("continue to check nested dependencies for :" + alias);
			// convert map to List
			Set<String> keys = dependencies.keySet();
			for (String key : keys) {
				if (log.isDebugEnabled()) {
					log.debug("looping over dependency list with key:" + key + " for alias " + alias);
				}
				List<Subscribable> my_dep = dependencies.get(key);
				if (my_dep != null) {
					Iterator<Subscribable> it = my_dep.iterator();
					while (it.hasNext()) {
						Subscribable obj = it.next();
						Dependencies deps = obj.getDependencies();
						if (deps != null) {
							if (log.isDebugEnabled()) {
								log.debug("found nested nested dependencies while looping with key:" + key + " for alias " + alias);
							}
							results = deps.getDownStreamDependents(alias);
						}
					}
				}
			}
		}
		return results;
	}
		
		private List<Subscribable> getDependents (String alias, boolean kill_flag) {
			List<Subscribable> results = new ArrayList<Subscribable>();
			if (log.isDebugEnabled()) {
				log.debug("in dependency tree for " + alias + " kill_flag is " + kill_flag + " / " + dependencies.size());
			}
			if (dependencies.containsKey(alias)) {
				log.debug("adding the list of dependencies for :" + alias);
				List<Subscribable> my_dep = dependencies.get(alias);
				Iterator<Subscribable> it = my_dep.iterator();
				while (it.hasNext()) {
					Subscribable obj = it.next();
					if (obj instanceof Scalable) {
						log.debug("adding scalable " + obj.getAlias() + " as dependency for " + alias);
						results.add(obj);
					} else {
						log.debug("adding nested dependencies from " + obj.getAlias() + " for " + alias);
						List<Subscribable> sub_results = obj.getAllDependencies();
						results.addAll(sub_results);
					}
				}
			} else {
				log.debug("continue to check nested dependencies for :" + alias);
				// convert map to List
				Set<String> keys = dependencies.keySet();
				for (String key : keys) {
					if (log.isDebugEnabled()) {
						log.debug("looping over dependency list with key:" + key + " for alias " + alias);
					}
					List<Subscribable> my_dep = dependencies.get(key);
					if (my_dep != null) {
						Iterator<Subscribable> it = my_dep.iterator();
						while (it.hasNext()) {
							Subscribable obj = it.next();
							Dependencies deps = obj.getDependencies();
							if (deps != null) {
								if (log.isDebugEnabled()) {
									log.debug("found nested nested dependencies while looping with key:" + key + " for alias " + alias);
								}
								results = deps.getDependencies(alias, kill_flag);
							}
						}
					}
				}
			}

		
		return results;
	}
	
	public List<Subscribable> getUpstreamDependents (String alias) {
		List<Subscribable> results = new ArrayList<Subscribable>();
		Set<String> keys = dependencies.keySet();
		for (String key : keys) {
			if (log.isDebugEnabled()) {
				log.debug("looping over dependency list with key:" + key + " for alias " + alias + 
						" in group " + this.getGroupTemp().getAlias());
			}
			List<Subscribable> my_dep = dependencies.get(key);
			if (my_dep != null) {
				Iterator<Subscribable> it = my_dep.iterator();
				while (it.hasNext()) {
					Subscribable obj = it.next();
					if (alias.equals(obj.getAlias())) {
						//results.add(obj);
						// adding "key"
						Subscribable key_upstreamdep = this.groupTemp.getSubscribable(key);
						if (log.isDebugEnabled()) {
							log.debug("adding subscribalbe " + key_upstreamdep.getAlias() + " to upstream dependency list");
						}
						results.add(key_upstreamdep);
						if (log.isDebugEnabled()) {
							log.debug("adding subsequent upstream dependencies for " + key_upstreamdep.getAlias() + 
									" as dependency for " + alias);
						}
						List<Subscribable> sub2 = this.getUpstreamDependents(key);
						results.addAll(sub2);
						if (log.isDebugEnabled()) {
							log.debug("added " + sub2.size() + " elements as subsequent upstream dependencies for " + key_upstreamdep.getAlias() + 
									" as dependency for " + alias);
						}
					}
					
				}
			}
		}
		// get upstream subscribables which have group as dependency
		Subscribable parent = groupTemp.getParent();
		if (parent instanceof GroupTemp) {
			GroupTemp gr = (GroupTemp)parent;
			List<Subscribable> results2 = gr.getUpstreamDependencies(this.groupTemp);
			for (Subscribable s : results2) {
				if (!results.contains(s)) {
					results.add(s);
				}
			}
			if (log.isDebugEnabled()) {
				log.debug("added " + results2.size() + " elements as subsequent upstream dependencies for " + this.groupTemp.getAlias() +
						" as dependency for " + alias);
			}
			
		}
		
		return results;
		
	}
	
	/*
	public List<Subscribable> getAllDependencies() {
		List<Subscribable> results = new ArrayList<Subscribable>();
		
		if (log.isDebugEnabled()) {
			log.debug("adding all nested dependencies to result list");
		}
		
		Set<String> keys = dependencies.keySet();
		if (log.isDebugEnabled()) {
			log.debug("adding all nested dependencies to result list with number of keys " + keys.size());
		}
		
		Iterator<String> keyit = keys.iterator();
		
		while (keyit.hasNext()) {
			String key = keyit.next();
			List<Subscribable> values = dependencies.get(key);
			
			Iterator<Subscribable> it = values.iterator();
			while (it.hasNext()) {
				Subscribable obj = it.next();
				if (obj instanceof Scalable) {
					if (log.isDebugEnabled()) {
						log.debug("adding scalable / cartridge " + obj.getAlias() + " to result list");
					}
					results.add(obj);
				} else {
					if (log.isDebugEnabled()) {
						log.debug("adding nested dependencies to result list");
					}
					List<Subscribable> sub_results = obj.getDependencies().getAllDependencies();
					results.addAll(sub_results);
				}
			}
			
		}
		
		return results;
	} */
		
	public GroupTemp getGroupTemp() {
		return groupTemp;
	}

	public void setGroupTemp(GroupTemp groupTemp) {
		this.groupTemp = groupTemp;
	}

	public String toString() {
		String result = "";
		StringBuffer buf = new StringBuffer();
		
		Set<String> keys = dependencies.keySet();
		
		Iterator<String> keyit = keys.iterator();
		
		while (keyit.hasNext()) {
			String key = keyit.next();
			buf.append("Key:" + key).append(" ");
			List<Subscribable> values = dependencies.get(key);
			
			Iterator<Subscribable> it = values.iterator();
			while (it.hasNext()) {
				Subscribable obj = it.next();
				buf.append("value:" + obj.getAlias()).append(" ");
			}
		}
		
		
		return buf.toString();
	}
	
}