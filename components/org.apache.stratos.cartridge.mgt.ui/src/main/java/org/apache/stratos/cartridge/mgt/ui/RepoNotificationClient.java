package org.apache.stratos.cartridge.mgt.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.reponotification.stub.RepoNotificationServiceException;
import org.apache.stratos.adc.reponotification.stub.RepoNotificationServiceStub;

import java.rmi.RemoteException;
import java.util.Locale;
import java.util.ResourceBundle;

public class RepoNotificationClient {

	private ResourceBundle bundle;
	public RepoNotificationServiceStub stub;
	public static final String BUNDLE = "org.apache.stratos.cartridge.mgt.ui.i18n.Resources";
	private static final Log log = LogFactory.getLog(RepoNotificationClient.class);

	public RepoNotificationClient(String cookie, String backendServerURL,
			ConfigurationContext configCtx, Locale locale) throws AxisFault {
		String serviceURL = backendServerURL + "RepoNotificationService";
		bundle = ResourceBundle.getBundle(BUNDLE, locale);

		stub = new RepoNotificationServiceStub(configCtx, serviceURL);
		ServiceClient client = stub._getServiceClient();
		Options option = client.getOptions();
		option.setManageSession(true);
		option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
		option.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
	}

	public void synchronize(String repositoryURL) throws AxisFault {
		try {
			stub.synchronize(repositoryURL);
		} catch (RemoteException e) {
			handleException("cannot.unsubscribe", e);
		} catch (RepoNotificationServiceException e) {
			handleException("cannot.unsubscribe", e);
		}
	}

	private void handleException(String msgKey, Exception e) throws AxisFault {
		String msg = bundle.getString(msgKey);
		log.error(msg, e);
		throw new AxisFault(msg, e);
	}
}
