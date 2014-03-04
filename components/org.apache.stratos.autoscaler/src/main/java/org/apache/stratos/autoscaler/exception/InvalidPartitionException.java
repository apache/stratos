package org.apache.stratos.autoscaler.exception;

/**
 *
 */
public class InvalidPartitionException extends Exception {

    private static final long serialVersionUID = -7521673271244696906L;
    private String message;

    public InvalidPartitionException(String message, Exception exception){
        super(message, exception);
        this.message = message;
    }


    public InvalidPartitionException(Exception exception){
        super(exception);
    }
    
    public InvalidPartitionException(String msg){
        super(msg);
        this.message = msg;
    }
    
    @Override
    public String getMessage() {
        return this.message;
    }
}
