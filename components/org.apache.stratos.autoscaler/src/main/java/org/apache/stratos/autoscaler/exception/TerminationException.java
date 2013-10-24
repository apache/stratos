package org.apache.stratos.autoscaler.exception;

/**
 *
 */
public class TerminationException extends Throwable {
    public TerminationException(String s, Exception e) {
        super(s, e);
    }
}
