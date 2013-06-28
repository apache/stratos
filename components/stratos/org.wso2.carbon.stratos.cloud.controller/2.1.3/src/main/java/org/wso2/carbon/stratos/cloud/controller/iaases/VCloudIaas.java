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
package org.wso2.carbon.stratos.cloud.controller.iaases;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.vcloud.compute.options.VCloudTemplateOptions;
import org.jclouds.vcloud.domain.network.IpAddressAllocationMode;
import org.wso2.carbon.stratos.cloud.controller.exception.CloudControllerException;
import org.wso2.carbon.stratos.cloud.controller.interfaces.Iaas;
import org.wso2.carbon.stratos.cloud.controller.jcloud.ComputeServiceBuilderUtil;
import org.wso2.carbon.stratos.cloud.controller.util.IaasProvider;

public class VCloudIaas extends Iaas{
    
    private static final Log log = LogFactory.getLog(VCloudIaas.class);

    @Override
    public void buildComputeServiceAndTemplate(IaasProvider iaasInfo) {

        // builds and sets Compute Service
        ComputeServiceBuilderUtil.buildDefaultComputeService(iaasInfo);
        
        // builds and sets Template
        buildTemplate(iaasInfo);
        
    }
    
    private void buildTemplate(IaasProvider iaas) {
        if (iaas.getComputeService() == null) {
            String msg = "Compute service is null for IaaS provider: " + iaas.getName();
            log.fatal(msg);
            throw new CloudControllerException(msg);
        }

        TemplateBuilder templateBuilder = iaas.getComputeService().templateBuilder();

        // set image id specified
        templateBuilder.imageId(iaas.getImage());

        // build the Template
        Template template = templateBuilder.build();

		// if you wish to auto assign IPs, instance spawning call should be blocking, but if you
		// wish to assign IPs manually, it can be non-blocking.
		// is auto-assign-ip mode or manual-assign-ip mode? - default mode is non-blocking 
		boolean blockUntilRunning = Boolean.parseBoolean(iaas.getProperty("autoAssignIp"));
		template.getOptions().as(TemplateOptions.class).blockUntilRunning(blockUntilRunning);
		
        // this is required in order to avoid creation of additional security groups by Jclouds.
        template.getOptions().as(TemplateOptions.class).inboundPorts(22, 80, 8080, 443, 8243);

        template.getOptions().as(VCloudTemplateOptions.class).ipAddressAllocationMode(IpAddressAllocationMode.POOL);

        // set Template
        iaas.setTemplate(template);
    }

    @Override
    public void setDynamicPayload(IaasProvider iaasInfo) {

    	// in VCloud case we need to run a script
        if (iaasInfo.getTemplate() != null && iaasInfo.getPayload() != null) {

        	Template template = iaasInfo.getTemplate();
        	String script = "";
        	String launchParams ="", key="";
        	
            // open the zip file stream
            ZipInputStream stream = new ZipInputStream(new ByteArrayInputStream(iaasInfo.getPayload()));

            try
            {

                // now iterate through each item in the stream. The get next
                // entry call will return a ZipEntry for each file in the
                // stream
                ZipEntry entry;
                while((entry = stream.getNextEntry())!=null)
                {
                	StringWriter writer = new StringWriter();
                	IOUtils.copy(stream, writer);
                	
                	if(entry.getName().contains("launch-params")){
                		launchParams = writer.toString();
                	} else if(entry.getName().contains("id_rsa")){
                		key = writer.toString();
                	}
                	
                }
            } catch (IOException e) {
	            log.error(e.getMessage(), e);
            }
            finally
            {
                // we must always close the zip file.
                try {
	                stream.close();
                } catch (IOException e) {

                	log.error("failed to close the ZIP stream", e);
                }
            }
            
            script = "mkdir /var/lib/cloud && mkdir /var/lib/cloud/instance && mkdir /var/lib/cloud/instance/payload && " +
            		 "echo \""+launchParams+"\" > /var/lib/cloud/instance/payload/launch-params && " +
            		 "echo \""+key+"\" > /var/lib/cloud/instance/payload/id_rsa && " +
            		 "cd /opt/ && " +
            		 "chmod 755 wso2-openstack-init.sh && "+
            		 "./wso2-openstack-init.sh";
        	
        	template.getOptions().overrideLoginUser(iaasInfo.getProperty("loginUser")).overrideLoginPassword(iaasInfo.getProperty("loginPassword")).runScript(script);
        }

    }

    @Override
    public boolean createKeyPairFromPublicKey(IaasProvider iaasInfo, String region, String keyPairName,
        String publicKey) {
        
    	//TODO
        return false;
    }

    @Override
    public String associateAddress(IaasProvider iaasInfo, NodeMetadata node) {

    	// TODO
        return "";
        
    }

	@Override
    public void releaseAddress(IaasProvider iaasInfo, String ip) {
		//TODO
    }
	
}
