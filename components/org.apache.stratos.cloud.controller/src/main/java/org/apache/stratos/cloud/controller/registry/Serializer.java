/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.messaging.domain.topology.Topology;

import java.io.*;

public class Serializer {
    
    private static final Log log = LogFactory.getLog(Serializer.class);

    public static void serializeToFile(Object serializableObj, String filePath) throws IOException {

        File outFile = new File(filePath);
        ObjectOutput objOut = null;
        FileOutputStream fileOutputStream = null;
        
        try {

            if(outFile.createNewFile()){
                log.debug("Serialization file is created at "+filePath);
            } else{
                log.debug("Serialization file is already existing at "+filePath);
            }
            fileOutputStream = new FileOutputStream(outFile);
            objOut = new ObjectOutputStream(fileOutputStream);
            objOut.writeObject(serializableObj);

        } catch (IOException e) {
            log.error("Failed to serialize the object "+serializableObj.toString()
                      + " to file "+filePath , e);
            throw e;
            
        } finally{
            if(objOut != null) {
                objOut.close();
            }
            if(fileOutputStream != null) {
                fileOutputStream.close();
            }
        }

    }
    
    /**
     * Serialize a {@link org.apache.stratos.cloud.controller.context.CloudControllerContext} to a byte array.
     * @param serializableObject
     * @return byte[] 
     * @throws IOException
     */
    public static byte[]  serializeToByteArray(Serializable serializableObject) throws IOException {

    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	ObjectOutput out = null;
    	try {
    	  out = new ObjectOutputStream(bos);   
    	  out.writeObject(serializableObject);
    	  
    	  return bos.toByteArray();
    	  
        } finally {
            if (out != null) {
                out.close();
            }
            bos.close();
        }

    }

     /**
     * Serialize a {@link org.apache.stratos.cloud.controller.context.CloudControllerContext} to a byte array.
     * @param topology
     * @return byte[]
     * @throws IOException
     */
    public static byte[] serializeToByteArray(Topology topology) throws IOException {

    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	ObjectOutput out = null;
    	try {
    	  out = new ObjectOutputStream(bos);
    	  out.writeObject(topology);

    	  return bos.toByteArray();

        } finally {
            if (out != null) {
                out.close();
            }
            bos.close();
        }

    }

}
