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

package org.apache.stratos.aws.extension;

import com.amazonaws.services.elasticloadbalancing.model.CreateAppCookieStickinessPolicyResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.aws.extension.exception.PersistenceException;
import org.apache.stratos.aws.extension.persistence.FileBasedPersistenceManager;
import org.apache.stratos.aws.extension.persistence.PersistenceManager;
import org.apache.stratos.aws.extension.persistence.dto.LBInfoDTO;
import org.apache.stratos.load.balancer.common.domain.Cluster;
import org.apache.stratos.load.balancer.common.domain.Member;
import org.apache.stratos.load.balancer.common.domain.Service;
import org.apache.stratos.load.balancer.common.domain.Topology;
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AWSLoadBalancer implements LoadBalancer {

    private static final Log log = LogFactory.getLog(AWSLoadBalancer.class);

    // A map <clusterId, load balancer info dto> to store load balancer information against the cluster id
    private static ConcurrentHashMap<String, LBInfoDTO> clusterIdToLoadBalancerMap = new ConcurrentHashMap<String, LBInfoDTO>();

    // Object used to invoke methods related to AWS API
    private AWSHelper awsHelper;

    // PersistenceManager: used to persist LB Information by tuples of <lb name, cluster id, region>
    PersistenceManager persistenceManager;

    public AWSLoadBalancer() throws LoadBalancerExtensionException {
        awsHelper = new AWSHelper();
        persistenceManager = new FileBasedPersistenceManager();
        initialize();
    }

    /*
     * configure method iterates over topology and configures the AWS load
     * balancers needed. Configuration may involve creating a new load balancer
     * for a cluster, updating existing load balancers or deleting unwanted load
     * balancers.
     */
    public boolean configure(Topology topology)
            throws LoadBalancerExtensionException {

        log.info("AWS load balancer extension is being reconfigured.");

        HashSet<String> activeClusters = new HashSet<String>();

        for (Service service : topology.getServices()) {
            for (Cluster cluster : service.getClusters()) {
                // Check if a load balancer is created for this cluster
                if (clusterIdToLoadBalancerMap.containsKey(cluster.getClusterId())) {
                    // A load balancer is already present for this cluster
                    // Get the load balancer and update it.

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Load balancer for cluster %s is already present.", cluster.getClusterId()));
                    }

	                if(updateExistingLoadBalancer(cluster)){
		                activeClusters.add(cluster.getClusterId());
	                }


                } else {
                    // Create a new load balancer for this cluster
                    Collection<Member> clusterMembers = cluster.getMembers();

                    if (clusterMembers.size() > 0) {

	                    //We assume all the members are in the same region.
                        Member aMember = clusterMembers.iterator().next();

                        // a unique load balancer name with user-defined prefix and a sequence number.
                        String loadBalancerName = awsHelper.generateLoadBalancerName(cluster.getServiceName());

                        String region = awsHelper.getAWSRegion(aMember.getInstanceId());

                        // list of AWS listeners obtained using port mappings of one of the members of the cluster.
                        List<Listener> listenersForThisCluster = awsHelper.getRequiredListeners(aMember);

                        // Get the initial zone identifier list (a, b, c) to consider in creating
                        // the LB as defined in aws.properties file
                        Set<String> initialZones = awsHelper.getInitialZones();

                        Set<String> initialAvailabilityZones = new HashSet<>();
                        if (initialZones.isEmpty()) {
                            // initial availability zones not defined
                            // use the default (<region>a)
                            initialAvailabilityZones.add(awsHelper.getAvailabilityZoneFromRegion(region));
                        } else {
                            // prepend the region and construct the availability zone list with
                            // full names (<region> + <zone>)
                            for (String zone : initialZones) {
                                initialAvailabilityZones.add(region + zone);
                            }
                        }


	                    String loadBalancerDNSName =
			                    createAWSLoadBalancer(loadBalancerName, region, listenersForThisCluster,initialAvailabilityZones);

                        log.info(String.format("Load balancer %s  created for cluster %s " , loadBalancerDNSName, cluster.getClusterId()));

	                    if(addClusterMembersInfo(clusterMembers, loadBalancerName, region)){
		                    activeClusters.add(cluster.getClusterId());
	                    }

                        // persist LB info

	                    LBInfoDTO lbInfoDTO = new LBInfoDTO(loadBalancerName, cluster.getClusterId(), region);
                        try {
                            persistenceManager.persist(lbInfoDTO);

                        } catch (PersistenceException e) {
	                        log.error(String.format(
			                        "Unable to persist LB Information for %s , cluster id %s " + loadBalancerName,
			                        cluster.getClusterId()));
                        }
	                    clusterIdToLoadBalancerMap.put(cluster.getClusterId(), lbInfoDTO);

                    }

                    pause(3000);
                }
            }
        }

        // if 'terminate.lb.on.cluster.removal' = true in aws-extension.sh
        if (AWSExtensionContext.getInstance().terminateLBOnClusterRemoval()) {

            // Find out clusters which were present earlier but are not now.
            List<String> clustersToRemoveFromMap = new ArrayList<String>();
            // TODO: improve using an iterator and removing the unwanted cluster id in this loop
            for (String clusterId : clusterIdToLoadBalancerMap.keySet()) {
                if (!activeClusters.contains(clusterId)) {
                    clustersToRemoveFromMap.add(clusterId);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Load balancer for cluster %s needs to be removed.", clusterId));
                    }

                }
            }


            // Delete load balancers associated with these clusters.
            for (String clusterId : clustersToRemoveFromMap) {
                // Remove load balancer for this cluster.
                final String loadBalancerName = clusterIdToLoadBalancerMap.get(clusterId).getName();
                final String region = clusterIdToLoadBalancerMap.get(clusterId).getRegion();
                awsHelper.deleteLoadBalancer(loadBalancerName, region);
                //remove and persist
                try {
                    persistenceManager.remove(new LBInfoDTO(loadBalancerName, clusterId, region));

                } catch (PersistenceException e) {
                    log.error(String.format("Unable to persist LB Information for[Load Balancer Name] %s [Cluster ID] %s"
                                            ,loadBalancerName, clusterId));
                }
                clusterIdToLoadBalancerMap.remove(clusterId);
            }
        }

        activeClusters.clear();
        log.info("AWS load balancer extension was reconfigured as per the topology.");
        return true;
    }

	private Boolean addClusterMembersInfo(Collection<Member> clusterMembers, String loadBalancerName, String region) {
		Boolean isUpdated=false;
		// Register instances in the cluster to load balancer
		List<Instance> instances = new ArrayList<Instance>();
		List<String> availabilityZones = new ArrayList<String>();

		for (Member member : clusterMembers) {
			isUpdated=true;
		    String instanceId = member.getInstanceId();

		    if (log.isDebugEnabled()) {
		        log.debug("Instance " + awsHelper.getAWSInstanceName(instanceId) + " needs to be registered to load balancer "
		                + loadBalancerName);
		    }

		    Instance instance = new Instance();
		    instance.setInstanceId(awsHelper.getAWSInstanceName(instanceId));

		    instances.add(instance);
		    // LB Common Member has a property 'EC2_AVAILABILITY_ZONE' which points to the ec2 availability
		    // zone for this member. Use the property value to update the LB about the relevant zone
		    String availabilityZone = getEC2AvaialbilityZoneOfMember(member);
		    if (availabilityZone != null) {
		        availabilityZones.add(availabilityZone);
		    }

			// add stickiness policy
			if (awsHelper.getAppStickySessionCookie() != null && !awsHelper.getAppStickySessionCookie().isEmpty()) {
				CreateAppCookieStickinessPolicyResult result = awsHelper.createStickySessionPolicy(loadBalancerName, awsHelper.getAppStickySessionCookie(),
				                                                                                   Constants.STICKINESS_POLICY,
				                                                                                   region);

				if (result != null) {
					// Take a single port mapping from a member, and apply the policy for
					// the LB Listener port (Proxy port of the port mapping)
					awsHelper.applyPolicyToLBListenerPorts(member.getPorts(), loadBalancerName,
					                                       Constants.STICKINESS_POLICY, region);
				}
			}

		}

		awsHelper.registerInstancesToLoadBalancer(loadBalancerName, instances, region);

		// update LB with the zones
		if (!availabilityZones.isEmpty() && !AWSExtensionContext.getInstance().isOperatingInVPC()) {
		    awsHelper.addAvailabilityZonesForLoadBalancer(loadBalancerName, availabilityZones, region);
		}
		return isUpdated;
	}

	private String createAWSLoadBalancer(String loadBalancerName, String region, List<Listener> listenersForThisCluster,
	                                     Set<String> initialAvailabilityZones) throws LoadBalancerExtensionException {
		// Returns DNS name of load balancer which was created.
		// This is used in the domain mapping of this cluster.
		String loadBalancerDNSName = awsHelper.createLoadBalancer(loadBalancerName, listenersForThisCluster,
		        region, initialAvailabilityZones, AWSExtensionContext.getInstance().isOperatingInVPC());

		// enable connection draining (default) and cross zone load balancing (if specified in aws-extension.sh)
		awsHelper.modifyLBAttributes(loadBalancerName, region, AWSExtensionContext.getInstance().
		        isCrossZoneLoadBalancingEnabled(), true);
		// Add the inbound rule the security group of the load balancer
		// For each listener, add a new rule with load balancer port as allowed protocol in the security group.
		// if security group id is defined, directly use that
		for (Listener listener : listenersForThisCluster) {
			int port = listener.getLoadBalancerPort();

			if (awsHelper.getLbSecurityGroupIdDefinedInConfiguration() != null && !awsHelper.getLbSecurityGroupIdDefinedInConfiguration().isEmpty()) {
				for (String protocol : awsHelper.getAllowedProtocolsForLBSecurityGroup()) {
					awsHelper.addInboundRuleToSecurityGroup(awsHelper.getLbSecurityGroupIdDefinedInConfiguration(),
					                                        region, protocol, port);
				}
			} else if (awsHelper.getLbSecurityGroupName() != null && !awsHelper
					.getLbSecurityGroupName().isEmpty()) {
				for (String protocol : awsHelper.getAllowedProtocolsForLBSecurityGroup()) {
					awsHelper.addInboundRuleToSecurityGroup(awsHelper.getSecurityGroupId(awsHelper.getLbSecurityGroupName(),region), region, protocol,port);
				}
			}
		}

		return loadBalancerDNSName;
	}

	private Boolean updateExistingLoadBalancer(Cluster cluster) {
		Boolean isUpdated=false;
		LBInfoDTO lbInfoDTO = clusterIdToLoadBalancerMap.get(cluster.getClusterId());

		String loadBalancerName = lbInfoDTO.getName();
		String region = lbInfoDTO.getRegion();

		// Get all the instances attached - Attach newly added instances to load balancer

		// attachedInstances list is useful in finding out what are the new instances which
		// should be attached to this load balancer.
		List<Instance> attachedInstances = awsHelper.getAttachedInstances(loadBalancerName, region);

		// clusterMembers stores all the members of a cluster.
		Collection<Member> clusterMembers = cluster.getMembers();

		isUpdated= addClusterMembersInfo(clusterMembers, loadBalancerName, region);

		return isUpdated;
	}

	private String getEC2AvaialbilityZoneOfMember(Member member) {
        if (member.getProperties() != null) {
            return member.getProperties().getProperty(Constants.EC2_AVAILABILITY_ZONE_PROPERTY);
        }

        return null;
    }

    /*
     * start method is called after extension if configured first time. Does
     * nothing but logs the message.
     */
    public void start() throws LoadBalancerExtensionException {
        log.info("AWS load balancer extension started.");
    }

    private void initialize() {
        // load persisted LB information
        Set<LBInfoDTO> lbInfo = null;
        try {
            lbInfo = persistenceManager.retrieve();

        } catch (PersistenceException e) {
            log.error("Unable to retrieve persisted LB Information", e);
        }

        if (lbInfo != null) {
            for (LBInfoDTO lbInfoDTO : lbInfo) {
                LoadBalancerDescription lbDesc = awsHelper.getLoadBalancerDescription(lbInfoDTO.getName(),
                        lbInfoDTO.getRegion());
                if (lbDesc != null) {
                    clusterIdToLoadBalancerMap.put(lbInfoDTO.getClusterId(),lbInfoDTO);
                } else {
                    // make debug
                    if (log.isInfoEnabled()) {
                        log.info("Unable to locate LB " + lbInfoDTO.getName());
                    }
                    // remove the persisted entry
                    try {
                        persistenceManager.remove(new LBInfoDTO(lbInfoDTO.getName(), lbInfoDTO.getClusterId(), lbInfoDTO.getRegion()));

                    } catch (PersistenceException e) {
                        log.error("Unable to remove persisted LB Information", e);
                    }
                }

            }
        }
    }

    /*
     * reload method is called every time after extension if configured. Does
     * nothing but logs the message.
     */
    public void reload() throws LoadBalancerExtensionException {
        // Check what is appropriate to do here.
        log.info("AWS load balancer extension reloaded.");
    }

    /*
     * stop method deletes load balancers for all clusters in the topology.
     */
    public void stop() throws LoadBalancerExtensionException {
        // Remove all load balancers if 'terminate.lbs.on.extension.stop' = true in aws-extension.sh
        if (AWSExtensionContext.getInstance().terminateLBsOnExtensionStop()) {
            for (Map.Entry<String, LBInfoDTO> lbInfoEntry : clusterIdToLoadBalancerMap
                    .entrySet()) {
                // Remove load balancer
                awsHelper.deleteLoadBalancer(lbInfoEntry.getValue().getName(),
                        lbInfoEntry.getValue().getRegion());

                // remove the persisted entry
                try {
                    persistenceManager.remove(new LBInfoDTO(lbInfoEntry.getValue().getName(), lbInfoEntry.getKey(),
                            lbInfoEntry.getValue().getRegion()));

                } catch (PersistenceException e) {
                    log.error("Unable to remove persisted LB Information", e);
                }
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info("Not terminating LBs since terminate.lbs.on.extension.stop=false");
            }
        }
    }

    private static void pause(long duration) {
        // sleep to stop AWS Rate Exceeding: Caused by: com.amazonaws.AmazonServiceException: Rate exceeded
        // (Service: AmazonElasticLoadBalancing; Status Code: 400; Error Code: Throttling; Request ID: xxx-xxx)
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {
        }
    }

    public static ConcurrentHashMap<String, LBInfoDTO> getClusterIdToLoadBalancerMap() {
        return clusterIdToLoadBalancerMap;
    }
}

