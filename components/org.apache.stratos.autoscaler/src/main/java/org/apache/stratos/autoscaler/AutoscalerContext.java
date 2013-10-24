package org.apache.stratos.autoscaler;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is there for accumulating cluster details which are not there in Topology
 */
public class AutoscalerContext {
    private static volatile AutoscalerContext instance;
    Map<String, ClusterContext> clusterContextMap;

    private AutoscalerContext() {
        clusterContextMap = new HashMap<String, ClusterContext>();
    }

    public static synchronized AutoscalerContext getInstance() {
        if (instance == null) {
            synchronized (AutoscalerContext.class){
                if (instance == null) {
                    instance = new AutoscalerContext();
                }
            }
        }
        return instance;
    }

    /**
     *
     * @param clusterContext will be added to map
     */
    public void addClusterContext(ClusterContext clusterContext) {

        clusterContextMap.put(clusterContext.getClusterId(), clusterContext);
    }

    /**
     * {@link ClusterContext} which carries clusterId will be removed from map
     * @param clusterId
     */
    public void removeClusterContext(String clusterId){

        clusterContextMap.remove(clusterId);
    }
    
    public boolean clusterExists(String clusterId){
        return clusterContextMap.containsKey(clusterId);
    }

    public ClusterContext getClusterContext(String clusterId) {

        return clusterContextMap.get(clusterId);
    }
}
