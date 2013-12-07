package org.apache.stratos.autoscaler.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

public class Serializer {
	/**
     * Serialize a object to a byte array.
     * @param serializableObj
     * @return byte[] 
     * @throws IOException
     */
    public static byte[] serializeToByteArray(Object serializableObj) throws IOException {

    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	ObjectOutput out = null;
    	try {
    	  out = new ObjectOutputStream(bos);   
    	  out.writeObject(serializableObj);
    	  
    	  return bos.toByteArray();
    	  
        } finally {
            if (out != null) {
                out.close();
            }
            bos.close();
        }

    }
}
