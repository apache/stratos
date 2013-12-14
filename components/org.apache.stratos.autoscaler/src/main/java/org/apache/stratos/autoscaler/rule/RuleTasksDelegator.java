package org.apache.stratos.autoscaler.rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.algorithm.AutoscaleAlgorithm;
import org.apache.stratos.autoscaler.algorithm.OneAfterAnother;
import org.apache.stratos.autoscaler.algorithm.RoundRobin;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.pojo.MemberContext;

import java.util.Properties;

/**
 * This will have utility methods that need to be executed from rule file...
 */
public class RuleTasksDelegator {

    public static final double SCALE_UP_FACTOR = 0.8;   //get from config
    public static final double SCALE_DOWN_FACTOR = 0.2;

    private static final Log log = LogFactory.getLog(RuleTasksDelegator.class);

    public double getPredictedValueForNextMinute(float average, float gradient, float secondDerivative, int timeInterval){
        double predictedValue;
//        s = u * t + 0.5 * a * t * t
        predictedValue = average + gradient * timeInterval + 0.5 * secondDerivative * timeInterval * timeInterval;
        return predictedValue;
    }

    public AutoscaleAlgorithm getAutoscaleAlgorithm(String partitionAlgorithm){
        AutoscaleAlgorithm autoscaleAlgorithm = null;
        if(Constants.ROUND_ROBIN_ALGORITHM_ID.equals(partitionAlgorithm)){

            autoscaleAlgorithm = new RoundRobin();
        } else if(Constants.ONE_AFTER_ANOTHER_ALGORITHM_ID.equals(partitionAlgorithm)){

            autoscaleAlgorithm = new OneAfterAnother();
        }
        return autoscaleAlgorithm;
    }

    public void delegateSpawn(PartitionContext partitionContext, String clusterId, String lbRefType) {
        try {

            String nwPartitionId = partitionContext.getNetworkPartitionId();
            NetworkPartitionContext ctxt =
                                          PartitionManager.getInstance()
                                                          .getNetworkPartition(nwPartitionId);

            
            String lbClusterId = getLbClusterId(lbRefType, partitionContext, ctxt);

            MemberContext memberContext =
                                         CloudControllerClient.getInstance()
                                                              .spawnAnInstance(partitionContext.getPartition(),
                                                                      clusterId,
                                                                      lbClusterId, partitionContext.getNetworkPartitionId());
            if (memberContext != null) {
               partitionContext.addPendingMember(memberContext);
            }

        } catch (Throwable e) {
            String message = "Cannot spawn an instance";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
   	}



    public static String getLbClusterId(String lbRefType, PartitionContext partitionCtxt, 
        NetworkPartitionContext nwPartitionCtxt) {
       
       String lbClusterId = null;

        if (lbRefType != null) {
            if (lbRefType.equals(org.apache.stratos.messaging.util.Constants.DEFAULT_LOAD_BALANCER)) {
                lbClusterId = nwPartitionCtxt.getDefaultLbClusterId();
            } else if (lbRefType.equals(org.apache.stratos.messaging.util.Constants.SERVICE_AWARE_LOAD_BALANCER)) {
                String serviceName = partitionCtxt.getServiceName();
                lbClusterId = nwPartitionCtxt.getLBClusterIdOfService(serviceName);
            } else {
                log.warn("Invalid LB reference type defined: [value] "+lbRefType);
            }
        }
       return lbClusterId;
    }

    public void delegateTerminate(Partition partition, String clusterId) {
        log.info("terminate from partition " + partition.getId() + " cluster " + clusterId );
    }

    public void delegateTerminate(String memberId) {
        try {

            CloudControllerClient.getInstance().terminate(memberId);
        } catch (Throwable e) {
            log.error("Cannot terminate instance", e);
        }
    }

   	public void delegateTerminateAll(String clusterId) {
           try {

               CloudControllerClient.getInstance().terminateAllInstances(clusterId);
           } catch (Throwable e) {
               log.error("Cannot terminate instance", e);
           }
       }



}
