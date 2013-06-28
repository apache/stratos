/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.utils;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.clustering.management.GroupManagementCommand;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfigurator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.wso2.carbon.core.CarbonAxisConfigurator;
import org.wso2.carbon.core.internal.CarbonCoreDataHolder;
import org.wso2.carbon.core.multitenancy.TenantAxisConfigurator;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.deployment.GhostDeployerUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.UUID;

/**
 * ClusterMessage for sending a deployment repository synchronization request
 */
public class SynchronizeRepositoryRequest extends GroupManagementCommand {

	/**
     * 
     */
	private static final long serialVersionUID = 8717694086109561127L;

	private transient static final Log log = LogFactory.getLog(SynchronizeRepositoryRequest.class);
	private int tenantId;
	private String tenantDomain;
	private UUID messageId;

	public SynchronizeRepositoryRequest() {
	}

	public SynchronizeRepositoryRequest(int tenantId, String tenantDomain, UUID messageId) {
		this.tenantId = tenantId;
		this.tenantDomain = tenantDomain;
		this.messageId = messageId;
	}

	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}

	public void execute(ConfigurationContext configContext) throws ClusteringFault {
		log.info("Received  [" + this + "] ");
		// Run only if the tenant is loaded
		if (tenantId == MultitenantConstants.SUPER_TENANT_ID ||
		    TenantAxisUtils.getTenantConfigurationContexts(configContext).get(tenantDomain) != null) {
			updateDeploymentRepository(configContext);
			doDeployment(configContext);
		}
	}

	private void doDeployment(ConfigurationContext configContext) {
		AxisConfigurator axisConfigurator = configContext.getAxisConfiguration().getConfigurator();
		if (axisConfigurator instanceof CarbonAxisConfigurator) {
			((CarbonAxisConfigurator) axisConfigurator).runDeployment();
		} else if (axisConfigurator instanceof TenantAxisConfigurator) {
			((TenantAxisConfigurator) axisConfigurator).runDeployment();
		}
	}

	private void updateDeploymentRepository(ConfigurationContext configContext) {

		log.info(" Update Deployment Repo...");
		/*
		 * BundleContext bundleContext =
		 * CarbonCoreDataHolder.getInstance().getBundleContext();
		 * ServiceReference reference =
		 * bundleContext.getServiceReference(DeploymentSynchronizer.class.getName
		 * ());
		 * if (reference != null) {
		 * ServiceTracker serviceTracker =
		 * new ServiceTracker(bundleContext,
		 * DeploymentSynchronizer.class.getName(), null);
		 * try {
		 * serviceTracker.open();
		 * for (Object obj : serviceTracker.getServices()) {
		 * // if the update is for worker node with ghost ON, then we will
		 * update the
		 * // whole repo for now. See CARBON-13899
		 * if (GhostDeployerUtils.isGhostOn() && CarbonUtils.isWorkerNode() &&
		 * tenantId > 0) {
		 * String repoPath = MultitenantUtils.getAxis2RepositoryPath(tenantId);
		 * ((DeploymentSynchronizer) obj).update(repoPath, repoPath, 3);
		 * } else {
		 * ((DeploymentSynchronizer) obj).update(tenantId);
		 * }
		 * }
		 * } catch (Exception e) {
		 * log.error("Repository update failed for tenant " + tenantId, e);
		 * setRepoUpdateFailed(configContext);
		 * } finally {
		 * serviceTracker.close();
		 * }
		 * }
		 */
	}

	private void setRepoUpdateFailed(ConfigurationContext configContext) {
		AxisConfigurator axisConfigurator = configContext.getAxisConfiguration().getConfigurator();
		if (axisConfigurator instanceof CarbonAxisConfigurator) {
			((CarbonAxisConfigurator) axisConfigurator).setRepoUpdateFailed();
		} else if (axisConfigurator instanceof TenantAxisConfigurator) {
			((TenantAxisConfigurator) axisConfigurator).setRepoUpdateFailed();
		}
	}

	public ClusteringCommand getResponse() {
		return null;
	}

	@Override
	public String toString() {
		return "SynchronizeRepositoryRequest{" + "tenantId=" + tenantId + ", tenantDomain='" +
		       tenantDomain + '\'' + ", messageId=" + messageId + '}';
	}
}
