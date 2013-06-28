/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.stratos.cloud.controller.persist;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Deserializer {
    
    private static final Log log = LogFactory.getLog(Deserializer.class);

    /**
     * We deserialize only if the path to the serialized object file is exists.
     * @param filePath path to the serialized object file
     * @return the object obtained after deserialization or null if file isn't valid.
     * @throws Exception
     */
    public static Object deserialize(String filePath) throws Exception {

        ObjectInputStream objIn = null; 
        Object obj = null;
        
        if(!new File(filePath).isFile()){
            return obj;
        }
        
        try {

            objIn = new ObjectInputStream(new FileInputStream(filePath));
            obj = objIn.readObject();

        } catch (IOException e) {
            log.error("Failed to deserialize the file at "+filePath , e);
            throw e;
            
        } catch (ClassNotFoundException e) {
            log.error("Failed to deserialize the file at "+filePath , e);
            throw e;
            
        } finally{
            objIn.close();
        }
        
        return obj;

    }
    
    /**
     * Deserialize a byte array and retrieve the object.
     * @param bytes bytes to be deserialized
     * @return the deserialized {@link Object}
     * @throws Exception if the deserialization is failed.
     */
    public static Object deserializeFromByteArray(byte[] bytes) throws Exception {

    	ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    	ObjectInput in = null;
    	try {
    	  in = new ObjectInputStream(bis);
    	  Object o = in.readObject(); 
    	  
    	  return o;
    	  
    	} finally {
    	  bis.close();
    	  in.close();
    	}
    }

}
