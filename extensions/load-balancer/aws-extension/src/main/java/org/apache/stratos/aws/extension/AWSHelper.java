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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.domain.*;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsResult;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.*;

public class AWSHelper {
	private String awsAccessKey;
	private String awsSecretKey;
	private String lbPrefix;
	private String lbSecurityGroupName;
	private String lbSecurityGroupDescription;
	private String allowedCidrIpForLBSecurityGroup;
	private int statisticsInterval;

	private AtomicInteger lbSequence;

	private List<String> allowedProtocolsForLBSecurityGroup;

	private ConcurrentHashMap<String, String> regionToSecurityGroupIdMap;

	private BasicAWSCredentials awsCredentials;
	private ClientConfiguration clientConfiguration;

	AmazonElasticLoadBalancingClient elbClient;
	AmazonEC2Client ec2Client;
	private AmazonCloudWatchClient cloudWatchClient;

	private static final Log log = LogFactory.getLog(AWSHelper.class);

	public AWSHelper() throws LoadBalancerExtensionException {
		// Read values for awsAccessKey, awsSecretKey etc. from config file

		String awsPropertiesFile = System
				.getProperty(Constants.AWS_PROPERTIES_FILE);

		Properties properties = new Properties();

		InputStream inputStream = null;

		try {
			inputStream = new FileInputStream(awsPropertiesFile);

			properties.load(inputStream);

			this.awsAccessKey = properties
					.getProperty(Constants.AWS_ACCESS_KEY);
			this.awsSecretKey = properties
					.getProperty(Constants.AWS_SECRET_KEY);

			if (this.awsAccessKey.isEmpty() || this.awsSecretKey.isEmpty()) {
				throw new LoadBalancerExtensionException(
						"Invalid AWS credentials.");
			}

			this.lbPrefix = properties.getProperty(Constants.LB_PREFIX);

			if (this.lbPrefix.isEmpty()
					|| this.lbPrefix.length() > Constants.LOAD_BALANCER_PREFIX_MAX_LENGTH) {
				throw new LoadBalancerExtensionException(
						"Invalid load balancer prefix.");
			}

			lbSequence = new AtomicInteger(1);

			this.lbSecurityGroupName = properties
					.getProperty(Constants.LOAD_BALANCER_SECURITY_GROUP_NAME);

			if (this.lbSecurityGroupName.isEmpty()
					|| this.lbSecurityGroupName.length() > Constants.SECURITY_GROUP_NAME_MAX_LENGTH) {
				throw new LoadBalancerExtensionException(
						"Invalid load balancer security group name.");
			}

			this.allowedCidrIpForLBSecurityGroup = properties
					.getProperty(Constants.ALLOWED_CIDR_IP_KEY);

			if (this.allowedCidrIpForLBSecurityGroup.isEmpty()) {
				throw new LoadBalancerExtensionException(
						"Invalid allowed CIDR IP.");
			}

			String allowedProtocols = properties
					.getProperty(Constants.ALLOWED_PROTOCOLS);

			if (allowedProtocols.isEmpty()) {
				throw new LoadBalancerExtensionException(
						"Please specify at least one Internet protocol.");
			}

			String[] protocols = allowedProtocols.split(",");

			this.allowedProtocolsForLBSecurityGroup = new ArrayList<String>();

			for (String protocol : protocols) {
				this.allowedProtocolsForLBSecurityGroup.add(protocol);
			}

			String interval = properties
					.getProperty(Constants.STATISTICS_INTERVAL);

			if (interval == null || interval.isEmpty()) {
				this.statisticsInterval = Constants.STATISTICS_INTERVAL_MULTIPLE_OF;
			} else {
				try {
					this.statisticsInterval = Integer.parseInt(interval);

					if (this.statisticsInterval
							% Constants.STATISTICS_INTERVAL_MULTIPLE_OF != 0) {
						this.statisticsInterval = Constants.STATISTICS_INTERVAL_MULTIPLE_OF;
					}
				} catch (NumberFormatException e) {
					log.warn("Invalid statistics interval. Setting it to 15.");
					this.statisticsInterval = 15;
				}
			}

			this.lbSecurityGroupDescription = Constants.LOAD_BALANCER_SECURITY_GROUP_DESCRIPTION;

			regionToSecurityGroupIdMap = new ConcurrentHashMap<String, String>();

			awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
			clientConfiguration = new ClientConfiguration();

			elbClient = new AmazonElasticLoadBalancingClient(awsCredentials,
					clientConfiguration);

			ec2Client = new AmazonEC2Client(awsCredentials, clientConfiguration);

			cloudWatchClient = new AmazonCloudWatchClient(awsCredentials,
					clientConfiguration);

		} catch (IOException e) {
			log.error("Error reading aws configuration file.");
			throw new LoadBalancerExtensionException(
					"Error reading aws configuration file.", e);
		} finally {
			try {
				inputStream.close();
			} catch (Exception e) {
				log.warn("Failed to close input stream to aws configuration file.");
			}
		}
	}

