package org.apache.stratos.autoscaler.exception;

/**
 * Exception class for handling invalid Kubernetes Master
 */
public class InvalidKubernetesMasterException extends Exception {
    private String message;

    public InvalidKubernetesMasterException(String message, Exception exception){
        super(message, exception);
        this.message = message;
    }

    public InvalidKubernetesMasterException(Exception exception){
        super(exception);
    }

    public InvalidKubernetesMasterException(String msg){
        super(msg);
        this.message = msg;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
