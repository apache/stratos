package org.apache.stratos.autoscaler.exception;

/**
 *
 */
public class SpawningException extends Exception {

    private static final long serialVersionUID = 4761501174753405374L;


    public SpawningException(String message, Exception exception){
        super(message, exception);
    }


    public SpawningException(Exception exception){
        super(exception);
    }
}
