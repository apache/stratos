package org.apache.stratos.autoscaler.exception;

/**
 *
 */
public class InvalidPartitionException extends Exception {

    private static final long serialVersionUID = -7521673271244696906L;

    public InvalidPartitionException(String message, Exception exception){
        super(message, exception);
    }


    public InvalidPartitionException(Exception exception){
        super(exception);
    }
    
    public InvalidPartitionException(String msg){
        super(msg);
    }
}
