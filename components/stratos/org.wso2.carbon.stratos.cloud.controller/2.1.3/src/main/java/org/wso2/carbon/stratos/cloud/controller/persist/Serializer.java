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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.cloud.controller.runtime.FasterLookUpDataHolder;

public class Serializer {
    
    private static final Log log = LogFactory.getLog(Serializer.class);

    public static void serializeToFile(Object serializableObj, String filePath) throws IOException {

        File outFile = new File(filePath);
        ObjectOutput ObjOut = null;
        
        try {

            if(outFile.createNewFile()){
                log.debug("Serialization file is created at "+filePath);
            } else{
                log.debug("Serialization file is already existing at "+filePath);
            }
            
            ObjOut = new ObjectOutputStream(new FileOutputStream(outFile));
            ObjOut.writeObject(serializableObj);

        } catch (IOException e) {
            log.error("Failed to serialize the object "+serializableObj.toString()
                      + " to file "+filePath , e);
            throw e;
            
        } finally{
            ObjOut.close();
        }

    }
    
    /**
     * Serialize a {@link FasterLookUpDataHolder} to a byte array.
     * @param serializableObj
     * @return byte[] 
     * @throws IOException
     */
    public static byte[] serializeToByteArray(FasterLookUpDataHolder serializableObj) throws IOException {

    	ByteArrayOutputStream bos = new ByteArrayOutputStream();
    	ObjectOutput out = null;
    	try {
    	  out = new ObjectOutputStream(bos);   
    	  out.writeObject(serializableObj);
    	  
    	  return bos.toByteArray();
    	  
    	} finally {
    	  out.close();
    	  bos.close();
    	}

    }

}
