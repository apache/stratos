package org.apache.stratos.messaging.domain.topology.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.ConfigCartridge;
import org.apache.stratos.messaging.domain.topology.ConfigGroup;
import org.apache.stratos.messaging.domain.topology.ConfigDependencies;
import org.apache.stratos.messaging.domain.topology.Cartridge;
import org.apache.stratos.messaging.domain.topology.Composite;
import org.apache.stratos.messaging.domain.topology.CompositeApplication;
import org.apache.stratos.messaging.domain.topology.ConfigCompositeApplication;
import org.apache.stratos.messaging.domain.topology.Dependencies;
import org.apache.stratos.messaging.domain.topology.Group;
import org.apache.stratos.messaging.domain.topology.Scalable;
import org.apache.stratos.messaging.domain.topology.Topology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class CompositeApplicationBuilder {

	private Map<String, Group> groups = null;
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
 				
 	        	Group top_level = this.buildApplication();
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
	
	
	
	public Group buildApplication() {

		for (ConfigGroup configGroup : configGroupArray) {
			log.debug("deploying group " + configGroup.getAlias());
			Group realGroup = new Group(configGroup.getAlias());
			groups.put(realGroup.getAlias(), realGroup);
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
			Group assembleGroup = groups.get(configGroup.getAlias());
			Map<String, Cartridge> groupCartridges = new HashMap<String, Cartridge>();
			log.debug("converting configuration for group " + assembleGroup.getAlias());
			for (String key : configGroup.getSubscribables()) {
				Group realgroup = groups.get(key);
				if (realgroup != null) {
					// try cartridges
					assembleGroup.add(realgroup);
					realgroup.setParent(assembleGroup);
					realgroup.setHomeGroup(assembleGroup);
				} else {
					Cartridge realcartridge_type = cartridgesTypes.get(key);
					if (realcartridge_type != null) {
						// create a copy of the cartridge type
						Cartridge groupCartridge = new Cartridge(realcartridge_type.getAlias());
						groupCartridge.setCartridgeId(getCartridgeId(assembleGroup.getAlias(), realcartridge_type.getAlias()));
						assembleGroup.add(groupCartridge);
						groupCartridge.setParent(assembleGroup);
						groupCartridge.setHomeGroup(assembleGroup); // TODO need to consolidate parent / home group
						groupCartridges.put(groupCartridge.getAlias(), groupCartridge);
						if (log.isDebugEnabled()) {
							log.debug("added new cartrdige of type " + groupCartridge.getAlias() + " and cartrdigeId " + groupCartridge.getCartridgeId() + 
									" to group " + assembleGroup.getAlias());
						}
					} else {
						log.debug("Error: no group, cartridge found for alias: " + key);
					}
				}
			}
			// build dependencies
			log.debug("building dependencies for group " + assembleGroup.getAlias());
			Dependencies real_dependencies = buildDependency(configGroup, groups, groupCartridges);
			assembleGroup.setDependencies(real_dependencies);
			real_dependencies.setGroup(assembleGroup);
		}
		
		Group application = getTopLevelGroup();
		log.debug("top level group is: " + application.getAlias());
		
		return application;
	}
	
	public Group getTopLevelGroup () {
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
		Group application = groups.get(alias);
		log.debug("top level group is: " + alias);
		return application;
	}
	
	public Dependencies buildDependency(ConfigGroup configGroup, Map<String, Group> groups, Map<String, Cartridge> groupCartridges) {
		
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
			    Group gr = groups.get(value);
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
		this.groups = new HashMap<String, Group>();
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
