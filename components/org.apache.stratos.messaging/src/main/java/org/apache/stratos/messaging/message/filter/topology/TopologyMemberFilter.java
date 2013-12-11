package org.apache.stratos.messaging.message.filter.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.message.filter.MessageFilter;
import org.apache.stratos.messaging.util.Constants;

import java.util.Collection;

/**
 * A filter to discard topology events which are not in a load balancer cluster.
 */
public class TopologyMemberFilter extends MessageFilter {
    private static final Log log = LogFactory.getLog(TopologyServiceFilter.class);
    private static volatile TopologyMemberFilter instance;

    public TopologyMemberFilter() {
        super(Constants.TOPOLOGY_MEMBER_FILTER);
    }

    public static synchronized TopologyMemberFilter getInstance() {
        if (instance == null) {
            synchronized (TopologyMemberFilter.class){
                if (instance == null) {
                    instance = new TopologyMemberFilter();
                    if(log.isDebugEnabled()) {
                        log.debug("Topology member filter instance created");
                    }
                }
            }
        }
        return instance;
    }

    public boolean lbClusterIdIncluded(String value) {
        return included(Constants.TOPOLOGY_MEMBER_FILTER_LB_CLUSTER_ID, value);
    }

    public boolean lbClusterIdExcluded(String value) {
        return excluded(Constants.TOPOLOGY_MEMBER_FILTER_LB_CLUSTER_ID, value);
    }

    public Collection<String> getIncludedLbClusterIds() {
        return getIncludedPropertyValues(Constants.TOPOLOGY_MEMBER_FILTER_LB_CLUSTER_ID);
    }
}
