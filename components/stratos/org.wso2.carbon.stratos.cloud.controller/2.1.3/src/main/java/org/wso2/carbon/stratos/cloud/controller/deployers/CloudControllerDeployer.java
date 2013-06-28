/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.stratos.cloud.controller.deployers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.deployment.AbstractDeployer;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.axis2.deployment.repository.util.DeploymentFileData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.cloud.controller.axiom.AxiomXpathParser;
import org.wso2.carbon.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.wso2.carbon.stratos.cloud.controller.util.IaasProvider;

/**
 * All the {@link IaasProvider}s will get deployed / undeployed / updated via this class. 
 */
public class CloudControllerDeployer extends AbstractDeployer{
    
    private static final Log log = LogFactory.getLog(CloudControllerDeployer.class);
    private static final String FILE_NAME = "cloud-controller";
    private Map<String, List<IaasProvider>> fileToIaasProviderListMap;

    @Override
    public void init(ConfigurationContext arg0) {
    	fileToIaasProviderListMap = new ConcurrentHashMap<String, List<IaasProvider>>();
    }

    @Override
    public void setDirectory(String arg0) {
        // component xml handles this
        
    }

    @Override
    public void setExtension(String arg0) {
        // component xml handles this
    }
    
    public void deploy(DeploymentFileData deploymentFileData) throws DeploymentException {
        
        log.debug("Started to deploy the deployment artifact: "+deploymentFileData.getFile());
        
        // since cloud-controller.xml resides in repository/conf
		if (deploymentFileData.getName().contains(FILE_NAME)) {

			AxiomXpathParser parser = new AxiomXpathParser(deploymentFileData.getFile());

			// parse the file
			parser.parse();

			// deploy iaases
			parser.setIaasProvidersList();
			parser.setDataPublisherRelatedData();
			parser.setTopologySyncRelatedData();

			// update map
			fileToIaasProviderListMap.put(deploymentFileData.getAbsolutePath(),
			                              new ArrayList<IaasProvider>(
			                                                          FasterLookUpDataHolder.getInstance()
			                                                                                .getIaasProviders()));

			log.info("Successfully deployed the cloud-controller XML file specified at " +
			         deploymentFileData.getAbsolutePath());
		}
        
    }
    

    public void undeploy(String file) throws DeploymentException {
        
        if (file.contains(FILE_NAME)) {
            // reset
            FasterLookUpDataHolder.getInstance().setSerializationDir("");
            
            // grab the entry from Map
            if(fileToIaasProviderListMap.containsKey(file)){
                // remove 'em
                FasterLookUpDataHolder.getInstance().getIaasProviders().removeAll(fileToIaasProviderListMap.get(file));
                
                log.info("Successfully undeployed the cloud-controller XML file specified at "+file);
            }
            // only one cloud-controller file, hence delete 'em all
//            FasterLookUpDataHolder.getInstance().setIaasProviders(new ArrayList<IaasProvider>());
            
        }
        
    }

}
