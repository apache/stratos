/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.iaases.openstack.networking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.domain.FloatingNetwork;
import org.apache.stratos.cloud.controller.domain.FloatingNetworks;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.NetworkInterface;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.util.ComputeServiceBuilderUtil;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.neutron.v2.NeutronApi;
import org.jclouds.openstack.neutron.v2.domain.FloatingIP;
import org.jclouds.openstack.neutron.v2.domain.IP;
import org.jclouds.openstack.neutron.v2.domain.Port;
import org.jclouds.openstack.neutron.v2.extensions.FloatingIPApi;
import org.jclouds.openstack.neutron.v2.features.PortApi;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.inject.Module;

/**
 * Leveraging openstack-neutron networking apis.
 * 
 * @author rajkumar
 */
public class NeutronNetworkingApi implements OpenstackNetworkingApi {

    private static final Log log = LogFactory.getLog(NeutronNetworkingApi.class);

    private final String provider = "openstack-neutron";
    private IaasProvider iaasProvider;
    private NeutronApi neutronApi;
    private PortApi portApi;
    private FloatingIPApi floatingIPApi;

    public NeutronNetworkingApi(IaasProvider iaasProvider) {
        String iaasProviderNullMsg = String.format("Iaas provider is null. Unable to create an instance of %s", 
        		NeutronNetworkingApi.class.getName());
        assertNotNull(iaasProvider, iaasProviderNullMsg);
        this.iaasProvider = iaasProvider;
    }

