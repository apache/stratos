package org.apache.stratos.autoscaler.exception;

import java.rmi.RemoteException;

/**
 *
 */
public class SpawningException extends Exception {

    public SpawningException(String exception, RemoteException message){
        super(exception, message);
    }


}
