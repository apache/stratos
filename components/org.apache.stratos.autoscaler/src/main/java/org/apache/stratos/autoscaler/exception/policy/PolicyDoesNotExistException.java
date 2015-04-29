package org.apache.stratos.autoscaler.exception.policy;

import org.apache.stratos.autoscaler.exception.AutoScalerException;

/**
 *
 */
public class PolicyDoesNotExistException extends AutoScalerException {

    public PolicyDoesNotExistException(String msg) {
        super(msg);
    }
}
