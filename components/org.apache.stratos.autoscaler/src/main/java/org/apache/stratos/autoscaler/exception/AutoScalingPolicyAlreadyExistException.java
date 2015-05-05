package org.apache.stratos.autoscaler.exception;

public class AutoScalingPolicyAlreadyExistException extends Exception {
    public AutoScalingPolicyAlreadyExistException(String msg) {
        super(msg);
    }
}
