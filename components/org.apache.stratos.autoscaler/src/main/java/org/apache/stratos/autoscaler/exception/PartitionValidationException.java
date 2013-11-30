package org.apache.stratos.autoscaler.exception;

/**
 *
 */
public class PartitionValidationException extends Exception {

	private static final long serialVersionUID = -3904452358279522141L;


	public PartitionValidationException(String message, Exception exception){
        super(message, exception);
    }


    public PartitionValidationException(Exception exception){
        super(exception);
    }
}
