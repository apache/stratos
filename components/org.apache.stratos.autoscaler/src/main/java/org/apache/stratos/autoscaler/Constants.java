package org.apache.stratos.autoscaler;

/**
 *
 */
public class Constants {

    public static String ROUND_ROBIN_ALGORITHM_ID = "round-robin";
    public static String ONE_AFTER_ANOTHER_ALGORITHM_ID = "one-after-another";

    public static String GRADIENT_OF_REQUESTS_IN_FLIGHT = "gradient_of_requests_in_flight";
    public static String AVERAGE_REQUESTS_IN_FLIGHT = "average_requests_in_flight";
    public static String SECOND_DERIVATIVE_OF_REQUESTS_IN_FLIGHT = "second_derivative_of_requests_in_flight";

    //scheduler
    public static final int SCHEDULE_DEFAULT_INITIAL_DELAY = 30;
    public static final int SCHEDULE_DEFAULT_PERIOD = 15;

    public static final String AUTOSCALER_CONFIG_FILE_NAME = "autoscaler.xml";

    public static final String CLOUD_CONTROLLER_SERVICE_SFX = "services/CloudControllerService";
    public static final int CLOUD_CONTROLLER_DEFAULT_PORT = 9444;

//           public void a(){
//             Cluster cluster = null;
////               log.info("cluster " + clusterId);
//       	    String clusterId = cluster.getClusterId();
//
//
//       		ClusterContext clusterContext = null;
////                       = $context.getClusterContext(clusterId);
//            AutoscalerContext context = null;
//       		if(null==clusterContext){
//
//            	clusterContext = new ClusterContext(cluster.getClusterId(), cluster.getServiceName()) ;
//               AutoscalePolicy policy = PolicyManager.getInstance().getPolicy(cluster.getAutoscalePolicyName());
//
//                   for(Partition partition: policy.getHAPolicy().getPartitions()){
//                       clusterContext.addPartitionCount(partition.getId(), 0);
//                   }
//                   context.addClusterContext(clusterContext);
//
//               }
//
//
//               float lbStatAverage = clusterContext.getAverageRequestsInFlight();
//               float lbStatGradient = clusterContext.getRequestsInFlightGradient();
//               float lbStatSecondDerivative = clusterContext.getRequestsInFlightSecondDerivative();
//
//               LoadThresholds loadThresholds = manager.getPolicy(cluster.autoscalePolicyName).getLoadThresholds();
//               float averageLimit = loadThresholds.getRequestsInFlight().getAverage();
//               float gradientLimit = loadThresholds.getRequestsInFlight().getGradient();
//               float secondDerivative  = loadThresholds.getRequestsInFlight().getSecondDerivative()
//               String partitionAlgorithm = manager.getPolicy(cluster.autoscalePolicyName).getHAPolicy().getPartitionAlgo();
//               log.info("partitionAlgorithm " + partitionAlgorithm);
//
//               AutoscaleAlgorithm autoscaleAlgorithm = null;
//               if(Constants.ROUND_ROBIN_ALGORITHM_ID.equals(partitionAlgorithm)){
//
//                   autoscaleAlgorithm = new RoundRobin();
//               } else if(Constants.ONE_AFTER_ANOTHER_ALGORITHM_ID.equals(partitionAlgorithm)){
//
//                   autoscaleAlgorithm = new OneAfterAnother();
//               }
//
//
//                if(lbStatAverage > averageLimit && lbStatGradient > gradientLimit){
////                    log.info("in scale up " );
//                    int i = 0;
//                    Partition partition = autoscaleAlgorithm.getScaleUpPartition(clusterId);
//                    if(partition != null){
////                        log.info("gonna scale up " );
//                        if(lbStatSecondDerivative > secondDerivative){
//                            int numberOfInstancesToBeSpawned = 2; // take from a config
//                                              log.info("gonna scale up by 2 " );
//                            evaluator.delegateSpawn(partition,clusterId, numberOfInstancesToBeSpawned);
//                            //spawnInstances Two
//
//                        } else {
//                            int numberOfInstancesToBeSpawned = 1;
//                            evaluator.delegateSpawn(partition,clusterId, numberOfInstancesToBeSpawned);
//                            //spawnInstances one
//                        }
//                    }
//                } else if(lbStatAverage < averageLimit && lbStatGradient < gradientLimit){
////                          log.info("in scale down " );
//                    //terminate one
//                    Partition partition = autoscaleAlgorithm.getScaleDownPartition(clusterId);
//                    if(partition != null){
////                    log.info("gonna scale down " );
//                        evaluator.delegateTerminate(partition,clusterId);
//                    }
//                }
//           }
}
