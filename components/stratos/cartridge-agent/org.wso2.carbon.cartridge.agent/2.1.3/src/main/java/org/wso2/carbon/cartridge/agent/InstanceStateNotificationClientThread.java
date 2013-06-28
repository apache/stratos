package org.wso2.carbon.cartridge.agent;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.instanceinfo.mgt.stub.InstanceInformationManagementServiceStub;
import org.wso2.carbon.cartridge.agent.registrant.Registrant;

import java.rmi.RemoteException;

public class InstanceStateNotificationClientThread implements Runnable {

	private Registrant registrant;
	private String state;

	private static final Log log = LogFactory
			.getLog(InstanceStateNotificationClientThread.class);

	public InstanceStateNotificationClientThread(Registrant registrant,
			String state) {
		this.registrant = registrant;
		this.state = state;
	}

	public void run() {
		try {
			log.info("Instance State is updated to " + state + " "
					+ registrant.getRemoteHost());
			String serviceURL = "https://" + System.getProperty("adc.host")
					+ ":" + System.getProperty("adc.port")
					+ "/services/InstanceInformationManagementService";
			InstanceInformationManagementServiceStub stub = new InstanceInformationManagementServiceStub(
					serviceURL);
			stub.updateInstanceState(registrant.getRemoteHost(), 123,
					registrant.retrieveClusterDomain(), "__$default",
					registrant.getService(), state);
		} catch (AxisFault e) {
            log.warn("Error notifying state " + state + " of registrant " + registrant + " to ADC", e);
		} catch (RemoteException e) {
            log.warn("Error notifying state " + state + " of registrant " + registrant + " to ADC", e);
		}

	}
}
