package org.apache.stratos.autoscaler.exception;

/**
 *
 */
public class PartitionValidationException extends Exception {

	private static final long serialVersionUID = -3904452358279522141L;
	private String message;

	public PartitionValidationException(String message, Exception exception){
        super(message, exception);
        this.setMessage(message);
    }

	public PartitionValidationException(String msg) {
		super(msg);
		this.message = msg;
	}
    public PartitionValidationException(Exception exception){
        super(exception);
    }


	public String getMessage() {
		return message;
	}


	public void setMessage(String message) {
		this.message = message;
	}
}