	public int getStatisticsInterval() {
		return statisticsInterval;
	}

	public int getNextLBSequence() {
		return lbSequence.getAndIncrement();
	}

	public String getLbSecurityGroupName() {
		return lbSecurityGroupName;
	}

	public List<String> getAllowedProtocolsForLBSecurityGroup() {
		return allowedProtocolsForLBSecurityGroup;
	}

	/**
	 * Creates a load balancer and returns its DNS name. Useful when a new
	 * cluster is added.
	 * 
	 * @param name
	 * @param listeners
	 * @param region
	 * @return DNS name of newly created load balancer
	 * @throws LoadBalancerExtensionException
	 */
	public String createLoadBalancer(String name, List<Listener> listeners,
			String region) throws LoadBalancerExtensionException {

		log.info("Creating load balancer " + name);

		CreateLoadBalancerRequest createLoadBalancerRequest = new CreateLoadBalancerRequest(
				name);

		createLoadBalancerRequest.setListeners(listeners);

		Set<String> availabilityZones = new HashSet<String>();
		availabilityZones.add(getAvailabilityZoneFromRegion(region));

		createLoadBalancerRequest.setAvailabilityZones(availabilityZones);

		try {
			String securityGroupId = getSecurityGroupIdForRegion(region);

			List<String> securityGroups = new ArrayList<String>();
			securityGroups.add(securityGroupId);

			createLoadBalancerRequest.setSecurityGroups(securityGroups);

			elbClient.setEndpoint(String.format(
					Constants.ELB_ENDPOINT_URL_FORMAT, region));

			CreateLoadBalancerResult clbResult = elbClient
					.createLoadBalancer(createLoadBalancerRequest);

			return clbResult.getDNSName();

		} catch (AmazonClientException e) {
			throw new LoadBalancerExtensionException(
					"Could not create load balancer " + name, e);
		}

	}

	/**
	 * Deletes the load balancer with the name provided. Useful when a cluster,
	 * with which this load balancer was associated, is removed.
	 * 
	 * @param loadBalancerName
	 * @param region
	 */
	public void deleteLoadBalancer(String loadBalancerName, String region) {

		log.info("Deleting load balancer " + loadBalancerName);

		DeleteLoadBalancerRequest deleteLoadBalancerRequest = new DeleteLoadBalancerRequest();
		deleteLoadBalancerRequest.setLoadBalancerName(loadBalancerName);

		try {
			elbClient.setEndpoint(String.format(
					Constants.ELB_ENDPOINT_URL_FORMAT, region));

			elbClient.deleteLoadBalancer(deleteLoadBalancerRequest);
		} catch (AmazonClientException e) {
			log.error("Could not delete load balancer : " + loadBalancerName, e);
		}
	}

	/**
	 * Attaches provided instances to the load balancer. Useful when new
	 * instances get added to the cluster with which this load balancer is
	 * associated.
	 * 
	 * @param loadBalancerName
	 * @param instances
	 * @param region
	 */
	public void registerInstancesToLoadBalancer(String loadBalancerName,
			List<Instance> instances, String region) {

		log.info("Attaching instance " + instances.get(0)
				+ " to load balancer + " + loadBalancerName);

		RegisterInstancesWithLoadBalancerRequest registerInstancesWithLoadBalancerRequest = new RegisterInstancesWithLoadBalancerRequest(
				loadBalancerName, instances);

		try {
			elbClient.setEndpoint(String.format(
					Constants.ELB_ENDPOINT_URL_FORMAT, region));

			elbClient.registerInstancesWithLoadBalancer(registerInstancesWithLoadBalancerRequest);

		} catch (AmazonClientException e) {
			log.error("Could not register instances to load balancer "
					+ loadBalancerName, e);
		}
	}

