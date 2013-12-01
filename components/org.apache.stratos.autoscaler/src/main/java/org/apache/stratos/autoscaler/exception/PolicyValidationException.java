package org.apache.stratos.autoscaler.exception;

/**
 *
 */
public class PolicyValidationException extends Exception {

    private static final long serialVersionUID = -7423800138697480115L;


    public PolicyValidationException(String message, Exception exception){
        super(message, exception);
    }


    public PolicyValidationException(Exception exception){
        super(exception);
    }
    
    public PolicyValidationException(String msg){
        super(msg);
    }
}
