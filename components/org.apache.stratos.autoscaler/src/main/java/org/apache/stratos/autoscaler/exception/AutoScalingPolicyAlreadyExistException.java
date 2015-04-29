package org.apache.stratos.autoscaler.exception;

public class AutoScalingPolicyAlreadyExistException extends AutoScalerException {
    public AutoScalingPolicyAlreadyExistException(String msg) {
        super(msg);
    }
}
