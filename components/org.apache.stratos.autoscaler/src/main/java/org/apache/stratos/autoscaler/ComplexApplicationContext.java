package org.apache.stratos.autoscaler;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.CompositeApplication;
import org.apache.stratos.messaging.domain.topology.Dependencies;
import org.apache.stratos.messaging.domain.topology.Group;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.util.CompositeApplicationBuilder;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;


public class ComplexApplicationContext {
	
	static {
		is_kill_all_enabled_flag = new HashMap<String, Boolean>();
		is_in_kill_all_transition = new HashSet<String>();
	}
	
	private static Map<String, Boolean> is_kill_all_enabled_flag;
	private static HashSet<String> is_in_kill_all_transition;
	

	
    private static final Log log = LogFactory.getLog(ComplexApplicationContext.class);  

    // return value of true will bring up new instance (all startup dependencies are up and active)
    public boolean checkStartupDependencies (String clusterId) {
    	String serviceType = "undefined";
    	if (log.isDebugEnabled()) {
			log.debug("checkStartupDependenciesY: serviceType " + serviceType + "  + clusterId "+ clusterId);
		}
    	return checkServiceDependencies (serviceType, clusterId, false);
    }
    
    
    public boolean checkStartupDependencies (String serviceType, String clusterId) {
    	if (log.isDebugEnabled()) {
			log.debug("checkStartupDependenciesY: serviceType " + serviceType + "  + clusterId "+ clusterId);
		}
    	return checkServiceDependencies (serviceType, clusterId, false);
    }
    

    // return false will terminate instances
    public boolean checkKillDependencies (String clusterId) {
    	String serviceType = "undefined";
    	if (log.isDebugEnabled()) {
			log.debug("checkKillDependenciesY: serviceType " + serviceType + "  + clusterId "+ clusterId);
		}
    	return checkKillTerminateDependencies (serviceType, clusterId, true);
    }
    
