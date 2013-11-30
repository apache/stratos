package org.apache.stratos.autoscaler.exception;

public class TerminationException extends Throwable {

	private static final long serialVersionUID = -6038793010380236971L;

	public TerminationException(String s, Exception e) {
        super(s, e);
    }
    
    public TerminationException(Exception e) {
        super(e);
    }
}