	/**
	 * Detaches provided instances from the load balancer, associated with some
	 * cluster. Useful when instances are removed from the cluster with which
	 * this load balancer is associated.
	 * 
	 * @param loadBalancerName
	 * @param instances
	 * @param region
	 */
	public void deregisterInstancesFromLoadBalancer(String loadBalancerName,
			List<Instance> instances, String region) {

		log.info("Detaching instance " + instances.get(0)
				+ " from load balancer + " + loadBalancerName);

		DeregisterInstancesFromLoadBalancerRequest deregisterInstancesFromLoadBalancerRequest = new DeregisterInstancesFromLoadBalancerRequest(
				loadBalancerName, instances);

		try {
			elbClient.setEndpoint(String.format(
					Constants.ELB_ENDPOINT_URL_FORMAT, region));

			elbClient.deregisterInstancesFromLoadBalancer(deregisterInstancesFromLoadBalancerRequest);

		} catch (AmazonClientException e) {
			log.error("Could not de-register instances from load balancer "
					+ loadBalancerName, e);
		}
	}

	/**
	 * Returns description of the load balancer which is helpful in determining
	 * instances, listeners associated with load balancer
	 * 
	 * @param loadBalancerName
	 * @param region
	 * @return description of the load balancer
	 */
	private LoadBalancerDescription getLoadBalancerDescription(
			String loadBalancerName, String region) {

		List<String> loadBalancers = new ArrayList<String>();

		loadBalancers.add(loadBalancerName);

		DescribeLoadBalancersRequest describeLoadBalancersRequest = new DescribeLoadBalancersRequest(
				loadBalancers);

		try {
			elbClient.setEndpoint(String.format(
					Constants.ELB_ENDPOINT_URL_FORMAT, region));

			DescribeLoadBalancersResult result = elbClient
					.describeLoadBalancers(describeLoadBalancersRequest);

			if (result.getLoadBalancerDescriptions() != null
					&& result.getLoadBalancerDescriptions().size() > 0)
				return result.getLoadBalancerDescriptions().get(0);
		} catch (AmazonClientException e) {
			log.error("Could not find description of load balancer "
					+ loadBalancerName, e);
		}

		return null;
	}

	/**
	 * Returns instances attached to the load balancer. Useful when deciding if
	 * all attached instances are required or some should be detached.
	 * 
	 * @param loadBalancerName
	 * @param region
	 * @return list of instances attached
	 */
	public List<Instance> getAttachedInstances(String loadBalancerName,
			String region) {
		try {
			LoadBalancerDescription lbDescription = getLoadBalancerDescription(
					loadBalancerName, region);

			if (lbDescription == null) {
				log.warn("Could not find description of load balancer "
						+ loadBalancerName);
				return null;
			}

			return lbDescription.getInstances();

		} catch (AmazonClientException e) {
			log.error("Could not find instances attached  load balancer "
					+ loadBalancerName, e);
			return null;
		}
	}

	/**
	 * Returns all the listeners attached to the load balancer. Useful while
	 * deciding if all the listeners are necessary or some should be removed.
	 * 
	 * @param loadBalancerName
	 * @param region
	 * @return list of instances attached to load balancer
	 */
	public List<Listener> getAttachedListeners(String loadBalancerName,
			String region) {
		try {
			LoadBalancerDescription lbDescription = getLoadBalancerDescription(
					loadBalancerName, region);

			if (lbDescription == null) {
				log.warn("Could not find description of load balancer "
						+ loadBalancerName);
				return null;
			}

			List<Listener> listeners = new ArrayList<Listener>();

			List<ListenerDescription> listenerDescriptions = lbDescription
					.getListenerDescriptions();

			for (ListenerDescription listenerDescription : listenerDescriptions) {
				listeners.add(listenerDescription.getListener());
			}

			return listeners;

		} catch (AmazonClientException e) {
			log.error("Could not find description of load balancer "
					+ loadBalancerName);
			return null;
		}

	}

