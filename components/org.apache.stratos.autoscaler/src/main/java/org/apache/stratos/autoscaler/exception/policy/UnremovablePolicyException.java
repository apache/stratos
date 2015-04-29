package org.apache.stratos.autoscaler.exception.policy;

import org.apache.stratos.autoscaler.exception.AutoScalerException;

public class UnremovablePolicyException extends AutoScalerException {
    public UnremovablePolicyException(String msg) {
        super(msg);
    }
}
