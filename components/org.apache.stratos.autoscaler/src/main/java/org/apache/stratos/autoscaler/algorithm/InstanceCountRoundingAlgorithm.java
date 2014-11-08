package org.apache.stratos.autoscaler.algorithm;

public class InstanceCountRoundingAlgorithm {

    public double getFloorCount(float requiredInstances){

        return (int)Math.floor(requiredInstances);
    }

    public int getCeilCount(float requiredInstances){

        return (int)Math.ceil(requiredInstances);
    }

    public int getFractionBasedCount(float requiredInstances, float fraction){

        return (requiredInstances - Math.floor(requiredInstances) > fraction) ? (int)Math.ceil(requiredInstances)
                : (int)Math.floor(requiredInstances);
    }


}
