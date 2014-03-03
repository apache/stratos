package org.apache.stratos.autoscaler.exception;

/**
 *
 */
public class PolicyValidationException extends Exception {

    private static final long serialVersionUID = -7423800138697480115L;
    private String message;

    public PolicyValidationException(String message, Exception exception){
        super(message, exception);
        this.setMessage(message);
    }


    public PolicyValidationException(Exception exception){
        super(exception);
    }
    
    public PolicyValidationException(String msg){
        super(msg);
        this.setMessage(msg);
    }


	public String getMessage() {
		return message;
	}


	public void setMessage(String message) {
		this.message = message;
	}
}
