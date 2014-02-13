package org.apache.stratos.autoscaler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.monitor.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.LbClusterMonitor;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is there for accumulating cluster details which are not there in Topology
 */
public class AutoscalerContext {
    private static volatile AutoscalerContext instance;
//    Map<String, ClusterContext> clusterContextMap;

    private static final Log log = LogFactory.getLog(AutoscalerContext.class);
    private AutoscalerContext() {
        try {
            setMonitors(new HashMap<String, ClusterMonitor>());
            setLbMonitors(new HashMap<String, LbClusterMonitor>());
        } catch (Exception e) {
            log.error("Rule evaluateMinCheck error", e);
        }
    }
    
    // Map<ClusterId, ClusterMonitor>
    private Map<String, ClusterMonitor> monitors;
    // Map<LBClusterId, LBClusterMonitor>
    private Map<String, LbClusterMonitor> lbMonitors;

	private static class Holder {
		private static final AutoscalerContext INSTANCE = new AutoscalerContext();
	}

	public static AutoscalerContext getInstance() {
		return Holder.INSTANCE;
	}

    public void addMonitor(ClusterMonitor monitor) {
        monitors.put(monitor.getClusterId(), monitor);
    }

    public ClusterMonitor getMonitor(String clusterId) {
        return monitors.get(clusterId);
    }
    
    public boolean moniterExist(String clusterId) {
        return monitors.containsKey(clusterId);
    }
    
    public boolean lbMoniterExist(String clusterId) {
        return lbMonitors.containsKey(clusterId);
    }
    
    public LbClusterMonitor getLBMonitor(String clusterId) {
        return lbMonitors.get(clusterId);
    }

    public ClusterMonitor removeMonitor(String clusterId) {
    	if(!moniterExist(clusterId)) {
    		log.fatal("Cluster monitor not found for cluster id: "+clusterId);
    		return null;
    	}
    	log.info("Removed monitor [cluster id]: " + clusterId);
        return monitors.remove(clusterId);
    }
    public LbClusterMonitor removeLbMonitor(String clusterId) {
    	if(!lbMoniterExist(clusterId)) {
    		log.fatal("LB monitor not found for cluster id: "+clusterId);
    		return null;
    	}
    	log.info("Removed LB monitor [cluster id]: " + clusterId);
        return lbMonitors.remove(clusterId);
    }

    public Map<String, ClusterMonitor> getMonitors() {
        return monitors;
    }


    public void setMonitors(Map<String, ClusterMonitor> monitors) {
        this.monitors = monitors;
    }

    public void setLbMonitors(Map<String, LbClusterMonitor> monitors) {
        this.lbMonitors = monitors;
    }

    public void addLbMonitor(LbClusterMonitor monitor) {
        lbMonitors.put(monitor.getClusterId(), monitor);
    }
}
