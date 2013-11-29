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

package org.apache.stratos.autoscaler.partition.deployers;

import java.io.File;
import java.util.List;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.AbstractDeployer;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;

/**
 * 
 * The Axis2 deployer class for party definitions definitions.
 */
public class PartitionDeployer extends AbstractDeployer {
	
	 private static final Log log = LogFactory.getLog(PartitionDeployer.class);
	 
	 private static String fileExt="xml"; //default
	 private static String deployDirectory=null;

	@Override
	public void init(ConfigurationContext context) {
		if(deployDirectory!=null){
			File deployDir = new File(new File(context.getAxisConfiguration().getRepository().getPath()),deployDirectory);
			if(!deployDir.exists()){
				//create policies deployment directory if not exist 
				try {
					deployDir.mkdirs();
				} catch (Exception e) {
					log.error("Unable to create policies deployment directory", e);
				}
			}
		}
	}

	@Override
	public void setDirectory(String dir) {
		deployDirectory = dir;
	}

	@Override
	public void setExtension(String ext) {
		fileExt = ext;
	}
	
	@Override
	public void deploy(DeploymentFileData deploymentFileData) throws DeploymentException {

		File partitionFile = deploymentFileData.getFile();
		log.debug("Started to deploy the policy: " + partitionFile);

		try {
			
			PartitionReader reader = new PartitionReader(partitionFile);
			
			List<Partition> partitionList = reader.getPartitionList();
			

			log.info("Successfully deployed the partition specified at "
					+ deploymentFileData.getAbsolutePath());
			 
		} catch (Exception e) {
			String msg = "Invalid partition artifact at " + deploymentFileData.getAbsolutePath();
			// back up the file
			File fileToBeRenamed = partitionFile;
			fileToBeRenamed.renameTo(new File(deploymentFileData.getAbsolutePath() + ".back"));
			log.error(msg, e);
			throw new DeploymentException(msg, e);
		}
	}
	
	@Override
	public void undeploy(String fileName) throws DeploymentException {
		//TODO undeploy logic
	}
	
	
	

}
