package org.wso2.carbon.adc.topology.mgt.util;

public class TopologyConstants {
    
    public static final String TOPIC_NAME = "cloud-controller-topology";
    public static final String MB_SERVER_IP = "mb.server.ip";
    public static final String DEFAULT_MB_SERVER_IP = "localhost:5673";
    
    public static final String TOPOLOGY_SYNC_CRON = "1 * * * * ? *";
	public static final String TOPOLOGY_SYNC_TASK_NAME = "TopologySubscriberTaskOfADC";
	public static final String TOPOLOGY_SYNC_TASK_TYPE = "TOPOLOGY_SUBSCRIBER_TASK";

}
