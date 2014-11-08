package org.apache.stratos.autoscaler.algorithm;

public class InstanceCountRoundingAlgorithm {

    public int getFractionBasedCount(float requiredInstances, float fraction){

        return (requiredInstances - Math.floor(requiredInstances) > fraction) ? (int)Math.ceil(requiredInstances)
                : (int)Math.floor(requiredInstances);
    }


}
