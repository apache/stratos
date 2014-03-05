package org.apache.stratos.autoscaler.exception;

public class TerminationException extends Throwable {

	private static final long serialVersionUID = -6038793010380236971L;
	private String message;
	
	public TerminationException(String s, Exception e) {
        super(s, e);
        this.setMessage(s);
    }
    
    public TerminationException(Exception e) {
        super(e);
    }

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
