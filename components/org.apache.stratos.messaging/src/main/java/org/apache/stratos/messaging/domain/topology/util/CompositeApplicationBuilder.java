package org.apache.stratos.messaging.domain.topology.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.*;

import java.util.*;



public class CompositeApplicationBuilder {

	private Map<String, GroupTemp> groups = null;
	private Map<String, Cartridge> cartridgesTypes = null;
	
	private List<ConfigGroup> configGroupArray = null;
	private List<ConfigCartridge> configCartridgeArray = null;
	
	private static Log log = LogFactory.getLog(CompositeApplicationBuilder.class);
	
    public CompositeApplication buildCompositeApplication(Topology topology, String appAlias) {
    	if (log.isDebugEnabled()) {
				log.debug("buildCompositeApplication configComplexApplication with topology " + topology);
			}
		Collection<ConfigCompositeApplication> configComplexApplications = topology.getConfigCompositeApplication();
		CompositeApplication complexApplication = new CompositeApplication();
		if (configComplexApplications.size() > 0) {
			ConfigCompositeApplication [] complexConfigAppArray = new ConfigCompositeApplication[configComplexApplications.size()];
			complexConfigAppArray = configComplexApplications.toArray(complexConfigAppArray);
			
			// assuming only 1 complex app is configured
			
			ConfigCompositeApplication configComplexApplication = complexConfigAppArray[0];
 			if (log.isDebugEnabled()) {
 				log.debug("buildCompositeApplication configComplexApplication" + configComplexApplication);
 			}
 			if (configComplexApplication!= null) {
 				if (log.isDebugEnabled()) {
 					log.debug("buildCompositeApplication configComplexApplication" + configComplexApplication);
 				}
 				setConfigConfiguration(configComplexApplication);
 				
 	        	GroupTemp top_level = this.buildApplication();
 	        	if (top_level != null) {
 	        		if (log.isDebugEnabled()) {
 	    				log.debug("ServiceGroupContext:configComplexApplication toplevel is " + top_level.getAlias());
 	    			}
 	        	} else {
 	        		if (log.isDebugEnabled()) {
 	    				log.debug("buildCompositeApplication toplevel is null");
 	    			}
 	        	}
 	        	if (top_level == null) {
 	        		log.debug("buildCompositeApplication top level group is null ...");
 	        		return null;
 	        	} else {
 	        		log.debug("buildCompositeApplication setting top level group " + top_level.getAlias());
 	        		//complexApplication.setTop_level(top_level);
 	        		complexApplication.setAll_groups(this.groups);
 	        		return complexApplication;
 	        	}
 			} else {
 				log.debug("buildCompositeApplication configComplexApplication is null");
 			}
		} else {
			log.debug("buildCompositeApplication configComplexApplications is null");
			return null;
		}
		return null;
    }
	
	
	
	public GroupTemp buildApplication() {

		for (ConfigGroup configGroup : configGroupArray) {
			log.debug("deploying group " + configGroup.getAlias());
			GroupTemp realGroupTemp = new GroupTemp(configGroup.getAlias());
			groups.put(realGroupTemp.getAlias(), realGroupTemp);
		}
		

		for (ConfigCartridge configCartridge : configCartridgeArray) {
			log.debug("deploying cartridge component " + configCartridge.getAlias());
			Cartridge realCartridge = new Cartridge(configCartridge.getAlias());
			cartridgesTypes.put(realCartridge.getAlias(), realCartridge);
		}
		
		// building groups
		// this should be done when reading the topology event in autoscaler
		log.debug("converting group configuration to groups and assembling application");
		for (ConfigGroup configGroup : configGroupArray) {
			GroupTemp assembleGroupTemp = groups.get(configGroup.getAlias());
			Map<String, Cartridge> groupCartridges = new HashMap<String, Cartridge>();
			log.debug("converting configuration for group " + assembleGroupTemp.getAlias());
			for (String key : configGroup.getSubscribables()) {
				GroupTemp realgroup = groups.get(key);
				if (realgroup != null) {
					// try cartridges
					assembleGroupTemp.add(realgroup);
					realgroup.setParent(assembleGroupTemp);
					realgroup.setHomeGroup(assembleGroupTemp);
				} else {
					Cartridge realcartridge_type = cartridgesTypes.get(key);
					if (realcartridge_type != null) {
						// create a copy of the cartridge type
						Cartridge groupCartridge = new Cartridge(realcartridge_type.getAlias());
						groupCartridge.setCartridgeId(getCartridgeId(assembleGroupTemp.getAlias(), realcartridge_type.getAlias()));
						assembleGroupTemp.add(groupCartridge);
						groupCartridge.setParent(assembleGroupTemp);
						groupCartridge.setHomeGroup(assembleGroupTemp); // TODO need to consolidate parent / home group
						groupCartridges.put(groupCartridge.getAlias(), groupCartridge);
						if (log.isDebugEnabled()) {
							log.debug("added new cartrdige of type " + groupCartridge.getAlias() + " and cartrdigeId " + groupCartridge.getCartridgeId() + 
									" to group " + assembleGroupTemp.getAlias());
						}
					} else {
						log.debug("Error: no group, cartridge found for alias: " + key);
					}
				}
			}
			// build dependencies
			log.debug("building dependencies for group " + assembleGroupTemp.getAlias());
			Dependencies real_dependencies = buildDependency(configGroup, groups, groupCartridges);
			assembleGroupTemp.setDependencies(real_dependencies);
			real_dependencies.setGroupTemp(assembleGroupTemp);
		}
		
		GroupTemp application = getTopLevelGroup();
		log.debug("top level group is: " + application.getAlias());
		
		return application;
	}
	