	public String getSecurityGroupId(String groupName, String region) {
		if (groupName == null || groupName.isEmpty()) {
			return null;
		}

		DescribeSecurityGroupsRequest describeSecurityGroupsRequest = new DescribeSecurityGroupsRequest();

		List<String> groupNames = new ArrayList<String>();
		groupNames.add(groupName);

		describeSecurityGroupsRequest.setGroupNames(groupNames);

		try {
			ec2Client.setEndpoint(String.format(
					Constants.EC2_ENDPOINT_URL_FORMAT, region));

			DescribeSecurityGroupsResult describeSecurityGroupsResult = ec2Client
					.describeSecurityGroups(describeSecurityGroupsRequest);

			List<SecurityGroup> securityGroups = describeSecurityGroupsResult
					.getSecurityGroups();

			if (securityGroups != null && securityGroups.size() > 0) {
				return securityGroups.get(0).getGroupId();
			}
		} catch (AmazonClientException e) {
			log.debug("Could not describe security groups.", e);
		}

		return null;
	}

	public String createSecurityGroup(String groupName, String description,
			String region) throws LoadBalancerExtensionException {
		if (groupName == null || groupName.isEmpty()) {
			throw new LoadBalancerExtensionException(
					"Invalid Security Group Name.");
		}

		CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest();
		createSecurityGroupRequest.setGroupName(groupName);
		createSecurityGroupRequest.setDescription(description);

		try {
			ec2Client.setEndpoint(String.format(
					Constants.EC2_ENDPOINT_URL_FORMAT, region));

			CreateSecurityGroupResult createSecurityGroupResult = ec2Client
					.createSecurityGroup(createSecurityGroupRequest);

			return createSecurityGroupResult.getGroupId();

		} catch (AmazonClientException e) {
			log.debug("Could not create security group.", e);
			throw new LoadBalancerExtensionException(
					"Could not create security group.", e);
		}

	}

	public void addInboundRuleToSecurityGroup(String groupId, String region,
			String protocol, int port) throws LoadBalancerExtensionException {
		if (groupId == null || groupId.isEmpty()) {
			throw new LoadBalancerExtensionException(
					"Invalid security group Id for addInboundRuleToSecurityGroup.");
		}

		boolean ruleAlreadyPresent = false;

		DescribeSecurityGroupsRequest describeSecurityGroupsRequest = new DescribeSecurityGroupsRequest();

		List<String> groupIds = new ArrayList<String>();
		groupIds.add(groupId);

		describeSecurityGroupsRequest.setGroupIds(groupIds);

		SecurityGroup secirutyGroup = null;

		try {
			ec2Client.setEndpoint(String.format(
					Constants.EC2_ENDPOINT_URL_FORMAT, region));

			DescribeSecurityGroupsResult describeSecurityGroupsResult = ec2Client
					.describeSecurityGroups(describeSecurityGroupsRequest);

			List<SecurityGroup> securityGroups = describeSecurityGroupsResult
					.getSecurityGroups();

			if (securityGroups != null && securityGroups.size() > 0) {
				secirutyGroup = securityGroups.get(0);
			}
		} catch (AmazonClientException e) {
			log.debug("Could not describe security groups.", e);
		}

		if (secirutyGroup != null) {
			List<IpPermission> existingPermissions = secirutyGroup
					.getIpPermissions();

			IpPermission neededPermission = new IpPermission();
			neededPermission.setFromPort(port);
			neededPermission.setToPort(port);
			neededPermission.setIpProtocol(protocol);

			Collection<String> ipRanges = new HashSet<String>();
			ipRanges.add(this.allowedCidrIpForLBSecurityGroup);

			neededPermission.setIpRanges(ipRanges);

			if (existingPermissions.contains(neededPermission)) {
				ruleAlreadyPresent = true;
			}
		}

		if (!ruleAlreadyPresent) {
			AuthorizeSecurityGroupIngressRequest authorizeSecurityGroupIngressRequest = new AuthorizeSecurityGroupIngressRequest();
			authorizeSecurityGroupIngressRequest.setGroupId(groupId);
			authorizeSecurityGroupIngressRequest
					.setCidrIp(this.allowedCidrIpForLBSecurityGroup);
			authorizeSecurityGroupIngressRequest.setFromPort(port);
			authorizeSecurityGroupIngressRequest.setToPort(port);
			authorizeSecurityGroupIngressRequest.setIpProtocol(protocol);

			try {
				ec2Client.setEndpoint(String.format(
						Constants.EC2_ENDPOINT_URL_FORMAT, region));

				ec2Client
						.authorizeSecurityGroupIngress(authorizeSecurityGroupIngressRequest);

			} catch (AmazonClientException e) {

				// if(!e.getMessage().contains("already exist"))
				// {
				throw new LoadBalancerExtensionException(
						"Could not add inbound rule to security group "
								+ groupId + ".");
				// }
			}
		}
	}

