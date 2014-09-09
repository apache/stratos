/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.domain.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.*;



/**
 * Defines a topology of serviceMap in Stratos.
 */
// Grouping
public class CompositeApplication implements Serializable {
    private static final long serialVersionUID = -1L;
    // Key: Service.serviceName
    private String alias;
    private GroupTemp top_level;
    private Map<String, GroupTemp> all_groups  = null;
    private List<String> clusterIds;
    private static final Log log = LogFactory.getLog(CompositeApplication.class);

    public CompositeApplication() {
       
    }

    public String getAlias() {
		return alias;
	}


	public void setAlias(String alias) {
		this.alias = alias;
	}

	/*
	public Group getTop_level() {
		return this.top_level;
	}*/
	/*
	public void setTop_level(Group top_level) {
		this.top_level = top_level;
	} */
	

	public Map<String, GroupTemp> getAll_groups() {
		return all_groups;
	}

	public void setAll_groups(Map<String, GroupTemp> all_groups) {
		this.all_groups = all_groups;
	}
	
	
	public List<GroupTemp> findAllGroupsForServiceType(String serviceType) {
		Set<String> group_names = this.all_groups.keySet();
		List<GroupTemp> found_groups = new ArrayList<GroupTemp>();
		
		for (String group_name: group_names) {
            GroupTemp gr = this.all_groups.get(group_name);
			Subscribable sub = gr.getSubscribable(serviceType);
			if (sub != null) {
				found_groups.add(gr);
			} 
		}
		
		return found_groups;
	}
	
	public GroupTemp getGroupFromGroupAlias(String alias) {
		return this.all_groups.get(alias);
	}
	

	public String [] getDependencies (String alias, boolean kill_flag) {
		if (log.isDebugEnabled()) {
			log.debug("getting dependencies from group :" +  this.top_level);
		}
		
		String [] dependenciesAliasArray = null;
		List<Subscribable> results = null;
		if (kill_flag && top_level.getDependencies().getKill_behavior().equals("kill-all")) {
			if (log.isDebugEnabled()) {
				log.debug("kill-all: returning all dependencies for " + alias);
			}
			
			results = top_level.getAllDependencies();
			dependenciesAliasArray = new String [results.size()];
		} else {
			if (log.isDebugEnabled()) {
				log.debug("getting selected dependencies from group : " + alias);
			}
			Dependencies group_deps = this.top_level.getDependencies();
			results = group_deps.getDependencies(alias, kill_flag);
			dependenciesAliasArray = new String [results.size()];
			for (int i = 0; i < results.size(); i++ ) {
				Subscribable s = results.get(i);
				dependenciesAliasArray[i] = s.getAlias();
				if (log.isDebugEnabled()) {
					log.debug("adding " + s.getAlias() + " to dependency array");
				}
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("returning dependencies for subscribale : " + alias);
		}
		return dependenciesAliasArray;
	}
	
	public String [] getDownStreamDependencies (String alias) {
		if (log.isDebugEnabled()) {
			log.debug("getting dependencies from group :" +  this.top_level);
		}
		
		String [] dependenciesAliasArray = null;
		List<Subscribable> results = null;
		
		if (log.isDebugEnabled()) {
			log.debug("getting selected dependencies from group : " + alias);
		}
		Dependencies group_deps = this.top_level.getDependencies();
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
	
	public String extractClusterGroupFromClusterId (String clusterId) {
		String sub1 = extractAlias(clusterId);
		if (sub1 == null) {
			return null;
		}
		
		String [] s = sub1.split("-");
		if (log.isDebugEnabled())  {
			log.debug("clusterGroup alias is " + sub1 + " size: " + s.length);
		}
		if (s.length == 0) {
			return null;
		} 
		if (log.isDebugEnabled())  {
			log.debug("cluster " + clusterId + " is in group " +  s[0]);
		}
		return s[0];
	}
	
	private String extractAlias (String clusterId) {
		String [] s = clusterId.split("\\.");
		if (log.isDebugEnabled())  {
			log.debug("clusterId alias is " + clusterId + " size: " + s.length);
		}
		if (s.length == 0) {
			return null;
		} 
		if (log.isDebugEnabled())  {
			log.debug("clusterId alias is " + clusterId + " alias: " + s[0]);
		}
		return s[0];
	}
	
	
	/*
	public String [] getUpstreamDependencies (String alias) {
		String [] dependenciesAliasArray = new String[0];
		if (log.isDebugEnabled()) {
			log.debug("searching home group for  : " + alias);
		}
		Group gr = getTop_level().findGroup(alias);
		if (gr == null) {
			if (log.isDebugEnabled()) {
				log.debug("no home group found for  : " + alias);
			}
			return dependenciesAliasArray;
		}
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
	} */
	
	public String extractAliasFromClusterId (String clusterId) {
		String [] s = clusterId.split("\\.");
		if (log.isDebugEnabled())  {
			log.debug("clusterId alias is " + clusterId + " size: " + s.length);
		}
		if (s.length == 0) {
			return null;
		} 
		if (log.isDebugEnabled())  {
			log.debug("clusterId alias is " + clusterId + " alias: " + s[0]);
		}
		return s[0];
	}
	
	/*
	public String [] getAllInPathOf(String aServiceId) {
	// check kill_all_enabled flag
		String [] upstreamDependencies = this.getUpstreamDependencies(aServiceId);
		String [] downstreamDependencies = this.getDownStreamDependencies(aServiceId);
		
 		List<String> all = new ArrayList<String>(upstreamDependencies.length + downstreamDependencies.length + 1);
 		for (String str : upstreamDependencies) {
 			all.add(str);
 		}
 		for (String str : downstreamDependencies) {
 			all.add(str);
 		}
 		all.add(aServiceId);
 		String [] arrayAll = new String[all.size()];
 		arrayAll = all.toArray(arrayAll);
 		return arrayAll;
	} */
	
	public Map<String, String> getAllInPathOfAsMap(String aServiceId, GroupTemp home_group) {
		// check kill_all_enabled flag
			Map<String, String> upstreamDependencies = home_group.getUpstreamDependenciesAsMap(aServiceId);
			Map<String, String> downstreamDependencies = home_group.getDownStreamDependenciesAsMap(aServiceId);
			
	 		Map<String, String> all = new HashMap<String, String>();
	 		all.putAll(upstreamDependencies);
	 		all.putAll(downstreamDependencies);
	 		
	 		all.put(aServiceId,  home_group.getAlias());
	 		
	 		return all;
		}
	

	@Override
    public String toString() {
		String result = "compositeApplication [" + alias + "]";
		if (top_level != null) {
			result = result + top_level.toString();
		}
        return result;
    }

    public List<String> getClusterIds() {
        return clusterIds;
    }

    public void setClusterIds(List<String> clusterIds) {
        this.clusterIds = clusterIds;
    }

    public void addClusterIdToApp(String clusterId) {
        this.clusterIds.add(clusterId);
    }
}
