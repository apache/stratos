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

package org.apache.stratos.autoscaler.policy.deployers;

import java.io.File;
import java.util.Iterator;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.policy.InvalidPolicyException;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.autoscaler.policy.model.Partition;
import org.apache.stratos.autoscaler.policy.model.PartitionGroup;

/**
 * 
 * The Reader class for Deployment-policy definitions.
 */
public class DeploymentPolicyReader  extends AbstractPolicyReader<DeploymentPolicy> {
	
	private static final Log log = LogFactory.getLog(DeploymentPolicyReader.class);
	
	
	public DeploymentPolicyReader(File file) {
		super(file);
	}
	
	public DeploymentPolicy read() throws InvalidPolicyException{
		DeploymentPolicy policy = new DeploymentPolicy();
		try {
			OMElement docEle = getDocument();
			if("deploymentPolicy".equalsIgnoreCase(docEle.getLocalName())){
				policy.setId(docEle.getAttributeValue(new QName("id")));
				//Partition-Groups
				OMElement partitionGroupsEle = docEle.getFirstChildWithName(new QName("partitionGroups"));
				Iterator<?> partitionGroupItr = partitionGroupsEle.getChildrenWithLocalName("partitionGroup");
				while(partitionGroupItr.hasNext()){
					Object nextGroup = partitionGroupItr.next();
					if(nextGroup instanceof OMElement){
						OMElement groupEle = (OMElement) nextGroup;
						PartitionGroup group = new PartitionGroup();
						group.setPartitionAlgo(readValue(groupEle, "partitionAlgo"));
						//Partitions
						OMElement partitionsEle = groupEle.getFirstChildWithName(new QName("partitions"));
						Iterator<?> partitionItr = partitionsEle.getChildrenWithLocalName("partition");
						while(partitionItr.hasNext()){
							Object next = partitionItr.next();
							if(next instanceof OMElement){
								OMElement partitionEle = (OMElement) next;
								Partition partition = new Partition();
								partition.setId(partitionEle.getAttributeValue(new QName("id")));
								partition.setPartitionMax(Integer.valueOf(readValue(partitionEle, "max")));
								partition.setPartitionMin(Integer.valueOf(readValue(partitionEle, "min")));
								group.getPartitions().add(partition);
							}
						}
						policy.getPartitionGroups().add(group);
					}
				}
			} else{
				throw new DeploymentException("File is not a valid deployment policy");
			}

		} catch (Exception e){
			log.error("Malformed deployment policy file", e);
			throw new InvalidPolicyException("Malformed deployment policy file",e);
		} finally{
			closeStream();
		}
		return policy;
	}

}
