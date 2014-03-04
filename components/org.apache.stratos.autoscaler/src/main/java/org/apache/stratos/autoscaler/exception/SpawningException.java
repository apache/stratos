package org.apache.stratos.autoscaler.exception;

/**
 *
 */
public class SpawningException extends Exception {

    private static final long serialVersionUID = 4761501174753405374L;
    private String message;

    public SpawningException(String message, Exception exception){
        super(message, exception);
        this.setMessage(message);
    }


    public SpawningException(Exception exception){
        super(exception);
    }


	public String getMessage() {
		return message;
	}


	public void setMessage(String message) {
		this.message = message;
	}
}
