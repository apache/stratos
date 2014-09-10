package org.apache.stratos.manager.deploy.cartridge;

import org.apache.stratos.cloud.controller.stub.pojo.CartridgeConfig;
import org.apache.stratos.manager.client.CloudControllerServiceClient;

public class DefaultCartridgeDeployer extends CartridgeDeployer{

	@Override
	protected void preDeployment() {
				
	}

	@Override
	protected void postDeployment() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void deployCartridge(CartridgeConfig cartridgeConfig) {
		try {
			CloudControllerServiceClient cloudControllerServiceClient = CloudControllerServiceClient.getServiceClient();
			cloudControllerServiceClient.deployCartridgeDefinition(cartridgeConfig);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

}
