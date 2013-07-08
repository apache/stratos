package org.apache.stratos.cartridge.mgt.ui;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

import com.google.gson.Gson;

public class RepoNotificationServlet extends HttpServlet {

	private static final long serialVersionUID = 4315990619456849911L;
	private static final Log log = LogFactory
			.getLog(RepoNotificationServlet.class);

	public RepoNotificationServlet() {
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		String payload = request.getParameter("payload");
		log.info(" repository payload received ");

		String repositoryURL;

		Gson gson = new Gson();
		Payload p = gson.fromJson(payload, Payload.class);
		repositoryURL = p.getRepository().getUrl();
		try {
			String backendServerURL = CarbonUIUtil.getServerURL(
					getServletContext(), request.getSession());
			ConfigurationContext configContext = (ConfigurationContext) getServletContext()
					.getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
			String cookie = (String) request.getSession().getAttribute(
					ServerConstants.ADMIN_SERVICE_COOKIE);
			new RepoNotificationClient(cookie, backendServerURL, configContext,
					request.getLocale()).synchronize(repositoryURL);
		} catch (Exception e) {
			log.error("Exception is occurred in synchronize, Reason : "
					+ e.getMessage());
		}

	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) {

		this.doPost(req, res);
	}

}