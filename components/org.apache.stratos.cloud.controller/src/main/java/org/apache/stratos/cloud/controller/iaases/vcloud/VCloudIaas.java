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
package org.apache.stratos.cloud.controller.iaases.vcloud;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.iaases.JcloudsIaas;
import org.apache.stratos.cloud.controller.util.ComputeServiceBuilderUtil;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.vcloud.compute.options.VCloudTemplateOptions;
import org.jclouds.vcloud.domain.network.IpAddressAllocationMode;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.net.URI;

import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.NetworkInterface;
import org.apache.stratos.cloud.controller.iaases.vcloud.VCloudPartitionValidator;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.jclouds.ContextBuilder;
import org.jclouds.rest.RestContext;
import org.jclouds.vcloud.VCloudApi;
import org.jclouds.vcloud.domain.NetworkConnectionSection;
import org.jclouds.vcloud.domain.ovf.VCloudHardDisk;
import org.jclouds.vcloud.domain.NetworkConnection;
import org.jclouds.vcloud.domain.Task;
import org.jclouds.vcloud.domain.Vm;
import org.jclouds.vcloud.domain.network.OrgNetwork;
import org.jclouds.vcloud.domain.ovf.VCloudVirtualHardwareSection;
import org.jclouds.vcloud.features.VAppApi;
import org.jclouds.vcloud.features.VmApi;
import org.jclouds.vcloud.domain.VApp;
import org.jclouds.vcloud.options.InstantiateVAppTemplateOptions;
import org.jclouds.vcloud.domain.DiskAttachOrDetachParams;



public class VCloudIaas extends JcloudsIaas {


	private static final Log log = LogFactory.getLog(VCloudIaas.class);
	
	private static final String SHELL_TYPE = "shellType";
	private static final String SCRIPTS_PATH = "scripts";
	private static final String CUSTOMIZATION_SCRIPT = "customization";
	private static final String PAYLOAD = "PAYLOAD";
	
	public VCloudIaas(IaasProvider iaasProvider) {
		super(iaasProvider);
	}

	@Override
	public void buildComputeServiceAndTemplate() {
		// builds and sets Compute Service
        ComputeService computeService = ComputeServiceBuilderUtil.buildDefaultComputeService(getIaasProvider());
        getIaasProvider().setComputeService(computeService);

		// builds and sets Template
		buildTemplate();

	}

	public void buildTemplate() {
		IaasProvider iaasInfo = getIaasProvider();
		
		if (iaasInfo.getComputeService() == null) {
			String msg = "Compute service is null for IaaS provider: "
					+ iaasInfo.getName();
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}
		
		//get 'native' version of jclouds' vCloud API.
		ComputeServiceContext context = iaasInfo.getComputeService().getContext();
		VCloudApi api = context.unwrapApi(VCloudApi.class);
		


		TemplateBuilder templateBuilder = iaasInfo.getComputeService()
				.templateBuilder();

		// set image id specified
		templateBuilder.imageId(iaasInfo.getImage());

		// build the Template
		Template template = templateBuilder.build();

		// if you wish to auto assign IPs, instance spawning call should be
		// blocking, but if you
		// wish to assign IPs manually, it can be non-blocking.
		// is auto-assign-ip mode or manual-assign-ip mode? - default mode is
		// non-blocking
		boolean blockUntilRunning = Boolean.parseBoolean(iaasInfo
				.getProperty("autoAssignIp"));
		template.getOptions().as(TemplateOptions.class)
				.blockUntilRunning(blockUntilRunning);

		// this is required in order to avoid creation of additional security
		// groups by Jclouds.
		template.getOptions().as(TemplateOptions.class)
				.inboundPorts(22, 80, 8080, 443, 8243);
		
        if (iaasInfo.getNetworkInterfaces() != null) {
            Set<String> networksSet = new LinkedHashSet<String>(iaasInfo.getNetworkInterfaces().length);
            Hashtable<String, NetworkConnection> vcloudNetworkOptions = new Hashtable<String, NetworkConnection>(iaasInfo.getNetworkInterfaces().length);

            int i = 0;
            for (NetworkInterface ni : iaasInfo.getNetworkInterfaces()) {

                String networkUuid = ni.getNetworkUuid();
                String networkName = null;
                IpAddressAllocationMode ipAllocMode = IpAddressAllocationMode.NONE;
                if (ni.getFixedIp() != null && !ni.getFixedIp().equals("")) {
                    ipAllocMode = IpAddressAllocationMode.MANUAL;
                } else {
                    ipAllocMode = IpAddressAllocationMode.POOL;
                }

                //fetch network name.
                try {
                    OrgNetwork orgNet = api.getNetworkApi().getNetwork(new URI(networkUuid));
                    networkName = orgNet.getName();
                } catch (URISyntaxException e) {
                    log.error("Network UUID '" + networkUuid + "' is not a URI/href.");
                }
                NetworkConnection nc = new NetworkConnection(networkName, i, ni.getFixedIp(), null, true,
                        null, //TODO: support fixed Mac addrs.
                        ipAllocMode);
                networksSet.add(networkUuid);
                vcloudNetworkOptions.put(networkUuid, nc);

                i++;
            }
            //new NetworkConnectionSection()

            //VmApi vmApi = api.getVmApi();
            //vmApi.updateNetworkConnectionOfVm();

            template.getOptions().networks(networksSet);
            template.getOptions().as(VCloudTemplateOptions.class).networkConnections(vcloudNetworkOptions);
        }

		//template.getOptions().as(VCloudTemplateOptions.class)
		//		.ipAddressAllocationMode(IpAddressAllocationMode.POOL);

		// set Template
		iaasInfo.setTemplate(template);
	}