    @Override
    public List<String> associateAddresses(NodeMetadata node) {
    	
    	assertNotNull(node, "Node cannot be null");

        if (null == neutronApi || null == portApi || null == floatingIPApi) {
            buildNeutronApi();
        }

        // internal network uuid to floating networks map, as defined in cartridge definition
        Map<String, List<FloatingNetwork>> networkUuidToFloatingNetworksMap =
                getNetworkUuidToFloatingNetworksMap(iaasProvider.getNetworkInterfaces());

        // private IP to floating networks map, as defined in cartridge definition
        Map<String, List<FloatingNetwork>> fixedIPToFloatingNetworksMap =
                getFixedIPToFloatingNetworksMap(iaasProvider.getNetworkInterfaces());

        // list of IPs allocated to this node
        List<String> associatedFlotingIPs = new ArrayList<String>();
        
        // wait until node gets private IPs
        while (node.getPrivateAddresses() == null) {
            CloudControllerUtil.sleep(1000);
        }
        
        // loop through all the fixed IPs of this node
        // and see whether we need to assign floating IP to each according to the cartridge deployment
        for (String privateIPOfTheNode : node.getPrivateAddresses()) {
            Port portOfTheFixedIP = getPortByFixedIP(privateIPOfTheNode);
            if (null == portOfTheFixedIP) {
                // we can't assign floating IP if port is null
                // it can't happen, a fixed/private IP can't live without a port
                // but doing a null check to be on the safe side
                if (log.isDebugEnabled()) {
                    String msg = String.format("Port not found for fixed IP %s", privateIPOfTheNode);
                    log.debug(msg);
                }
                continue;
            }
            // get list of floating networks associated with each network interfaces (refer cartridge definition)
            List<FloatingNetwork> floatingNetworks = networkUuidToFloatingNetworksMap.get(portOfTheFixedIP.getNetworkId());
            // if no floating networks is defined for a network interface, no need to assign any floating IPs, skip the current iteration
            if (null == floatingNetworks || floatingNetworks.isEmpty()) {
                // since no floating networks found in networkUuidToFloatingNetworksMap,
                // we will search in fixedIPToFloatingNetworksMap
                floatingNetworks = fixedIPToFloatingNetworksMap.get(privateIPOfTheNode);
                if (null == floatingNetworks || floatingNetworks.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        String msg = String.format("No floating networks defined for the network interface %s", 
                        		portOfTheFixedIP.getNetworkId());
                        log.debug(msg);
                    }
                }
                continue;
            }
            // if floating networks defined for a network interface, assign one floating IP from each floating network
            for (FloatingNetwork floatingNetwork : floatingNetworks) {
                FloatingIP allocatedFloatingIP = null;
                if (floatingNetwork.getNetworkUuid() != null && !floatingNetwork.getNetworkUuid().isEmpty()) {
                    allocatedFloatingIP = assignFloatingIP(portOfTheFixedIP, floatingNetwork.getNetworkUuid());
                } else if (floatingNetwork.getFloatingIP() != null && !floatingNetwork.getFloatingIP().isEmpty()) {
                    allocatedFloatingIP = assignPredefinedFloatingIP(portOfTheFixedIP, floatingNetwork.getFloatingIP());
                } else {
                    String msg = String.format("Neither floating network uuid or floating IP defined for the floating network %s", 
                    		floatingNetwork.getName());
                    log.error(msg);
                    throw new CloudControllerException(msg);
                }
                
                String allocatedFloatingIPNullMsg = String.format("Error occured while assigning floating IP. "
                		+ "Please check whether the floating network %s can be reached from the fixed IP range", floatingNetwork.getNetworkUuid());
                assertNotNull(allocatedFloatingIP, allocatedFloatingIPNullMsg);

                String allocatedFloatingIPAddressNullOrEmptyMsg = String.format("Error occured while assigning floating IP. "
                		+ "Please check whether the floating network %s can be reached from the fixed IP range", floatingNetwork.getNetworkUuid());
                assertNotNullAndNotEmpty(allocatedFloatingIP.getFloatingIpAddress(), allocatedFloatingIPAddressNullOrEmptyMsg);

                associatedFlotingIPs.add(allocatedFloatingIP.getFloatingIpAddress());
            }
        }
        return associatedFlotingIPs;
    }

    @Override
    public String associatePredefinedAddress(NodeMetadata node, String ip) {
    	// we are not considering about the predefined floating IP which are
    	// defined in property section of the cartridge definition
    	// because it doesn't make sense when we are having so many interfaces defined. 
    	// so we don't know which interface to be assigned with this ip
    	// so in neutron network case, we are only considering the predefined floating IPs defined in floating networks section
    	// so we should not call this API for neutron networks
        return null;
    }

    @Override
    public void releaseAddress(String ip) {
    	
    	String iPNotValidMsg = String.format("Unable to release the IP. The given IP is not valid", ip);
    	assertValidIP(ip, iPNotValidMsg);

        if (null == neutronApi || null == portApi || null == floatingIPApi) {
            buildNeutronApi();
        }

        if (log.isDebugEnabled()) {
            String msg = String.format("Trying delete the floating IP %s", ip);
            log.debug(msg);
        }

        FloatingIP floatingIP = getFloatingIPByIPAddress(ip);
        if (null == floatingIP) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Floating IP %s is not found. "
                		+ "It might be already deleted, if instance is already terminated", ip);
                log.debug(msg);
            }
            return;
        }

        boolean deleted = floatingIPApi.delete(floatingIP.getId());
        if (deleted) {
            if (log.isDebugEnabled()) {
                String msg = String.format("Successfully deleted the floating IP %s", ip);
                log.debug(msg);
            }
        } else {
            String msg = String.format("Couldn't release the floating IP %s", ip);
            log.error(msg);
            throw new CloudControllerException(msg);
        }
    }

    /**
     * Assign a {@link FloatingIP} from the given {@link FloatingNetwork} to the given {@link Port}.
     * It will either assign an existing floating IP
     * 		or it will create and assign a new floating IP.
     *
     * @param port                the {@link Port} to which a floating IP to be assigned.
     * @param floatingNetworkUuid the network uuid of the floating network
     *                            from which a floating IP to be chosen/created
     * @return the assigned Floating IP
     */
    private FloatingIP assignFloatingIP(Port port, String floatingNetworkUuid) {
        // checking whether if there are any available floating IPs in the external network
        // if there are any we don't need to create a new one
        ArrayList<FloatingIP> unassignedFloatingIPs = getUnassignedFloatingIPsByNetworkUuid(floatingNetworkUuid);
        
        // we should remove all predefined floating IPs from unassigned list
        // otherwise, these predefined floating IPs can be associated to some other interfaces
        if (unassignedFloatingIPs != null) {
        	if (log.isDebugEnabled()) {
        		String msg = String.format("Unassigned floating IPs from the network %s - %s", 
        				floatingNetworkUuid, unassignedFloatingIPs.toString());
        		log.debug(msg);
    		}
			Iterator<FloatingIP> unassginedFloatingIPsIterator = unassignedFloatingIPs.iterator();
			while (unassginedFloatingIPsIterator.hasNext()) {
				FloatingIP floatingIP = unassginedFloatingIPsIterator.next();
				List<String> allPredefinedFloatingIPs = getAllPredefinedFloatingIPs(iaasProvider.getNetworkInterfaces());
				if (allPredefinedFloatingIPs != null && !allPredefinedFloatingIPs.isEmpty()) {
		        	if (log.isDebugEnabled()) {
		        		String msg = String.format("Predefined  floating IPs - %s found in cartridge", 
		        				allPredefinedFloatingIPs.toString());
		        		log.debug(msg);
		    		}
					Iterator<String> predefinedFloatingIPsIterator = allPredefinedFloatingIPs.iterator();
					while (predefinedFloatingIPsIterator.hasNext()) {
						String floatingIPAddress = predefinedFloatingIPsIterator.next();
						if (floatingIP.getFloatingIpAddress() != null 
								&& floatingIP.getFloatingIpAddress().equals(floatingIPAddress)) {
							unassginedFloatingIPsIterator.remove();
				        	if (log.isDebugEnabled()) {
				        		String msg = String.format("Removed predefined floating IP %s from available floating IPs", 
				        				floatingIPAddress);
				        		log.debug(msg);
				    		}
						}
					}
				}
			}
		}
        
        if (unassignedFloatingIPs == null || unassignedFloatingIPs.isEmpty()) {
            return createAndAssignFloatingIP(port, floatingNetworkUuid);
        }
        
    	if (log.isDebugEnabled()) {
    		String msg = String.format("Available floating IPs from the network %s - %s", 
    				floatingNetworkUuid, unassignedFloatingIPs.toString());
    		log.debug(msg);
		}

        // shuffle and get the last for randomness
        Collections.shuffle(unassignedFloatingIPs);
        FloatingIP selectedFloatingIP = Iterables.getLast(unassignedFloatingIPs);
        
    	if (log.isDebugEnabled()) {
    		String msg = String.format("Floating IP %s is selected among %s from the network %s", 
    				selectedFloatingIP.getFloatingIpAddress(), unassignedFloatingIPs.toString(), floatingNetworkUuid);
    		log.debug(msg);
		}
        
        return updateFloatingIP(selectedFloatingIP, port);
    }

    /**
     * Assign a given FloatingIP address to the given {@link Port}
     * It will verify that the given FloatingIP address is actually exists
     * 		and it is not allocated to any of the port
     *
     * @param port the {@link Port} to which the given FloatingIP address to be assigned
     * @param ip   predefined floating IP address to be assigned
     * @return assigned {@link FloatingIP}
     */
    private FloatingIP assignPredefinedFloatingIP(Port port, String predefinedFloatingIP) {
    	
    	String invalidIPMsg = String.format("Invalid predefined floating IP %s", predefinedFloatingIP);
    	assertValidIP(predefinedFloatingIP, invalidIPMsg);
    	assertNotNull(port, "Invalid port. Port cannot be null");
    	
        FloatingIP floatingIP = getFloatingIPByIPAddress(predefinedFloatingIP);
        
        String floatingIPNullMsg = String.format("No such available floating IP %s found", predefinedFloatingIP);
        assertNotNull(floatingIP, floatingIPNullMsg);

        ArrayList<FloatingIP> availableFloatingIPs = getUnassignedFloatingIPs();
        FloatingIP updatedFloatingIP = null;
        if (availableFloatingIPs.contains(floatingIP)) {
            updatedFloatingIP = updateFloatingIP(floatingIP, port);
        } else {
            String msg = String.format("Predefined floating IP %s is either already allocated to another port %s or unavilable", 
            		predefinedFloatingIP, floatingIP.getPortId());
            log.error(msg);
            throw new CloudControllerException(msg);
        }
        
        return updatedFloatingIP;
    }

    /**
     * Assign the given Floating IP to the given port.
     *
     * @param floatingIP       the Floating IP to be assigned
     * @param portTobeAssigned the port to which the given Floating IP to be assigned
     * @return the updated {@link FloatingIP}
     */
    private FloatingIP updateFloatingIP(FloatingIP floatingIP, Port portTobeAssigned) {
    	
    	assertNotNull(floatingIP, "Cannot update floating IP. Given floating IP is null");
    	String portNotNullMsg = String.format("Cannot update floating IP %s. Given port is null", 
    			floatingIP.getFloatingIpAddress());
    	assertNotNull(portTobeAssigned, portNotNullMsg);
    	
        FloatingIP updatedFloatingIP = null;
        if (log.isDebugEnabled()) {
            String msg = String.format("Trying to assign existing floating IP %s to the port %s", 
            		floatingIP.getFloatingIpAddress(), portTobeAssigned.getId());
            log.debug(msg);
        }
        
        try {
            updatedFloatingIP = floatingIPApi.update(floatingIP.getId(), FloatingIP.UpdateFloatingIP.updateBuilder()
                    .portId(portTobeAssigned.getId())
                    .fixedIpAddress(portTobeAssigned.getFixedIps().iterator().next().getIpAddress())
                    .build());
        } catch (Exception e) {
            String msg = String.format("Error while trying to assign existing floating IP %s to the port %s", 
            		floatingIP.toString(), portTobeAssigned.toString());
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }
        
        String updatedFloatingIPNullMessage = String.format("Unable to assign existing floating IP %s "
        		+ "to the port %s", floatingIP.toString(), portTobeAssigned.toString());
        assertNotNull(updatedFloatingIP, updatedFloatingIPNullMessage);
        
        if (log.isDebugEnabled()) {
            String msg = String.format("Successfully updated the floating IP %s", floatingIP.toString());
            log.debug(msg);
        }
        return updatedFloatingIP;
    }

    /**
     * Create a new Floating IP from the given floating network and assign it to the given port
     *
     * @param port                the port to which a Floating IP to be assigned
     * @param floatingNetworkUuid the network uuid of
     *                            the floating network from which the Floating IP should be created
     * @return the newly created/assigned {@link FloatingIP}
     */
    private FloatingIP createAndAssignFloatingIP(Port port, String floatingNetworkUuid) {
    	
    	assertNotNull(port, "Cannot create floating IP. Invalid port. Port cannot be null");
    	assertNotNullAndNotEmpty(floatingNetworkUuid, "Cannot create floating IP. Invalid floating network uuid. "
    			+ "Floating network uuid cannot be null");
    	
        if (log.isDebugEnabled()) {
            String msg = String.format("Trying to create a floating IP from network %s to assign to the port %s", 
            		floatingNetworkUuid, port.getId());
            log.debug(msg);
        }
        
        FloatingIP.CreateFloatingIP createFip;
        try {
            createFip = FloatingIP.createBuilder(floatingNetworkUuid).portId(port.getId()).build();
        } catch (Exception e) {
            String msg = String.format("Error while getting floating IP builder for the external network %s and port %s", 
            		floatingNetworkUuid, port.toString());
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }
        
        FloatingIP floatingIP = null;
        try {
            floatingIP = floatingIPApi.create(createFip);
        } catch (Exception e) {
            String msg = String.format("Error while creating floating IP for the port %s, from floating network %s", 
            		port.toString(), floatingNetworkUuid);
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }
        
        String msg = String.format("Unable to create a floting IP from network %s", floatingNetworkUuid);
        assertNotNull(floatingIP, msg);
        
        return floatingIP;
    }

    /**
     * Get all unassigned Floating IPs from the given floating network.
     * To get all unassigned Floating IPs from all floating networks
     * 		use {@link #getUnassignedFloatingIPs()}
     *
     * @param networkUuid the network uuid of the floating network
     * @return list of all unassigned {@link FloatingIP} from the given floating network
     */
    private ArrayList<FloatingIP> getUnassignedFloatingIPsByNetworkUuid(final String networkUuid) {
    	if (networkUuid == null || networkUuid.isEmpty()) {
			return null;
		}
        ArrayList<FloatingIP> availableFloatingIPs = Lists
                .newArrayList(Iterables.filter(floatingIPApi.list().concat()
                        .toList(), new Predicate<FloatingIP>() {
                    @Override
                    public boolean apply(FloatingIP arg0) {
                        return arg0.getPortId() == null
                                && arg0.getFloatingNetworkId() != null
                                && arg0.getFloatingNetworkId().equals(networkUuid);
                    }
                }));
        return availableFloatingIPs;
    }

    /**
     * Get all unassigned Floating IPs.
     * To get all unassigned Floating IPs from a floating network
     * 		use {@link #getUnassignedFloatingIPsByNetworkUuid(String)}
     * 
     * @return list of all the unassigned {@link FloatingIP} from all floating networks allocated to this tenant
     */
    private ArrayList<FloatingIP> getUnassignedFloatingIPs() {
        ArrayList<FloatingIP> availableFloatingIPs = Lists
                .newArrayList(Iterables.filter(floatingIPApi.list().concat()
                        .toList(), new Predicate<FloatingIP>() {
                    @Override
                    public boolean apply(FloatingIP arg0) {
                        return arg0.getPortId() == null;
                    }
                }));
        return availableFloatingIPs;
    }

    /**
     * Get the {@link FloatingIP} by its Floating IP Address
     *
     * @param floatingIPAddress the Floating IP Address (a.k.a public IP address)
     * @return the {@link FloatingIP} if found, null otherwise
     */
    private FloatingIP getFloatingIPByIPAddress(final String floatingIPAddress) {
    	if (!isValidIP(floatingIPAddress)) {
			return null;
		}
    	
        Iterable<FloatingIP> floatingIP = Iterables.filter(floatingIPApi.list().concat().toList(),
                new Predicate<FloatingIP>() {
                    @Override
                    public boolean apply(FloatingIP input) {
                        return input.getFloatingIpAddress() != null
                                && input.getFloatingIpAddress().equals(floatingIPAddress);
                    }
                });
        if (floatingIP.iterator().hasNext()) {
            return floatingIP.iterator().next();
        }
        return null;
    }

    /**
     * Get the {@link Port} by its fixed IP
     *
     * @param fixedIP the fixed IP of the port to be retrieved
     * @return the {@link Port} if found, null otherwise
     */
    private Port getPortByFixedIP(final String fixedIP) {
    	if (!isValidIP(fixedIP)) {
			return null;
		}
    	
        Iterable<Port> port = Iterables.filter(portApi.list().concat().toList(),
                new Predicate<Port>() {
                    @Override
                    public boolean apply(Port input) {
                        for (IP ip : input.getFixedIps()) {
                            if (ip.getIpAddress() != null
                                    && ip.getIpAddress().equals(fixedIP)) {
                                return true;
                            }
                        }
                        return false;
                    }
                });

        // a fixed/private IP can be associated with at most one port
        if (port.iterator().hasNext()) {
            return port.iterator().next();
        }
        return null;
    }

    private void buildNeutronApi() {
    	
    	String iaasProviderNullMsg = "IaasProvider is null. Unable to build neutron API";
    	assertNotNull(iaasProvider, iaasProviderNullMsg);

        String region = ComputeServiceBuilderUtil.extractRegion(iaasProvider);
        String regionNullOrEmptyErrorMsg = String.format("Region is not set. Unable to build neutron API for the iaas provider %s", 
        		iaasProvider.getProvider());
        assertNotNullAndNotEmpty(region, regionNullOrEmptyErrorMsg);

        String endpoint = iaasProvider.getProperty(CloudControllerConstants.JCLOUDS_ENDPOINT);
        String endpointNullOrEmptyErrorMsg = String.format("Endpoint is not set. Unable to build neutorn API for the iaas provider %s", 
        		iaasProvider.getProvider());
        assertNotNullAndNotEmpty(endpoint, endpointNullOrEmptyErrorMsg);

        Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());

        try {
            this.neutronApi = ContextBuilder.newBuilder(provider).credentials(iaasProvider.getIdentity(),
                    iaasProvider.getCredential()).endpoint(endpoint).modules(modules).buildApi(NeutronApi.class);
        } catch (Exception e) {
            String msg = String.format("Unable to build neutron API for [provider=%s, identity=%s, credential=%s, endpoint=%s]",
                    provider, iaasProvider.getIdentity(), iaasProvider.getCredential(), endpoint);
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }

        this.portApi = neutronApi.getPortApi(region);
        String portApiNullOrEmptyErrorMessage = String.format("Unable to get port Api from neutron Api for region ", region);
        assertNotNull(portApi, portApiNullOrEmptyErrorMessage);

        this.floatingIPApi = neutronApi.getFloatingIPApi(region).get();
        String floatingIPApiNullOrEmptyErrorMessage = String.format("Unable to get floatingIP Api from neutron Api for region ", region);
        assertNotNull(floatingIPApi, floatingIPApiNullOrEmptyErrorMessage);
    }

    /**
     * Check whether the given Object is null
     * and throw {@link CloudControllerException} if it is null
     *
     * @param object       the object to be null checked
     * @param errorMessage the error message to logged and thrown
     */
    private void assertNotNull(Object object, String errorMessage) {
        if (null == object) {
            log.error(errorMessage);
            throw new CloudControllerException(errorMessage);
        }
    }

    /**
     * Check whether the given String is null or empty
     * and throw {@link CloudControllerException} if it is null or empty
     *
     * @param string       the String to be null/empty checked
     * @param errorMessage the error message to logged and thrown
     */
    private void assertNotNullAndNotEmpty(String string, String errorMessage) {
        if (null == string || string.isEmpty()) {
            log.error(errorMessage);
            throw new CloudControllerException(errorMessage);
        }
    }
    
    /**
     * Validate the given IP address
     * 
     * @param ip the IP to be validated
     * @param errorMessage the error message to be logged and thrown
     */
    private void assertValidIP(String ip, String errorMessage) {
    	if (!isValidIP(ip)) {
			log.error(errorMessage);
			throw new CloudControllerException(errorMessage);
		}
    }
    
    /**
     * Check whether the given IP is valid.
     * 
     * @param ip IP to be validated
     * @return true if valid, false otherwise
     */
    private boolean isValidIP(String ip) {
    	return (ip != null && InetAddresses.isInetAddress(ip));
    }
    
    /**
     * Get networkUuid to list of {@link FloatingNetworks} map.
     * This map will exclude those entries which are not having networkUuid or {@link FloatingNetworks}
     * 
     * @param networkInterfaces array of {@link NetworkInterface}
     * @return networkUuid to list of {@link FloatingNetworks} map
     */
    public Map<String, List<FloatingNetwork>> getNetworkUuidToFloatingNetworksMap(NetworkInterface[] networkInterfaces) {
    	String nwInterfacesNullMsg = "Input NetworkInterface array cannot be null";
    	assertNotNull(networkInterfaces, nwInterfacesNullMsg);
    	
        Map<String, List<FloatingNetwork>> networkInterfaceToFloatingNetworksMap =
                new HashMap<String, List<FloatingNetwork>>();
        for (NetworkInterface networkInterface : networkInterfaces) {
            // if no floating networks defined, skip the network interface from the map
            // because we don't need to care about this network interface when assigning floating IPs
            if (networkInterface.getFloatingNetworks() == null
                    || networkInterface.getFloatingNetworks().getFloatingNetworks() == null
                    || networkInterface.getFloatingNetworks().getFloatingNetworks().length == 0) {
                continue;
            }
            // if no network uuid is defined for a network interface, skip that interface from this map
            if (networkInterface.getNetworkUuid() == null || networkInterface.getNetworkUuid().isEmpty()) {
                continue;
            }
            networkInterfaceToFloatingNetworksMap.put(networkInterface.getNetworkUuid(),
                    Arrays.asList(networkInterface.getFloatingNetworks().getFloatingNetworks()));
        }
        return networkInterfaceToFloatingNetworksMap;
    }

    /**
     * Get fixedIp to list of {@link FloatingNetworks} map.
     * This map will exclude those entries which are not having fixedIp or {@link FloatingNetworks}
     * 
     * @param networkInterfaces array of {@link NetworkInterface}
     * @return fixedIp to list of {@link FloatingNetworks} map
     */
    public Map<String, List<FloatingNetwork>> getFixedIPToFloatingNetworksMap(NetworkInterface[] networkInterfaces) {
    	String nwInterfacesNullMsg = "Input NetworkInterface array cannot be null";
    	assertNotNull(networkInterfaces, nwInterfacesNullMsg);
    	
        Map<String, List<FloatingNetwork>> fixedIPToFloatingNetworksMap =
                new HashMap<String, List<FloatingNetwork>>();
        for (NetworkInterface networkInterface : networkInterfaces) {
            // if no floating networks defined, skip the network interface from the map
            // because we don't need to care about this network interface when assigning floating IPs
            if (networkInterface.getFloatingNetworks() == null
                    || networkInterface.getFloatingNetworks().getFloatingNetworks() == null
                    || networkInterface.getFloatingNetworks().getFloatingNetworks().length == 0) {
                continue;
            }
            // if no fixed IP is defined for a network interface, skip that interface from this map
            if (networkInterface.getFixedIp() == null || networkInterface.getFixedIp().isEmpty()) {
                continue;
            }
            fixedIPToFloatingNetworksMap.put(networkInterface.getFixedIp(),
                    Arrays.asList(networkInterface.getFloatingNetworks().getFloatingNetworks()));
        }
        return fixedIPToFloatingNetworksMap;
    }
    
    /**
     * Get all predefined all floating IPs defined in cartridge definition. 
     * 
     * @param array of {@link NetworkInterface}
     * @return list of predefined floating IPs
     */
    public List<String> getAllPredefinedFloatingIPs(NetworkInterface[] networkInterfaces) {
    	String nwInterfacesNullMsg = "Input NetworkInterface array cannot be null";
    	assertNotNull(networkInterfaces, nwInterfacesNullMsg);
    	
    	List<String> allPredefinedFloatingIPs = new ArrayList<String>();
    	for (NetworkInterface networkInterface : networkInterfaces) {
    		// if no floating networks defined, skip it
    		if (null == networkInterface.getFloatingNetworks()) {
				continue;
			}
            FloatingNetwork[] floatingNetworks = networkInterface.getFloatingNetworks().getFloatingNetworks();
			if (floatingNetworks == null || floatingNetworks.length == 0) {
                continue;
            }
			
            for (FloatingNetwork floatingNetwork : floatingNetworks) {
				String floatingIP = floatingNetwork.getFloatingIP();
				// we are giving more priority to network uuid over fixed floating IPs
				// so if both network uuid and floating IPs defined, we are not going to assign those floating IPs
				// so these can be assigned to some other interfaces
				// hence excluding from predefined floating IPs list
				String networkUuid = floatingNetwork.getNetworkUuid();
				if (networkUuid == null || networkUuid.isEmpty()) {
					if (floatingIP != null && InetAddresses.isInetAddress(floatingIP)) {
						allPredefinedFloatingIPs.add(floatingIP);
					}
				}
			}
		}
    	return allPredefinedFloatingIPs;
    }
}