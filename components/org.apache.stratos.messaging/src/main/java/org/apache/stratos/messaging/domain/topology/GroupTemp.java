package org.apache.stratos.messaging.domain.topology;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class GroupTemp implements Subscribable,Composite, Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String alias;
	private Map<String, Subscribable> subscribables = new HashMap<String, Subscribable>();
	private Dependencies dependencies;
	private Subscribable parent = null;
	private GroupTemp homeGroupTemp;
	private static final Log log = LogFactory.getLog(GroupTemp.class);

	public GroupTemp(String alias) {
		this.alias = alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	public String getAlias() {
		return alias;
	}
	
	public Map<String, Subscribable> getSubscribables() {
		return subscribables;
	}
	
	public Subscribable getSubscribable(String alias) {
		return subscribables.get(alias);
	}

	public Subscribable getParent() {
		return parent;
	}

	public GroupTemp getHomeGroup() {
		return homeGroupTemp;
	}

	public void setHomeGroup(GroupTemp homeGroupTemp) {
		this.homeGroupTemp = homeGroupTemp;
	}

	public void setParent(Subscribable parent) {
		this.parent = parent;
	}
	
	public String getKillBehaviorForAlias(String alias) {
		if (this.dependencies != null) {
			return this.dependencies.getKill_behavior_for_alias(alias);
		} else {
			return Dependencies.KILL_UNDEFINED;
		}
	}
	
	public String findKillBehavior(String alias) {
		
		List<GroupTemp> all_groupTemps = getAllGroups();
		String kill_behavior = Dependencies.KILL_UNDEFINED;
		
		if (log.isDebugEnabled()) {
			log.debug("searching kill behavior for " + alias + " in group " + this.alias);
		}
		
		for (GroupTemp groupTemp : all_groupTemps) {
			kill_behavior = groupTemp.getKillBehaviorForAlias(alias);
			if (Dependencies.KILL_UNDEFINED.equals(kill_behavior)) {
				if (log.isDebugEnabled()) {
					log.debug("kill behavior for " + alias + " in group " + this.alias + " is undefined");
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("found kill behavior for " + alias + " in group " + this.alias);
				}
				return groupTemp.getKillBehaviorForAlias(alias);
			}
		}
		return dependencies.getKill_behavior();
	}
	
	public List<GroupTemp> getAllGroups () {
		List<GroupTemp> results = new ArrayList<GroupTemp>();
		// adding the group to list
		results.add(this);
		Set<String> keys = subscribables.keySet();
		if (log.isDebugEnabled()) {
			log.debug("adding all nested groups to result list with number of keys " + keys.size() + " in group " + this.alias);
		}
		
		Iterator<String> keyit = keys.iterator();
		
		while (keyit.hasNext()) {
			String key = keyit.next();
			Object obj = subscribables.get(key);
			if (!(obj instanceof GroupTemp)) {
				if (log.isDebugEnabled()) {
					log.debug("skipping non composite obj " + obj);
				}

			} else  {
				
				GroupTemp groupTemp = (GroupTemp) obj;
				if (log.isDebugEnabled()) {
					log.debug("adding nested groups / composite to result list " + groupTemp.getAlias());
			    }
				List<GroupTemp> sub_results = groupTemp.getAllGroups();
				results.addAll(sub_results);
			}
		}
		
		return results;
	}
	
	public Dependencies findDependencies(String alias) {
		GroupTemp gr = findGroup(alias);
		if (gr !=null) {
			return gr.dependencies;
		}
		return null;
	}
	
	public GroupTemp findGroup(String alias) {
		List<GroupTemp> all_groupTemps = getAllGroups();
		for (GroupTemp groupTemp : all_groupTemps) {
			if (log.isDebugEnabled()) {
				log.debug("findGroup in group  " + groupTemp.getAlias() + " for alias " + alias);
			}
			if (groupTemp.subscribables.containsKey(alias)) {
				return groupTemp;
			} else {
				// does it need to be checked ?
			}
		}
		
		return null;
	}
	
	// returns all groups which equal the kill_behavior as parameter
	public String [] findAllGroupsWithKill(String [] aliases, String kill_behavior) {
		List<String> results = new ArrayList<String>();
		for (String alias : aliases) {
			GroupTemp gr = findGroup(alias);
			// no null check, shouldn't be null
			if (gr != null) {
				Dependencies dep = gr.getDependencies();
				if (dep != null) {
					if (kill_behavior.equals(dep.getKill_behavior())) {
						results.add(gr.getAlias());
					}
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("no group found for alias  " + alias);
				}
			}
		}
		String [] arr = new String[results.size()];
		arr = results.toArray(arr);
		return arr;
	}
	
	public String [] findAllGroupsWithKill2(Map<String, String> aliasesMap, String kill_behavior) {
		List<String> results = new ArrayList<String>();
		Set<String> aliases = aliasesMap.keySet();
		for (String alias : aliases) {
			GroupTemp gr = findGroup(alias);
			// no null check, shouldn't be null
			if (gr != null) {
				Dependencies dep = gr.getDependencies();
				if (dep != null) {
					if (kill_behavior.equals(dep.getKill_behavior())) {
						results.add(gr.getAlias());
					}
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("no group found for alias  " + alias);
				}
			}
		}
		String [] arr = new String[results.size()];
		arr = results.toArray(arr);
		return arr;
	}
	

	public Dependencies getDependencies() {
		return dependencies;
	}
	
	public List<Subscribable>  getAllDependencies() {
		List<Subscribable> results = new ArrayList<Subscribable>();
		
		if (log.isDebugEnabled()) {
			log.debug("adding all nested subscribables to result list in subscribable " + this.alias);
		}
		
		Set<String> keys = subscribables.keySet();
		if (log.isDebugEnabled()) {
			log.debug("adding all nested dependencies to result list with number of keys " + keys.size());
		}
		
		Iterator<String> keyit = keys.iterator();
		
		while (keyit.hasNext()) {
			String key = keyit.next();
			Subscribable obj = subscribables.get(key);
			if (obj instanceof Scalable) {
				if (log.isDebugEnabled()) {
					log.debug("adding scalable / cartridge " + obj.getAlias() + " to result list");
				}
				results.add(obj);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("adding nested dependencies to result list");
				}
				List<Subscribable> sub_results = obj.getAllDependencies();
				results.addAll(sub_results);
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("returning " + results.size() + " subscribables while getting all dependent subscribables ");
		}
		return results;
	}
	
	public String [] getDownStreamDependenciesAsArray (String alias) {
		if (log.isDebugEnabled()) {
			log.debug("getting dependencies from group :" +  this.getAlias());
		}
		
		String [] dependenciesAliasArray = null;
		List<Subscribable> results = null;
		
		if (log.isDebugEnabled()) {
			log.debug("getting selected dependencies from group : " + alias);
		}
		Dependencies group_deps = this.getDependencies();
		results = group_deps.getDownStreamDependents(alias);
		dependenciesAliasArray = new String [results.size()];
		for (int i = 0; i < results.size(); i++ ) {
			Subscribable s = results.get(i);
			dependenciesAliasArray[i] = s.getAlias();
			if (log.isDebugEnabled()) {
				log.debug("adding " + s.getAlias() + " to dependency array");
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("returning dependencies for subscribale : " + alias);
		}
		return dependenciesAliasArray;
	}
	
	public Map<String, String> getDownStreamDependenciesAsMap (String alias) {
		if (log.isDebugEnabled()) {
			log.debug("getting dependencies from group :" +  this.getAlias());
		}
		
		Map<String, String> dependencies_map= null;
		List<Subscribable> results = null;
		
		if (log.isDebugEnabled()) {
			log.debug("getting selected dependencies from group : " + alias);
		}
		Dependencies group_deps = this.getDependencies();
		results = group_deps.getDownStreamDependents(alias);
		dependencies_map = new HashMap<String, String>(results.size());
		for (int i = 0; i < results.size(); i++ ) {
			Subscribable s = results.get(i);
			
			GroupTemp gr = s.getHomeGroup(); // TODO - need to change return type to Group
			String gr_alias = null;
			if (gr != null) {
				gr_alias = gr.getAlias();
			} else {
				if (log.isDebugEnabled()) {
					log.debug("home group is null for subscribable " + s.getAlias());
				}
			}
			dependencies_map.put(s.getAlias(), gr_alias);
			if (log.isDebugEnabled()) {
				log.debug("adding " + s.getAlias() + " in group " + gr_alias + " to downstream dependency map ");
			}
			
		}
		
		if (log.isDebugEnabled()) {
			log.debug("returning dependencies for subscribale : " + alias);
		}
		return dependencies_map;
	}
	
	public String [] getUpstreamDependenciesAsArray (String alias) {
		String [] dependenciesAliasArray = new String[0];
		if (log.isDebugEnabled()) {
			log.debug("searching home group for  : " + alias);
		}
		GroupTemp gr = this;
		Subscribable sub = gr.getSubscribable(alias);
		if (log.isDebugEnabled()) {
			log.debug("found home group for  : " + alias + " , group is " + gr.getAlias() + " subscribable is " + sub);
		}
		//List<Subscribable> results = gr.getUpstreamDependencies(sub);
		List<Subscribable> results = gr.getDependencies().getUpstreamDependents(sub.getAlias());
		if (log.isDebugEnabled()) {
			log.debug("home group upstream result for  : " + alias + " is " + results.size());
		}
		dependenciesAliasArray = new String [results.size()];
		for (int i = 0; i < results.size(); i++) {
			Subscribable item = results.get(i);
			if (item != null) {
				dependenciesAliasArray[i] = item.getAlias();
			} else {
				if (log.isDebugEnabled()) {
					log.debug("dependenciesAliasArray in getUpstreamDependencies has null value");
					dependenciesAliasArray[i] = null;
				}
			}
		}
		return dependenciesAliasArray;
	}
	
	public Map<String, String> getUpstreamDependenciesAsMap (String alias) {
		if (log.isDebugEnabled()) {
			log.debug("searching home group for  : " + alias);
		}
		GroupTemp gr = this;
		Map<String, String> dependencies_map = null;
		Subscribable sub = gr.getSubscribable(alias);
		if (log.isDebugEnabled()) {
			log.debug("found home group for  : " + alias + " , group is " + gr.getAlias() + " subscribable is " + sub);
		}

		List<Subscribable> results = gr.getDependencies().getUpstreamDependents(sub.getAlias());
		if (log.isDebugEnabled()) {
			log.debug("home group upstream result for  : " + alias + " is " + results.size());
		}
		dependencies_map = new HashMap<String, String>(results.size());
		for (int i = 0; i < results.size(); i++) {
			Subscribable s = results.get(i);
			if (s != null) {
				GroupTemp home_gr = s.getHomeGroup(); // TODO - need to change return type to Group
				String key = null;
				String val = null;
				if (home_gr != null) {
					key =  s.getAlias();
					val = home_gr.getAlias();
					if (log.isDebugEnabled()) {
						log.debug("adding " + key + " in group " + val + " to upstream dependency map ");
					}
					dependencies_map.put(key, val);
				} 
				
			} else {
				if (log.isDebugEnabled()) {
					log.debug("dependenciesAliasMap in getUpstreamDependencies has null value");
				}
			}
			
			
		}
		return dependencies_map;
	}
	
	public List<Subscribable>  getUpstreamDependencies(Subscribable subscribable) {
		List<Subscribable> upstream = new ArrayList<Subscribable>();
		Subscribable parentSubscribable = this.getParent();
		if (log.isDebugEnabled()) {
			log.debug("getting upstream dependencies for " + subscribable.getAlias() + " in group " + this.alias);
		}
		if (subscribable instanceof Scalable)  {
			// this would be typically where the upstream search starts
			if (log.isDebugEnabled()) {
				log.debug("subscribable is  cartridge with alias " + subscribable.getAlias() + " in group " + this.alias);
			}
			if (parent == null) {
				if (log.isDebugEnabled()) {
					log.debug("parent is null, stopping upstream search in group " + this.alias + " for " + alias  );
				}
				return upstream;
			} else if (parent instanceof GroupTemp) {

				GroupTemp parentGroupTemp = (GroupTemp) parentSubscribable;
				if (log.isDebugEnabled()) {
					log.debug("continue upstream search in parent group " + parentGroupTemp.alias + " for " + alias  );
				}
				// continue with group alias as dependency alias (dependent would be group)
				List<Subscribable> list = parentGroupTemp.getUpstreamDependencies(this);
				if (log.isDebugEnabled()) {
					log.debug("found " + list.size() + " items in upstream search in parent group " + parentGroupTemp.alias + " for " + alias  );
				}
				upstream.addAll(list);
			}
		} else if (subscribable instanceof GroupTemp) {
			// get local dependencies
			if (log.isDebugEnabled()) {
				log.debug(subscribable.getAlias() + " is a group,  checking in dependencies list of this group " + this.alias + " for " + alias  );
			}
			List<Subscribable> listlocal = dependencies.getUpstreamDependents(subscribable.getAlias());
			if (log.isDebugEnabled()) {
				log.debug("found " + listlocal.size() + " items in upstream search in group " + this.alias + " for " + alias  );
			}
			// add local cartridges which depend on the group
			upstream.addAll(listlocal);
			// continue upstream with parent of this group
			Subscribable parent = this.getParent();
			if (parent == null) {
				if (log.isDebugEnabled()) {
					log.debug("parent is null, stopping upstream search in group " + this.alias + " for " + alias  );
				}
				return upstream;
			} else if (parent instanceof GroupTemp) {
				if (log.isDebugEnabled()) {
					log.debug("continue upstream search in parent group " + parent.getAlias() + " for " + alias  );
				}
				// continue with group alias as dependency alias (dependent would be group)
				List<Subscribable> list = ((GroupTemp) parent).getUpstreamDependencies(this);
				if (log.isDebugEnabled()) {
					log.debug("found " + list.size() + " items in upstream search in parent group " + parent.getAlias() + " for " + alias  );
				}
				// merge upstream dependencies and local dependencies
				upstream.addAll(list);
			}

		} 
		return upstream;
	}

	public void setDependencies(Dependencies dependencies) {
		this.dependencies = dependencies;
	}

	



	@Override
	public void subscribe() {
		// TODO Auto-generated method stub
		log.debug("subscribing to group: " + alias);
		
		Iterator<String> it = subscribables.keySet().iterator();
		
		while (it.hasNext()) {
			String key = it.next();
			Subscribable subscribable = subscribables.get(key);
			subscribable.subscribe();
		}
		
	}

	@Override
	public void unsubscribe() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void add(Subscribable subscribale) {
		// TODO Auto-generated method stub
		if (log.isDebugEnabled()) {
			log.debug("building the group, adding subscribable " + subscribale.getAlias() + "  to group  " + this.getAlias());
		}
		subscribables.put(subscribale.getAlias(), subscribale);
	}

	@Override
	public void remove(Subscribable subscribale) {
		// TODO Auto-generated method stub
		subscribables.remove(subscribale.getAlias());
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer();
		Iterator<String> it = subscribables.keySet().iterator();
		
		while (it.hasNext()) {
			String key = it.next();
			Subscribable subscribable = subscribables.get(key);
			buf.append("subscribable: " + subscribable.toString());
			
		}
		return buf.toString();
	}


}