	public String getSecurityGroupIdForRegion(String region)
			throws LoadBalancerExtensionException {
		if (region == null)
			return null;

		if (this.regionToSecurityGroupIdMap.contains(region)) {
			return this.regionToSecurityGroupIdMap.get(region);
		} else {
			// Get the the security group id if it is already present.
			String securityGroupId = getSecurityGroupId(
					this.lbSecurityGroupName, region);

			if (securityGroupId == null) {
				securityGroupId = createSecurityGroup(this.lbSecurityGroupName,
						this.lbSecurityGroupDescription, region);
			}

			this.regionToSecurityGroupIdMap.put(region, securityGroupId);

			return securityGroupId;
		}
	}

	/**
	 * @param loadBalancerName
	 * @param region
	 * @param timeInterval
	 *            in seconds
	 * @return
	 */
	public int getRequestCount(String loadBalancerName, String region,
			int timeInterval) {
		int count = 0;

		try {
			GetMetricStatisticsRequest request = new GetMetricStatisticsRequest();
			request.setMetricName(Constants.REQUEST_COUNT_METRIC_NAME);
			request.setNamespace(Constants.CLOUD_WATCH_NAMESPACE_NAME);

			Date currentTime = new DateTime(DateTimeZone.UTC).toDate();
			Date pastTime = new DateTime(DateTimeZone.UTC).minusSeconds(
					timeInterval).toDate();

			request.setStartTime(pastTime);
			request.setEndTime(currentTime);

			request.setPeriod(timeInterval);

			HashSet<String> statistics = new HashSet<String>();
			statistics.add(Constants.SUM_STATISTICS_NAME);
			request.setStatistics(statistics);

			HashSet<Dimension> dimensions = new HashSet<Dimension>();
			Dimension loadBalancerDimension = new Dimension();
			loadBalancerDimension
					.setName(Constants.LOAD_BALANCER_DIMENTION_NAME);
			loadBalancerDimension.setValue(loadBalancerName);
			dimensions.add(loadBalancerDimension);
			request.setDimensions(dimensions);

			cloudWatchClient.setEndpoint(String.format(
					Constants.CLOUD_WATCH_ENDPOINT_URL_FORMAT, region));

			GetMetricStatisticsResult result = cloudWatchClient
					.getMetricStatistics(request);

			List<Datapoint> dataPoints = result.getDatapoints();

			if (dataPoints != null && dataPoints.size() > 0) {
				count = dataPoints.get(0).getSum().intValue();
			}

		} catch (AmazonClientException e) {
			log.error(
					"Could not get request count statistics of load balancer "
							+ loadBalancerName, e);
		}

		return count;
	}

	public int getAllResponsesCount(String loadBalancerName, String region,
			int timeInterval) {
		int total = 0;

		Date currentTime = new DateTime(DateTimeZone.UTC).toDate();
		Date pastTime = new DateTime(DateTimeZone.UTC).minusSeconds(
				timeInterval).toDate();

		total += getResponseCountForMetric(loadBalancerName, region,
				Constants.HTTP_RESPONSE_2XX, pastTime, currentTime,
				timeInterval);
		total += getResponseCountForMetric(loadBalancerName, region,
				Constants.HTTP_RESPONSE_3XX, pastTime, currentTime,
				timeInterval);
		total += getResponseCountForMetric(loadBalancerName, region,
				Constants.HTTP_RESPONSE_4XX, pastTime, currentTime,
				timeInterval);
		total += getResponseCountForMetric(loadBalancerName, region,
				Constants.HTTP_RESPONSE_5XX, pastTime, currentTime,
				timeInterval);

		return total;
	}

