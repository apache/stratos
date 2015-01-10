package org.apache.stratos.manager.exception;

/**
 * Created by imesh on 1/5/15.
 */
public class ApplicationSignUpException extends Exception {

    public ApplicationSignUpException(String message) {
        super(message);
    }

    public ApplicationSignUpException(String message, Throwable e) {
        super(message, e);
    }
}
