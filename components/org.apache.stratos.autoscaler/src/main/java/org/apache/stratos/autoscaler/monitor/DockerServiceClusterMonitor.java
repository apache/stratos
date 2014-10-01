package org.apache.stratos.autoscaler.monitor;

import java.util.Properties;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.KubernetesClusterContext;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.enums.ClusterType;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

public final class DockerServiceClusterMonitor extends ContainerClusterMonitor{
	
	private static final Log log = LogFactory.getLog(DockerServiceClusterMonitor.class);

	private String lbReferenceType;
    private int numberOfReplicasInServiceCluster = 0;
	int retryInterval = 60000;
	
    public DockerServiceClusterMonitor(KubernetesClusterContext kubernetesClusterCtxt, 
    		String serviceClusterID, String serviceId, AutoscalePolicy autoscalePolicy) {
    	super(serviceClusterID, serviceId, ClusterType.DockerServiceCluster, kubernetesClusterCtxt,
    			new AutoscalerRuleEvaluator(), autoscalePolicy);
        readConfigurations();
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
				log.debug("KubernetesServiceClusterMonitor is running.. " + this.toString());
			}
			try {
				if (!ClusterStatus.In_Maintenance.equals(getStatus())) {
					monitor();
				} else {
					if (log.isDebugEnabled()) {
						log.debug("KubernetesServiceClusterMonitor is suspended as the cluster is in "
								+ ClusterStatus.In_Maintenance + " mode......");
					}
				}
			} catch (Exception e) {
				log.error("KubernetesServiceClusterMonitor : Monitor failed." + this.toString(),
						e);
			}
			try {
				Thread.sleep(getMonitorInterval());
			} catch (InterruptedException ignore) {
			}
		}
	}
	
	@Override
	protected void monitor() {
		
	    // is container created successfully?
		boolean success = false;
		String kubernetesClusterId = getKubernetesClusterCtxt().getKubernetesClusterID();
		
		try {
			TopologyManager.acquireReadLock();
			Properties props = TopologyManager.getTopology().getService(getServiceId()).getCluster(getClusterId()).getProperties();
			int minReplicas = Integer.parseInt(props.getProperty(StratosConstants.KUBERNETES_MIN_REPLICAS));
			
			int nonTerminatedMembers = getKubernetesClusterCtxt().getActiveMembers().size() + getKubernetesClusterCtxt().getPendingMembers().size();

			if (nonTerminatedMembers == 0) {
				
				while (!success) {
					try {

						MemberContext memberContext = CloudControllerClient.getInstance().createContainer(kubernetesClusterId, getClusterId());
						if(null != memberContext) {
							getKubernetesClusterCtxt().addPendingMember(memberContext);
							success = true;
							numberOfReplicasInServiceCluster = minReplicas;
							if(log.isDebugEnabled()){
								log.debug(String.format("Pending member added, [member] %s [kub cluster] %s", 
										memberContext.getMemberId(), getKubernetesClusterCtxt().getKubernetesClusterID()));
							}
						} else {
							if (log.isDebugEnabled()) {
								log.debug("Returned member context is null, did not add to pending members");
							}
						}
					} catch (Throwable e) {
						if (log.isDebugEnabled()) {
							String message = "Cannot create a container, will retry in "+(retryInterval/1000)+"s";
							log.debug(message, e);
						}
					}
					
	                try {
	                    Thread.sleep(retryInterval);
	                } catch (InterruptedException e1) {
	                }
				}
			}
		} finally {
			TopologyManager.releaseReadLock();
		}
	}
	
	@Override
	public void destroy() {
        getMinCheckKnowledgeSession().dispose();
        getScaleCheckKnowledgeSession().dispose();
        setDestroyed(true);
        if(log.isDebugEnabled()) {
            log.debug("KubernetesServiceClusterMonitor Drools session has been disposed. "+this.toString());
        }		
	}
	
    @Override
    protected void readConfigurations () {
    	// same as VM cluster monitor interval
        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
        int monitorInterval = conf.getInt(AutoScalerConstants.AUTOSCALER_MONITOR_INTERVAL, 90000);
        setMonitorInterval(monitorInterval);
        if (log.isDebugEnabled()) {
            log.debug("KubernetesServiceClusterMonitor task interval: " + getMonitorInterval());
        }
    }

    @Override
    public String toString() {
        return "KubernetesServiceClusterMonitor "
        		+ "[ kubernetesHostClusterId=" + getKubernetesClusterCtxt().getKubernetesClusterID()
        		+ ", clusterId=" + getClusterId() 
        		+ ", serviceId=" + getServiceId() + "]";
    }
    
	public String getLbReferenceType() {
		return lbReferenceType;
	}

	public void setLbReferenceType(String lbReferenceType) {
		this.lbReferenceType = lbReferenceType;
	}
}