    public boolean checkKillDependencies (String serviceType, String clusterId) {
    	if (log.isDebugEnabled()) {
			log.debug("checkKillDependenciesY: serviceType " + serviceType + "  + clusterId "+ clusterId);
		}
    	return checkKillTerminateDependencies (serviceType, clusterId, true);
    }
    
    
    public boolean checkServiceDependencies (String serviceType, String clusterId, boolean kill_flag) {
    	
    	
		if (log.isDebugEnabled()) {
			log.debug("ServiceGroupContext:checkServiceDependencies for service with XY " + 
				" serviceType "  + serviceType + 
    			" clusterId: " + clusterId  + " kill_flag: " + kill_flag);
		}		

		CompositeApplicationBuilder builder = new CompositeApplicationBuilder();
		CompositeApplication complexApplication = builder.buildCompositeApplication(TopologyManager.getTopology(), null);
		

		// no app configured
		if (complexApplication == null) {
			return true;
		}
		
		String aServiceId = serviceType;
		if (log.isDebugEnabled()) {
			log.debug("checking dependencies for service alias " + aServiceId);
		}

		if (aServiceId == null) {
			if (log.isDebugEnabled()) {
				log.debug("invalid serviceType null for cluster  " + clusterId + "skipping dependency check (returning true)");
			}
			return true;
		} 
		
		List<Group> service_type_groups = complexApplication.findAllGroupsForServiceType(serviceType);

		String clusterGroupFromClusterId = extractClusterGroupFromClusterId(clusterId);
		
		if (clusterGroupFromClusterId == null) {
			if (log.isDebugEnabled()) {
				log.debug("cluster id " + clusterId + " has incompatible name to extract group, skipping dependency check (return true)");
			}
		}
		
		for (Group service_type_group : service_type_groups) {
			// check if cluster is in the group
			if (log.isDebugEnabled()) {
				log.debug(" checking if cluster  " + clusterId + " is in the group " + service_type_group.getAlias() +
						"extracted group from clusterId is " + clusterGroupFromClusterId);
			}
			if (service_type_group.getAlias().equals(clusterGroupFromClusterId)) {
				boolean result_flag = checkServiceDependenciesForServiceType (serviceType, clusterId, kill_flag, service_type_group);
				if (log.isDebugEnabled()) {
					log.debug("cluster is " + clusterId + " is in the group " + service_type_group.getAlias() + " and startup dependency check is " + result_flag);
				}
				return result_flag;
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("no matching group found for cluster  " + clusterId + " skipping dependency check (return true)" );
		}
		
    	return true;
    	
    }
    
    public boolean checkServiceDependenciesForServiceType (String serviceType, String clusterId, boolean kill_flag, Group home_group) {
		
    	String aServiceId = serviceType;
		
		if (home_group == null) {
 			if (log.isDebugEnabled()) {
 				log.debug(" lone cluster without group " + aServiceId + "skip checking and return true (no dependency check)" );
 			}
 			return true;
 		}
		
		
		if (ComplexApplicationContext.isInKillAllTransition(this.getKillInTransitionKey(serviceType, home_group.getAlias()))) {
			if (log.isDebugEnabled()) {
				log.debug(" subscribable " + aServiceId + " is inKillAll transition, not spawning a new instance" );
			}
			return false;
		} else {
			if (log.isDebugEnabled()) {
				log.debug(" subscribable " + aServiceId + " is not inKillAll transition, continue with dependenciy check" );
			}
		}

		Map<String, String> downstreamDependencies = home_group.getDownStreamDependenciesAsMap(aServiceId);
		
		
		if (downstreamDependencies == null || downstreamDependencies.size() == 0) {
			if (log.isDebugEnabled()) {
				log.debug("serviceType " + aServiceId + " has no dependencies, returning true (no kill)");
			}
			return true;
		} 
		
		if (log.isDebugEnabled()) {
 			StringBuffer buf = new StringBuffer();
 			buf.append("downstreamdependencies list: [ ");
 			
 			
			Set<String> downstream_keys = downstreamDependencies.keySet();
 			for (String c : downstream_keys) {
 				String d = downstreamDependencies.get(c);
 				buf.append(c + ", in group:  ").append(downstreamDependencies.get(d));
 			}
 			
 			buf.append("] ").append(" serviceId ").append(aServiceId);
 			log.debug(buf.toString());
 		} 

		
		List<String> in_active_downstreams = this.getServiceSet_StateInActive(downstreamDependencies);	
		if (in_active_downstreams.size() > 0) {
			if (log.isDebugEnabled()) {
				log.debug("found inactive downstream dependencies for serviceType " + aServiceId + " returning false");
				for (String in_active : in_active_downstreams) {
					log.debug("inactive downstream dependency " + in_active + " for " + aServiceId);
				}
			}
			
			return false;
		}

		if (log.isDebugEnabled()) {
			log.debug("returning true for dependency check on serviceType " + serviceType);
		}
		return true;
	}    
    
    // return false will terminate instances
    public boolean checkKillTerminateDependencies (String serviceType, String clusterId, boolean kill_flag) {
		if (log.isDebugEnabled()) {
			log.debug("ServiceGroupContext:checkKillTerminateDependencies for service with X " + 
				" serviceType "  + serviceType + 
    			" clusterId: " + clusterId  + " kill_flag: " + kill_flag);
		}		
		
		if (log.isDebugEnabled()) {
			log.debug("getting app from builder ");
		}
		
		CompositeApplicationBuilder builder = new CompositeApplicationBuilder();
		CompositeApplication complexApplication = builder.buildCompositeApplication(TopologyManager.getTopology(), null);
		
		String aServiceId = serviceType;
		if (log.isDebugEnabled()) {
			log.debug("checking dependencies for service alias " + 
    			aServiceId);
		}

		if (aServiceId == null) {
			if (log.isDebugEnabled()) {
				log.debug("invalid serviceType null for cluster  " + clusterId + "skipping dependency check (returning true)");
			}
			return true;
		} 


		// no app configured, don't terminate
		if (complexApplication == null) {
			return true;
		}

		List<Group> service_type_groups = complexApplication.findAllGroupsForServiceType(serviceType);

		String clusterGroupFromClusterId = extractClusterGroupFromClusterId(clusterId);
		
		if (clusterGroupFromClusterId == null) {
			if (log.isDebugEnabled()) {
				log.debug("cluster id " + clusterId + " has incompatible name to extract group, skipping terminate dependency check (return true)");
			}
		}
		
		for (Group service_type_group : service_type_groups) {
			// check if cluster is in the group
			if (log.isDebugEnabled()) {
				log.debug(" checking if cluster  " + clusterId + " is in the group " + service_type_group.getAlias() +
						"extracted group from clusterId is " + clusterGroupFromClusterId);
			}
			if (service_type_group.getAlias().equals(clusterGroupFromClusterId)) {
				boolean result_flag = checkKillTerminateDependenciesForServiceType (serviceType, clusterId, kill_flag, 
						service_type_group, complexApplication);
				if (log.isDebugEnabled()) {
					log.debug("cluster is " + clusterId + " is in the group " + service_type_group.getAlias() + " and kill dependency check is " + result_flag);
				}
				return result_flag;
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("no matching group found for cluster  " + clusterId + " skipping terminate dependency check (return true)" );
		}
		
		return true;
    }

    // return false will terminate instances
    public boolean checkKillTerminateDependenciesForServiceType (String serviceType, String clusterId, boolean kill_flag, 
    		Group home_group, CompositeApplication complexApplication ) {
		
    	String aServiceId = serviceType;
		
		if (home_group == null) {
 			if (log.isDebugEnabled()) {
 				log.debug(" lone cluster without top level group " + aServiceId + "skip checking and return true (no kill)" );
 			}
 			return true;
 		} else if (home_group.findGroup(aServiceId) == null) {
 			if (log.isDebugEnabled()) {
 				log.debug(" lone cluster without group " + aServiceId + "skip checking and return true (no kill)" );
 			}
 			return true;
 		}

 		
		//Group home_group = complexApplication.getTop_level().findGroup(aServiceId);

		if (log.isDebugEnabled()) {
			log.debug("checking  downstream dependencies for " + aServiceId );
		}
		Map<String, String> downstreamDependencies = null;
		downstreamDependencies = home_group.getDownStreamDependenciesAsMap(aServiceId);
		if (log.isDebugEnabled()) {
 			StringBuffer buf = new StringBuffer();
 			buf.append("downstreamdependencies list: [ ");
 			
 			if (downstreamDependencies != null && downstreamDependencies.keySet().size() > 0) {
 				Set<String> downstream_keys = downstreamDependencies.keySet();
	 			for (String c : downstream_keys) {
	 				String d = downstreamDependencies.get(c);
	 				buf.append(c + ", in group:  ").append(d).append(" ");
	 			}
	 			buf.append("] ").append(" serviceId ").append(aServiceId);
 			} else {
 				buf.append(" downstreamDependencies is null ");
 			}
 			log.debug(buf.toString());
 		}
		
		
		
		if (log.isDebugEnabled()) {
				log.debug("checking  upstream dependencies for " + aServiceId );
		}
		// 2. get upstream dependencies
		Map<String, String> upstreamDependencies = home_group.getUpstreamDependenciesAsMap(aServiceId);
 		
		if (log.isDebugEnabled()) {
 			StringBuffer buf = new StringBuffer();
 			buf.append("upstreamdependencies list: [ ");
 			
 			if (upstreamDependencies != null && upstreamDependencies.keySet().size() > 0) {
 				Set<String> upstream_keys = upstreamDependencies.keySet();
	 			for (String c : upstream_keys) {
	 				String d = upstreamDependencies.get(c);
	 				buf.append(c + ", in group:  ").append(upstreamDependencies.get(d)).append(" ");
	 			}
	 			
	 			buf.append("] ").append(" serviceId ").append(aServiceId);
 			} else {
 				buf.append(" upstreamDependencies is null ");
 			}
 			log.debug(buf.toString());
 		}
;	
 		List<String> in_active_upstreams = this.getServiceSet_StateInActive(upstreamDependencies);
 		
 		if (log.isDebugEnabled()) {
			log.debug("getting list of InActive upstream dependencies for  " + aServiceId + " with size of "  + in_active_upstreams.size());
 		}	
 		
 		String kill_behavior = Dependencies.KILL_UNDEFINED;
 		// return false if instances should be terminated, true if not
 		for (String serviceTypeAlias : in_active_upstreams) {
 			String gr_alias = upstreamDependencies.get(serviceTypeAlias); 
 			Group gr = complexApplication.getGroupFromGroupAlias(gr_alias);
 			if (gr != null) {
 				
	 			kill_behavior = gr.getDependencies().getKill_behavior();
	 			if (kill_behavior.equals(Dependencies.KILL_ALL)) {
	 				if (ComplexApplicationContext.isKillAllEnabled(gr.getAlias())) {
	 					if (log.isDebugEnabled()) {
	 	 	 				log.debug(" isKillAllEnabled is enabled on upstream (kill) for group " + gr.getAlias() + ", disabling kilAll and preventing " + 
	 					              " serviceType " + aServiceId + " in group " + gr.getAlias() + 
	 	 	 						" to spin up a new instance (setting killAllTransitionFlag) ");
	 	 	 			}
	 				    // adding to the killalltransition flag
	 					// building key from alias + group alias
	 					ComplexApplicationContext.setKillAllTransitionFlag(getKillInTransitionKey(serviceTypeAlias,gr.getAlias()));
	 					// building key from alias + group alias
	 					ComplexApplicationContext.setKillAllTransitionFlag(getKillInTransitionKey(aServiceId,home_group.getAlias()));
	 					return false;
	 				} else {
	 					if (log.isDebugEnabled()) {
	 	 	 				log.debug(" isKillAllEnabled is disabled on upstream (no kill) for group " + gr.getAlias() );
	 	 	 			}
	 					return true;
	 				}
	 			} else if (kill_behavior.equals(Dependencies.KILL_DEPENDENTS)) {
	 				if (log.isDebugEnabled()) {
	 	 				log.debug(" continue to check upstream for kill_flag " + kill_behavior + " for group " + gr.getAlias() );
	 	 			}
	 			// continue to check
	 			} else if (kill_behavior.equals(Dependencies.KILL_NONE)) {
	 				if (log.isDebugEnabled()) {
	 	 				log.debug(" continue to check upstream for kill_flag " + kill_behavior + " for group " + gr.getAlias() );
	 	 			}
	 				// continue to check
	 			} else {
	 				if (log.isDebugEnabled()) {
	 	 				log.debug(" continue to check upstream for invalide kill_flag " + kill_behavior + " for group " + gr.getAlias() );
	 	 			}
	 				//continue to check
	 			}
	 		} else {
	 			// skip 
	 			if (log.isDebugEnabled()) {
 	 				log.debug(" no group found for " + serviceTypeAlias + " while loopig over in_active_upstreams" );
 	 			}
	 		}
 			
 		}
 		
 		// check kill_all_enabled flag
 		Map<String, String> all = complexApplication.getAllInPathOfAsMap(aServiceId, home_group);
 		String [] group_with_kill_all_aliases = home_group.findAllGroupsWithKill2(all, Dependencies.KILL_ALL);
 		// "persistent flag for each group"
 		this.updateEnableKillAllFlag(all, group_with_kill_all_aliases);
 		
 		//List<String> in_active_downstreams = this.getClusterSet_StateInActive(dependeciesAliasArray);jj
 		List<String> in_active_downstreams = this.getServiceSet_StateInActive(downstreamDependencies);
 		
 		if (log.isDebugEnabled()) {
			log.debug("getting list of InActive downstream dependencies for  " + aServiceId + " with size of "  + in_active_downstreams.size());
 		}
 		
 		kill_behavior = Dependencies.KILL_UNDEFINED;
 		for (String alias : in_active_downstreams) {
 			Group gr = home_group.findGroup(alias);
 			if (gr !=null) {
	 			kill_behavior = gr.getDependencies().getKill_behavior();
	 			if (kill_behavior.equals(Dependencies.KILL_ALL) ) {
	 				if (log.isDebugEnabled()) {
	 	 				log.debug(" return true on downstream for kill_flag " + kill_behavior + " for group " + gr.getAlias() );
	 	 			}
	 				if (ComplexApplicationContext.isKillAllEnabled(gr.getAlias())) {
	 					if (log.isDebugEnabled()) {
	 	 	 				log.debug(" isKillAllEnabled is enabled on downstream (kill) for group " + gr.getAlias() +
	 	 	 						  " setting killAllTransitionFlag for" + alias);
	 	 	 			}
	 					// adding to the killalltransition flag
	 					ComplexApplicationContext.setKillAllTransitionFlag(alias);
	 					return false;
	 				} else {
	 					if (log.isDebugEnabled()) {
	 	 	 				log.debug(" isKillAllEnabled is disabled on downstream (no kill) for group " + gr.getAlias() );
	 	 	 			}
	 					return true;
	 				}
	 			} else if (kill_behavior.equals(Dependencies.KILL_DEPENDENTS)) {
	 				if (log.isDebugEnabled()) {
	 	 				//log.debug(" continue downstream for kill_flag " + kill_behavior + " for group " + gr.getAlias() );
	 					log.debug(" downstream service(s) is inactive for aServiceId " + aServiceId + 
		 						" returning false (kill) and terminating cluster members" );
	 				}
	 				return false;
	 			} else if (kill_behavior.equals(Dependencies.KILL_NONE)) {
	 				if (log.isDebugEnabled()) {
	 	 				log.debug(" continue downstream to check for kill_flag " + kill_behavior + " for group " + gr.getAlias() );
	 	 			}
	 				// continue to check
	 			} else {
	 				if (log.isDebugEnabled()) {
	 	 				log.debug(" continue downstream to check for invalide kill_flag " + kill_behavior + " for group " + gr.getAlias() );
	 	 			}
	 				//continue to check
	 			}
 			} else {
	 			// skip 
	 			if (log.isDebugEnabled()) {
 	 				log.debug(" no group found for " + alias + " while loopig over in_active_downstreams" );
 	 			}
	 		}
 			
 		}
 		
 		// this cluster
 		Group gr = home_group.findGroup(aServiceId);
 		if (gr == null) {
 			if (log.isDebugEnabled()) {
 				log.debug(" cluster without group, should not reach this code ? for " + aServiceId );
 			}
 			return true;
 		} 
 		
 		kill_behavior = gr.getDependencies().getKill_behavior();
 		
 		if (kill_behavior.equals(Dependencies.KILL_DEPENDENTS)) {
			if (log.isDebugEnabled()) {
 				log.debug(Dependencies.KILL_DEPENDENTS + " check if any downstream cluster is inactive for aServiceId " + aServiceId );
 			}
			if (in_active_downstreams.size() > 0) {
				if (log.isDebugEnabled()) {
	 				log.debug(" downstream cluster(s) is inactive for aServiceId " + aServiceId + 
	 						" returning false (kill) and terminating cluster members" );
	 			}
				return false;
			} else {
				if (log.isDebugEnabled()) {
	 				log.debug(" no downstream cluster(s) is inactive for aServiceId " + aServiceId + 
	 						" returning true (no kill)" );
	 			}
				return true;
			}
 		}
 		
 		
		if (log.isDebugEnabled()) {
			log.debug("returning true (no kill) for down and upstream dependency check on clusterId " + aServiceId);
		}
		return true;
	}
	
    
	private boolean hasClusterActiveMember (Cluster cluster) {
		boolean flag = false;
        if(cluster.isLbCluster()){
        	if (log.isDebugEnabled()) {
        		log.debug("cluster member is lbCluster, not checking " + cluster);
        	}
        }else{
        	if (log.isDebugEnabled()) {
            	log.debug("checking member acitve for " + 
    	    			" clusterId: " + cluster.getClusterId() + 
    	    			" serviceId: " + cluster.getServiceName());
        	}

			Collection<Member> members = cluster.getMembers();
			for (Member member:members) {
				if (log.isDebugEnabled()) {
					log.debug("listing members while checking if active" + 
							member.getMemberId() + 
							" private Ip: " + member.getMemberIp() + 
							" public Ip:" + member.getMemberPublicIp() + 
							" member state: " + member.getStatus());
				}
				if (member.getStatus().equals(MemberStatus.Activated)) {
					log.debug("member ACTIVE found :" + member.getMemberId());
					flag = true;
					break;
				}
			}
			 
            }
        return flag;
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
	
	private String extractClusterGroupFromClusterId (String clusterId) {
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
	

	
	private void updateEnableKillAllFlag(Map<String, String>all_dependencies, String [] group_with_kill_all_aliases) {
		if (log.isDebugEnabled()) {
			log.debug("updating enable_kill_flag ");
		}
		if (group_with_kill_all_aliases == null)  {
			return;
		}
		//if (isClusterSet_StateActive(all_dependencies)) { //
		if (isServiceSet_StateActive(all_dependencies)) {
			for (String alias : group_with_kill_all_aliases) {
				ComplexApplicationContext.setKillAllEnabled(alias);
				if (log.isDebugEnabled()) {
					log.debug("enable  enable_kill_flag for subscribable" + alias);
				}
			}
		//} else if (isClusterSet_StateInActive(all_dependencies)) {
		} else if (isServiceSet_StateInActive(all_dependencies)) {
			for (String alias : group_with_kill_all_aliases) {
				ComplexApplicationContext.resetKillAllEnabled(alias);
				if (log.isDebugEnabled()) {
					log.debug("disable  enable_kill_flag for subscribable" + alias);
				}
			}
			
			// resetting killalltransition flag for all subscribables
			Set<String> key_set = all_dependencies.keySet();
			for (String serviceTypeAlias : key_set) {
				String group_alias = all_dependencies.get(serviceTypeAlias);
				ComplexApplicationContext.resetKillAllTransitionFlag(getKillInTransitionKey(serviceTypeAlias, group_alias));
				if (log.isDebugEnabled()) {
					log.debug("resetting  enable_kill_flag, is_in_kill_all_transition for subscribable " + serviceTypeAlias + " in group " + group_alias);
				}
			}
			
		} else {
			// do nothing
			if (log.isDebugEnabled()) {
				log.debug("leaving enable_kill_flag, is_in_kill_all_transition unchanged ");
			}
		}
		
	}
	
	private String getKillInTransitionKey(String serviceTypeAlias, String gr_alias) {
		return serviceTypeAlias + gr_alias;
	}

	
	private boolean isServiceSet_StateActive(Map<String, String>serviceTypes) {
		List<String> result = getServiceSet_StateActive(serviceTypes);
		if (result.size() == serviceTypes.size()) {
			return true;
		} 
		
		return false;
	}
	
	private boolean isServiceSet_StateInActive(Map<String, String> serviceTypes) {
		List<String> result = getServiceSet_StateInActive(serviceTypes);
		if (result.size() == serviceTypes.size()) {
			return true;
		} 
		
		return false;
	}
	
	
	private List<String> getServiceSet_StateInActive (Map<String, String> serviceTypesMap) {
		List<String> result = new ArrayList<String> ();
		
		if (log.isDebugEnabled()) {
			log.debug("checking ServiceSet_StateInActive " + serviceTypesMap.size());
		}
		
		if (serviceTypesMap == null) {
			if (log.isDebugEnabled()) {
				log.debug("skipping getting set of InActive services, serviceTypes is null ");
			}
			return result;
		} 
		
		if (log.isDebugEnabled()) {
			log.debug("getting set of InActive clusters for serviceTypes (length)" + serviceTypesMap.size());
		}
		
		
		Collection<Service> services = TopologyManager.getTopology().getServices();
		
		Set<String> serviceTypes = serviceTypesMap.keySet();
		
		for (String serviceType : serviceTypes) {
			boolean hasServiceFound = false;
			for(Service service : services) {
				String serviceTypeGroup = serviceTypesMap.get(serviceType);
				if (log.isDebugEnabled()) {
					log.debug("checking inactive state for service " + service.getServiceName() + 
							" with nr_of_clusters: " + service.getClusters().size() + 
							" against serviceType " + serviceType + " in group " + serviceTypeGroup);
				}
				if (serviceType.equals(service.getServiceName())) {
					// matching service type  - check if has active cluster
					if (log.isDebugEnabled()) {
						log.debug("matching service types, checking clusters for service " + serviceType);
					}
					hasServiceFound = true;
					Collection<Cluster> clusters = service.getClusters();
					boolean hasClusterWithActiveMember = false;
					if (clusters.size() > 0) {
						// at least one cluster has to exist for service to exist 
						if (log.isDebugEnabled()) {
							log.debug("service " + service.getServiceName() + " has at least 1 cluster ");
						}
						for (Cluster cluster : clusters) {
							String clusterGroup = extractClusterGroupFromClusterId(cluster.getClusterId());
							if (log.isDebugEnabled()) {
								log.debug("checking (inactive) cluster state for  " + cluster.getClusterId() + " (in group " + clusterGroup + ")" +
										" and serviceType " + serviceType + " (in group " + serviceTypeGroup + ")");
							}
							// TODO if (hasClusterActiveMember (cluster)) {
							// check group cluster is in 
							
							if (clusterGroup != null && clusterGroup.equals(serviceTypeGroup)) {
								if (hasClusterActiveMember (cluster)) { 
									hasClusterWithActiveMember = true;
									if (log.isDebugEnabled()) {
										log.debug("found active cluster for service " + cluster.getClusterId() + " in group " + serviceTypeGroup);
									}
									break;
								}
							}
						}
					} else {
						if (log.isDebugEnabled()) {
							log.debug("service " + service.getServiceName() + " in group " + serviceTypeGroup + " has no cluster, adding as inactive service ");
							hasClusterWithActiveMember = false;
						}
					}
					
					if (!hasClusterWithActiveMember) {
						result.add(serviceType);
						if (log.isDebugEnabled()) {
							log.debug("service has not a clutser with active member, adding " + serviceType + " as inactive");
						}
					}
				} else {
					if (log.isDebugEnabled()) {
						log.debug("service mismatch between " + service.getServiceName() + " and servicetype " + serviceType + " continue to search ");
					}
				}
				
			}
			// covers the case that service hasn't be deployed yet
			if (!hasServiceFound) {
				result.add(serviceType);
				if (log.isDebugEnabled()) {
					log.debug("no matching service found for " + serviceType + " adding as inactive");
				}
			}
		}

		return result;
	}
	
	
	private List<String> getServiceSet_StateActive (Map<String, String> serviceTypesMap) {
		List<String> result = new ArrayList<String> ();
		
		if (log.isDebugEnabled()) {
			log.debug("checking ServiceSet_StateActive " + serviceTypesMap.size());
		}
		
		if (serviceTypesMap == null) {
			if (log.isDebugEnabled()) {
				log.debug("skipping getting set of Active services, serviceTypes is null ");
			}
			return result;
		} 
		
		if (log.isDebugEnabled()) {
			log.debug("getting set of Active clusters for serviceTypes (length)" + serviceTypesMap.size());
		}
		
		
		Collection<Service> services = TopologyManager.getTopology().getServices();
	
		Set<String> serviceTypes = serviceTypesMap.keySet();
		
		for (String serviceType : serviceTypes) {
			boolean hasServiceFound = false;
			for(Service service : services) {
				String serviceTypeGroup = serviceTypesMap.get(serviceType);
				if (log.isDebugEnabled()) {
					log.debug("checking active state for service " + service.getServiceName() + 
							" with nr_of_clusters: " + service.getClusters().size() + 
							" against serviceType " + serviceType + " in group " + serviceTypeGroup);
				}
				if (serviceType.equals(service.getServiceName())) {
					// matching service type  - check if has active cluster
					if (log.isDebugEnabled()) {
						log.debug("matching service types, checking clusters for service " + serviceType);
					}
					hasServiceFound = true;
					Collection<Cluster> clusters = service.getClusters();
					boolean hasClusterWithActiveMember = false;
					if (clusters.size() > 0) {
						// at least one cluster has to exist for service to exist 
						if (log.isDebugEnabled()) {
							log.debug("service " + service.getServiceName() + " has at least 1 cluster ");
						}
						for (Cluster cluster : clusters) {
							String clusterGroup = extractClusterGroupFromClusterId(cluster.getClusterId());
							if (log.isDebugEnabled()) {
								log.debug("checking (active) cluster state for  " + cluster.getClusterId() + " (in group " + clusterGroup + ")" +
										" and serviceType " + serviceType + " (in group " + serviceTypeGroup + ")");
							}
							
							if (clusterGroup != null && clusterGroup.equals(serviceTypeGroup) && hasClusterActiveMember (cluster)) {
								hasClusterWithActiveMember = true;
								if (log.isDebugEnabled()) {
									log.debug("found active cluster for service " + cluster.getClusterId() + " in group " + serviceTypeGroup +
											" , adding as active service");
								}
								result.add(serviceType);
								break;
							}
						}
					} else {
						if (log.isDebugEnabled()) {
							log.debug("service " + service.getServiceName() + " has no cluster, skipping service ");
							hasClusterWithActiveMember = false;
						}
					}
					
					if (!hasClusterWithActiveMember) {;
						if (log.isDebugEnabled()) {
							log.debug("service has not a clutser with active member, skipping " + serviceType + " as active service");
						}
					}
				} else {
					if (log.isDebugEnabled()) {
						log.debug("service mismatch between " + service.getServiceName() + " and servicetype " + serviceType + " continue to search ");
					}
				}
				
			}
			// covers the case that service hasn't be deployed yet
			if (!hasServiceFound) {
				if (log.isDebugEnabled()) {
					log.debug("no matching service found for " + serviceType + " skipping as active service");
				}
			}
		}

		return result;
	}
	
	/*
	 * get a list of clusters based on the subscription alias name
	 */
	private List<Cluster> getClusters (String [] clusterAliases) {
		List<Cluster> clusters = new ArrayList<Cluster>();
		
		for (String alias : clusterAliases) {
			Cluster cluster = getClusterFromAlias(alias);
			if (cluster != null) {
				clusters.add(cluster);
			} else {
				if (log.isDebugEnabled()) {
					log.debug("getting a (inactive) cluster retrieved as null for alias " + alias);
				}
			}
		}
		return clusters;
	}
	
	/*
	 * get a cluster based on the subscription alias name
	 */
	private Cluster getClusterFromAlias(String findClusterId) {
		Cluster result = null;
		Collection<Service> services = TopologyManager.getTopology().getServices();
		for(Service service : services) {
			// extract list of clusters, clusterId -> alias
			if (log.isDebugEnabled()) {
				log.debug("ServiceGroupContext:calculateKillBehaviorFlag:service:" + service.getServiceName());
			}
			
			for(Cluster cluster : service.getClusters()) {
				String clusterId = cluster.getClusterId();
				String clusterIdAlias = this.extractAlias(clusterId);
				if (log.isDebugEnabled()) {
					log.debug("looping over cluster " +  clusterId + 
							" extracted alias " + clusterIdAlias);
				}
				
				if (clusterIdAlias != null && findClusterId.equals(clusterIdAlias)) {
					return cluster;
				} else {
					if (log.isDebugEnabled()) {
						log.debug(" ignoring cluster " + clusterId + " in cluster check for " + findClusterId);
					}
				}

			}
		}
		return result;
	}
	
	private static void setKillAllEnabled(String groupId ) { 
    	ComplexApplicationContext.is_kill_all_enabled_flag.put(groupId, true);
    }
	
    private static void resetKillAllEnabled(String groupId ) { 
    	// all cartridges are terminated after kill_all 
    	ComplexApplicationContext.is_kill_all_enabled_flag.put(groupId, false);
    }
    
    private static void resetKillAllTransitionFlag(String alias) {
    	ComplexApplicationContext.is_in_kill_all_transition.remove(alias);
    }
    
    private static void setKillAllTransitionFlag(String alias) {
    	ComplexApplicationContext.is_in_kill_all_transition.add(alias);
    }
    
    public static synchronized Boolean isKillAllEnabled(String groupId) {
    	Boolean flag = false;
    	if (ComplexApplicationContext.is_kill_all_enabled_flag == null) {
    		if (log.isDebugEnabled()) {
				log.debug(" creating new is_kill_all_enabled_flag");
			}
    		ComplexApplicationContext.is_kill_all_enabled_flag = new HashMap<String, Boolean>();
    	} 
        flag = ComplexApplicationContext.is_kill_all_enabled_flag.get(groupId);
        if (flag == null) {
        	if (log.isDebugEnabled()) {
				log.debug(" is_kill_all_enabled_flag not initialized for " + groupId + " initializing with true");
			}
        	
        	ComplexApplicationContext.setKillAllEnabled(groupId);
        	flag = ComplexApplicationContext.is_kill_all_enabled_flag.get(groupId);
        }
        return flag;
    }
    
    
    public static synchronized Boolean isInKillAllTransition(String key) {
    	if (ComplexApplicationContext.is_in_kill_all_transition == null) {
    		if (log.isDebugEnabled()) {
				log.debug(" creating new is_in_kill_all_transition");
			}
    		ComplexApplicationContext.is_in_kill_all_transition = new HashSet<String>();
    	} 
        if (ComplexApplicationContext.is_in_kill_all_transition.contains(key)) {
        	return true;
        }
        return false;
    }
    
}