	public int getResponseCountForMetric(String loadBalancerName,
			String region, String metricName, Date startTime, Date endTime,
			int timeInterval) {
		int count = 0;

		try {
			GetMetricStatisticsRequest request = new GetMetricStatisticsRequest();
			request.setMetricName(metricName);
			request.setNamespace(Constants.CLOUD_WATCH_NAMESPACE_NAME);

			request.setStartTime(startTime);
			request.setEndTime(endTime);

			request.setPeriod(timeInterval);

			HashSet<String> statistics = new HashSet<String>();
			statistics.add(Constants.SUM_STATISTICS_NAME);
			request.setStatistics(statistics);

			HashSet<Dimension> dimensions = new HashSet<Dimension>();
			Dimension loadBalancerDimension = new Dimension();
			loadBalancerDimension
					.setName(Constants.LOAD_BALANCER_DIMENTION_NAME);
			loadBalancerDimension.setValue(loadBalancerName);
			dimensions.add(loadBalancerDimension);
			request.setDimensions(dimensions);

			cloudWatchClient.setEndpoint(String.format(
					Constants.CLOUD_WATCH_ENDPOINT_URL_FORMAT, region));

			GetMetricStatisticsResult result = cloudWatchClient
					.getMetricStatistics(request);

			List<Datapoint> dataPoints = result.getDatapoints();

			if (dataPoints != null && dataPoints.size() > 0) {
				count = dataPoints.get(0).getSum().intValue();
			}

		} catch (AmazonClientException e) {
			log.error("Could not get the statistics for metric " + metricName
					+ " of load balancer " + loadBalancerName, e);
		}

		return count;
	}

	/**
	 * Returns the Listeners required for the service. Listeners are derived
	 * from the proxy port, port and protocol values of the service.
	 * 
	 * @param service
	 * @return list of listeners required for the service
	 */
	public List<Listener> getRequiredListeners(Member member) {
		List<Listener> listeners = new ArrayList<Listener>();

		Collection<Port> ports = member.getPorts();

		for (Port port : ports) {
			int instancePort = port.getValue();
			int proxyPort = port.getProxy();
			String protocol = port.getProtocol().toUpperCase();
			String instanceProtocol = protocol;

			Listener listener = new Listener(protocol, proxyPort, instancePort);
			listener.setInstanceProtocol(instanceProtocol);

			listeners.add(listener);
		}

		return listeners;
	}

	/**
	 * Constructs name of the load balancer to be associated with the cluster
	 * 
	 * @param clusterId
	 * @return name of the load balancer
	 * @throws LoadBalancerExtensionException
	 */
	public String generateLoadBalancerName()
			throws LoadBalancerExtensionException {
		String name = null;

		name = lbPrefix + getNextLBSequence();

		if (name.length() > Constants.LOAD_BALANCER_NAME_MAX_LENGTH)
			throw new LoadBalancerExtensionException(
					"Load balanacer name length (32 characters) exceeded");

		return name;
	}

	/**
	 * Extract instance id in IaaS side from member instance name
	 * 
	 * @param memberInstanceName
	 * @return instance id in IaaS
	 */
	public String getAWSInstanceName(String memberInstanceName) {
		if (memberInstanceName.contains("/")) {
			return memberInstanceName
					.substring(memberInstanceName.indexOf("/") + 1);
		} else {
			return memberInstanceName;
		}
	}

	/**
	 * Extract IaaS region from member instance name
	 * 
	 * @param memberInstanceName
	 * @return IaaS region to which member belongs
	 */
	public String getAWSRegion(String memberInstanceName) {
		if (memberInstanceName.contains("/")) {
			return memberInstanceName.substring(0,
					memberInstanceName.indexOf("/"));
		} else {
			return null;
		}
	}

	/**
	 * Get availability zone from region
	 * 
	 * @param region
	 * @return Availability zone of IaaS
	 */
	public String getAvailabilityZoneFromRegion(String region) {
		if (region != null) {
			return region + "a";
		} else
			return null;
	}
}