	public GroupTemp getTopLevelGroup () {
		String alias = null;
		for (ConfigGroup configGroup : configGroupArray) {
			alias = configGroup.getAlias();
			boolean isTopLevelGroup = true;
			log.debug("checking if group " + alias + " is a sub component");
			for (ConfigGroup configGroupInner : configGroupArray) {
				for (String key : configGroupInner.getSubscribables()) {
					log.debug("in group " + configGroupInner.getAlias() );
					if (key.equals(alias)) {
						log.debug("found group " + alias + " as sub component in " + configGroupInner.getAlias());
						isTopLevelGroup = false;
						break;
					} 
				}
			}
			if (isTopLevelGroup) {
				log.debug("is top level group: " + alias);
				break;
			}
		}		
		GroupTemp application = groups.get(alias);
		log.debug("top level group is: " + alias);
		return application;
	}
	
	public Dependencies buildDependency(ConfigGroup configGroup, Map<String, GroupTemp> groups, Map<String, Cartridge> groupCartridges) {
		
		// building dependencies
		ConfigDependencies config_dep = configGroup.getDependencies();
		Dependencies real_dependencies = new Dependencies();
		if (config_dep != null) {
		    String kill_behavior = config_dep.getKill_behavior();
		    real_dependencies.setKill_behavior(kill_behavior);
		    List<ConfigDependencies.Pair> startup_order = config_dep.getStartup_order(); 
		    for (ConfigDependencies.Pair pair: startup_order) {
		    	String key = pair.getKey();
		    	String value = pair.getValue();
			    //check groups
			    GroupTemp gr = groups.get(value);
			    log.debug("checking dependency for key " + key + " /val: " + value + " in groups");
			    if (gr != null) {
			    	real_dependencies.addDependency(key, gr);
			    } else {
			    	log.debug("checking dependency for key " + key + " /val: " + value + " in group cartridges");
			    	Cartridge cr = groupCartridges.get(value);
			    	if (cr != null) {
			    		real_dependencies.addDependency(key, cr);
			    		if (log.isDebugEnabled()) {
		    				log.debug("adding group cartridge  " + cr.getCartridgeId()+ " as dependency");
		    			}
			    	} else {
			    		cr = this.cartridgesTypes.get(value);
			    		if (cr != null) {
			    			real_dependencies.addDependency(key, cr);
			    			if (log.isDebugEnabled()) {
			    				log.debug("adding a lone cartridge  " + cr.getAlias() + " as dependency");
			    			}
		    			} else {
		    				log.debug("error retrieving group with name " + key);
		    			}
			    	}
			    }
 
		    }	    
		}
		return real_dependencies;
    }
	
	private  void setConfigConfiguration(ConfigCompositeApplication configApp) {
		this.groups = new HashMap<String, GroupTemp>();
		this.cartridgesTypes = new HashMap<String, Cartridge>();
		
		if (configApp.getComponents() != null) {
			configGroupArray = configApp.getComponents();
		} else {
			configGroupArray = new ArrayList<ConfigGroup>();
		}

		if (configApp.getCartridges() != null) {
			configCartridgeArray = configApp.getCartridges();
		} else {
			configCartridgeArray = new ArrayList<ConfigCartridge>();
		}

	}
	
	private String getCartridgeId (String homeGroupId, String cartridgeType) {
		String id = homeGroupId +":"+ cartridgeType;
		return id;
	}
}
