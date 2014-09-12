package org.apache.stratos.autoscaler.monitor;

import java.util.Properties;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.KubernetesClusterContext;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

public class KubernetesClusterMonitor implements Runnable{
	
	private static final Log log = LogFactory.getLog(KubernetesClusterMonitor.class);

	protected KubernetesClusterContext kubernetesClusterCtxt;
	protected String clusterId;
	protected String serviceId;
	protected AutoscalePolicy autoscalePolicy;
	protected int monitorInterval;
	protected boolean isDestroyed;
    private ClusterStatus status;
    private String lbReferenceType;
    private boolean hasPrimary;
    private int numberOfReplicasInServiceCluster = 0;
	
    public KubernetesClusterMonitor(KubernetesClusterContext kubernetesClusterCtxt, String serviceClusterID, String serviceId, 
    		AutoscalePolicy autoscalePolicy) {
    	this.clusterId = serviceClusterID;
    	this.serviceId = serviceId;
    	this.kubernetesClusterCtxt = kubernetesClusterCtxt;
    	this.autoscalePolicy = autoscalePolicy;
        readConfigurations();
    }

    private void readConfigurations () {
    	// same as VM cluster monitor interval
        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
        monitorInterval = conf.getInt(AutoScalerConstants.AUTOSCALER_MONITOR_INTERVAL, 90000);
        if (log.isDebugEnabled()) {
            log.debug("Kubernetes Cluster Monitor task interval: " + getMonitorInterval());
        }
    }
	
	@Override
	public void run() {
		try {
			// TODO make this configurable,
			// this is the delay the min check of normal cluster monitor to wait
			// until LB monitor is added
			Thread.sleep(60000);
		} catch (InterruptedException ignore) {
		}

		while (!isDestroyed()) {
			if (log.isDebugEnabled()) {
				log.debug("Kubernetes cluster monitor is running.. " + this.toString());
			}
			try {
				if (!ClusterStatus.In_Maintenance.equals(status)) {
					monitor();
				} else {
					if (log.isDebugEnabled()) {
						log.debug("Kubernetes cluster monitor is suspended as the cluster is in "
								+ ClusterStatus.In_Maintenance + " mode......");
					}
				}
			} catch (Exception e) {
				log.error("Kubernetes cluster monitor: Monitor failed." + this.toString(),
						e);
			}
			try {
				Thread.sleep(monitorInterval);
			} catch (InterruptedException ignore) {
			}
		}
	}
	
	private void monitor() {
		
		String kubernetesClusterId = this.kubernetesClusterCtxt.getKubernetesClusterID();
		
		try {
			TopologyManager.acquireReadLock();
			Properties props = TopologyManager.getTopology().getService(serviceId).getCluster(clusterId).getProperties();
			int minReplicas = Integer.parseInt(props.getProperty(StratosConstants.KUBERNETES_MIN_REPLICAS));

			if (this.numberOfReplicasInServiceCluster < minReplicas) {
				
				int numOfAdditionalReplicas = minReplicas - this.numberOfReplicasInServiceCluster;

				for (int i = 0; i < numOfAdditionalReplicas; i++) {
					try {
						CloudControllerClient.getInstance().createContainer(kubernetesClusterId, clusterId);
						this.numberOfReplicasInServiceCluster++;
					} catch (Throwable e) {
			            String message = "Cannot create a container";
			            log.error(message, e);
			            throw new RuntimeException(message, e);
					}
				}
			}
		} finally {
			TopologyManager.releaseReadLock();
		}
	}

    @Override
    public String toString() {
        return "KubernetesClusterMonitor "
        		+ "[ kubernetesHostClusterId=" + this.kubernetesClusterCtxt.getKubernetesClusterID() 
        		+ ", clusterId=" + clusterId 
        		+ ", serviceId=" + serviceId + "]";
    }
    
	public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
    
    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }
    
	public AutoscalePolicy getAutoscalePolicy() {
		return autoscalePolicy;
	}

	public void setAutoscalePolicy(AutoscalePolicy autoscalePolicy) {
		this.autoscalePolicy = autoscalePolicy;
	}

	public int getMonitorInterval() {
		return monitorInterval;
	}
	
    public ClusterStatus getStatus() {
        return status;
    }

    public void setStatus(ClusterStatus status) {
        this.status = status;
    }
    
    public boolean isDestroyed() {
        return isDestroyed;
    }

    public void setDestroyed(boolean isDestroyed) {
        this.isDestroyed = isDestroyed;
    }

	public String getLbReferenceType() {
		return lbReferenceType;
	}

	public void setLbReferenceType(String lbReferenceType) {
		this.lbReferenceType = lbReferenceType;
	}

	public boolean isHasPrimary() {
		return hasPrimary;
	}

	public void setHasPrimary(boolean hasPrimary) {
		this.hasPrimary = hasPrimary;
	}
}