	@Override
	public void setDynamicPayload(byte[] payload) {
		// in vCloud case we need to run a script
		IaasProvider iaasProvider = getIaasProvider();

		if (iaasProvider.getTemplate() == null || payload == null) {
			if (log.isDebugEnabled()) {
				log.debug("Payload for vCloud not found");
			}
			return;
		}

		String shellType = iaasProvider.getProperty(SHELL_TYPE);

		if (shellType == null || shellType.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("Shell Type for vCloud Customization script not found from properties");
			}
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Shell Type '%s' will be used for vCloud Customization script", shellType));
		}

		// Payload is a String value
		String payloadStr = new String(payload);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Payload '%s' will be used for vCloud Customization script", payload));
		}

		Template template = iaasProvider.getTemplate();

		File scriptPath = new File(CarbonUtils.getCarbonConfigDirPath(), SCRIPTS_PATH);

		File customizationScriptFile = new File(new File(scriptPath, shellType), CUSTOMIZATION_SCRIPT);

		if (!customizationScriptFile.exists()) {
			if (log.isWarnEnabled()) {
				log.warn(String.format("The vCloud Customization script '%s' does not exist",
						customizationScriptFile.getAbsolutePath()));
			}
			return;
		}

		String customizationScript = null;

		try {
			customizationScript = FileUtils.readFileToString(customizationScriptFile);
		} catch (IOException e) {
			if (log.isErrorEnabled()) {
				log.error(
						String.format("Error reading the vCloud Customization script '%s'",
								customizationScriptFile.getAbsolutePath()), e);
			}
		}

		if (StringUtils.isEmpty(customizationScript)) {
			if (log.isDebugEnabled()) {
				log.debug("No content vCloud Customization script not found from properties");
			}
			return;
		}

		// Set payload
		customizationScript = customizationScript.replaceAll(PAYLOAD, payloadStr);

		if (log.isDebugEnabled()) {
			log.debug(String.format("The vCloud Customization script\n%s", customizationScript));
		}
		
		// Ensure the script is run.
		if (customizationScript.length() >= 49000) {
			log.warn("The vCloud customization script is >=49000 bytes in size; uploading dummy script and running real script via ssh.");
			String dummyCustomizationScript = "#!/bin/sh\n"
				+ "#STRATOS: the real customization script will be invoked via ssh, since it exceeds the 49000 byte limit "
				+ "imposed by vCloud;\n"
				+ "#see "
				+ "http://pubs.vmware.com/vcd-55/topic/com.vmware.vcloud.api.doc_55/GUID-1BA3B7C5-B46C-48F7-8704-945BC47A940D.html\n";
			template.getOptions().as(VCloudTemplateOptions.class).customizationScript(dummyCustomizationScript);
			template.getOptions().runScript(customizationScript);
		} else {
			template.getOptions().as(VCloudTemplateOptions.class).customizationScript(customizationScript);
		}

		// Run the script
		//template.getOptions().runScript(customizationScript);
	}

	@Override
	public boolean createKeyPairFromPublicKey(String region, String keyPairName, String publicKey) {

		// TODO
		return false;
	}

	@Override
	public List<String> associateAddresses(NodeMetadata node) {

		// TODO
		return null;

	}

	@Override
	public String associatePredefinedAddress(NodeMetadata node, String ip) {
    	return "";
    }

	@Override
	public void releaseAddress(String ip) {
		// TODO
	}

    @Override
    public boolean isValidRegion(String region) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isValidZone(String region, String zone) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean isValidHost(String zone, String host) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return new VCloudPartitionValidator();
    }

	@Override
	public String createVolume(int sizeGB, String snapshotId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String attachVolume(String instanceId, String volumeId, String deviceName) {
        IaasProvider iaasInfo = getIaasProvider();

        if (StringUtils.isEmpty(volumeId)) {
            log.error("Volume provided to attach can not be null");
        }

        if (StringUtils.isEmpty(instanceId)) {
            log.error("Instance provided to attach can not be null");
        }

        URI instanceIdHref = null;
        URI volumeIdHref = null;
        try {
            //the instanceId format is a bit silly for vCloud.
            instanceIdHref = new URI("https:/" + instanceId);
        } catch (URISyntaxException e) {
            log.error("Failed to attach volume, because the instance id cannot be converted into a url by concatenating "
                + "'https:/' with " + instanceId + ". Full stacktrace: " + e.toString());
            return null;
        }
        try {
            volumeIdHref = new URI(volumeId);
        } catch (URISyntaxException e) {
            log.error("Failed to attach voluume, because the volume id '" + volumeId + "' is not a valid href (URI))"
                    + e.toString());
        }

        //get 'native' version of jclouds' vCloud API.
        ComputeServiceContext context = iaasInfo.getComputeService()
                .getContext();

        VCloudApi api = context.unwrapApi(VCloudApi.class);

        //Disks need to be attached to individual VMs, not vApps. The instanceId is the VApp.
        VAppApi vAppApi = api.getVAppApi();
        Set<Vm> vmsInVapp = vAppApi.getVApp(instanceIdHref).getChildren();
        //Each vApp today has just 1 VM in it. Validate assumption.
        assert(vmsInVapp.size() == 1);
        Vm vm = vmsInVapp.iterator().next();
        URI vmHref = vm.getHref();
        VmApi vmApi = api.getVmApi();

        // invest
        /*
        VCloudHardDisk.Builder hardDiskBuilder = new VCloudHardDisk.Builder();
        VCloudHardDisk hardDisk = hardDiskBuilder.instanceID(volumeId).build();
        VCloudVirtualHardwareSection vvhs = vm.getVirtualHardwareSection();
        VCloudHardDisk.Builder Vchd = new VCloudHardDisk.Builder();
        vvhs.toBuilder().item(Vchd.capacity(3).instanceID("hgfhgf").build()).build();
        VApp va = vAppApi.getVApp(instanceIdHref);

        */ //EO invest
        DiskAttachOrDetachParams params = new DiskAttachOrDetachParams(volumeIdHref);
        Task t = vmApi.attachDisk(vmHref, params);

        log.info(String.format("Volume [id]: %s attachment for instance [id]: %s was successful [status]: Attaching. Iaas : %s, Task: %s", volumeId, instanceId, iaasInfo, t));
        return "Attaching";
	}

	@Override
	public void detachVolume(String instanceId, String volumeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteVolume(String volumeId) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public String getIaasDevice(String device) {
        return device;
    }

}
