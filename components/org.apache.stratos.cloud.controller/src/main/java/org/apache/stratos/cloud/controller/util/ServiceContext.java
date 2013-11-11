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
package org.apache.stratos.cloud.controller.util;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.messaging.domain.topology.Partition;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * We keep information regarding a service (i.e. a cartridge instance)
 * in this object.
 */
public class ServiceContext implements Serializable{
    private static Log log = LogFactory.getLog(ServiceContext.class);

    private static final long serialVersionUID = -6740964802890082678L;
    private File file;
	private String clusterId;
    private String tenantRange;
    private String hostName;
    private String payloadFilePath = "/tmp/" + CloudControllerConstants.PAYLOAD_NAME + ".zip";
    private String cartridgeType;
    private Cartridge cartridge;
    private StringBuilder payload;
    private String autoScalerPolicyName;
    private List<Partition> partitionList = new ArrayList<Partition>();

    /**
     * Key - Value pair.
     */
    private Map<String, String> properties = new HashMap<String, String>();
    /**
     * Key - IaaS Type
     * Value - {@link IaasContext} object
     */
    private Map<String, IaasContext> iaasCtxts = new HashMap<String, IaasContext>();

    public Map<String, IaasContext> getIaasCtxts() {
    	return iaasCtxts;
    }

	public String getClusterId() {
        return clusterId;
    }

    public boolean setClusterId(String domainName) {
        if (!"".equals(domainName)) {
            this.clusterId = domainName;
            return true;
        }

        return false;
    }

    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    public String getProperty(String key) {

        if(properties.containsKey(key)){
            return properties.get(key);
        }

        return "";
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }


    public Cartridge getCartridge() {
        return cartridge;
    }

    public void setCartridge(Cartridge cartridge) {
        this.cartridge = cartridge;
    }

	public String getTenantRange() {
	    return tenantRange;
    }

	public void setTenantRange(String tenantRange) {
	    this.tenantRange = tenantRange;
    }

	public IaasContext addIaasContext(String iaasType){
		IaasContext ctxt = new IaasContext(iaasType);
		iaasCtxts.put(iaasType, ctxt);
		return ctxt;
	}

	public IaasContext getIaasContext(String type){
		return iaasCtxts.get(type);
	}

	public void setIaasContextMap(Map<String, IaasContext> map){
		iaasCtxts = map;
	}

	public String getPayloadFile() {
	    return payloadFilePath;
    }

	public void setPayloadFile(String payloadFile) {
	    this.payloadFilePath = payloadFile;
    }

	public String getHostName() {
		if(cartridge != null && (hostName == null || hostName.isEmpty())){
			return cartridge.getHostName();
		}
	    return hostName;
    }

	public void setHostName(String hostName) {
	    this.hostName = hostName;
    }

	public String getCartridgeType() {
	    return cartridgeType;
    }

	public void setCartridgeType(String cartridgeType) {
	    this.cartridgeType = cartridgeType;
    }

	public StringBuilder getPayload() {
	    return payload;
    }

	public void setPayload(StringBuilder payload) {
	    this.payload = payload;
    }

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

    public String toXml() {
		String str = "<service domain=\"" + clusterId +
		                                    "\" tenantRange=\"" + tenantRange + "\" policyName=\"" + autoScalerPolicyName + "\">\n" +
		                                    "\t<cartridge type=\"" + cartridgeType +
		                                    "\"/>\n"  + "\t<host>" + hostName +
		                                    "</host>\n" + "\t<payload>" + payload +
		                                    "</payload>\n" +
		                                    propertiesToXml() +
		                                    "</service>";
		return str;
	}

    public String propertiesToXml() {
		StringBuilder builder = new StringBuilder("");
		for (Iterator<Map.Entry<String, String>> iterator = getProperties().entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<String, String> prop = iterator.next();

			String key = prop.getKey();
			String value = prop.getValue();
			if (key != null) {
				builder.append("\t<property name=\""+key +"\" value=\"" + (value == null ? "" : value) +"\"/>\n");
			}

		}

		return builder.toString();
	}

    public byte[] generatePayload() {
        String payloadStringTempFile = "launch-params";

        FileWriter fstream;
        try {
            fstream = new FileWriter(payloadStringTempFile);

        } catch (IOException e) {
            log.error(e.getMessage());
           throw new CloudControllerException(e.getMessage(), e);
        }
        BufferedWriter out = new BufferedWriter(fstream);
       try {
            out.write(payload.toString());
           out.close();

       } catch (IOException e) {
            log.error(e.getMessage());
            throw new CloudControllerException(e.getMessage(), e);
        }


        FileInputStream fis;
        try {
            fis = new FileInputStream(payloadStringTempFile);
        } catch (FileNotFoundException e) {
             String msg = "Failed while persisting the payload of clusterId : "
						+ clusterId;
				log.error(msg, e);
				throw new CloudControllerException(msg, e);
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(this.payloadFilePath);

        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
            throw new CloudControllerException(e.getMessage(), e);
        }

        ZipOutputStream zos = new ZipOutputStream(fos);
        addToZipFile(System.getProperty("user.dir"), payloadStringTempFile, zos);

        File folder = new File(CarbonUtils.getCarbonHome() + File.separator
               + "repository" + File.separator + "resources" + File.separator
                + "user-data");

       if(folder != null && folder.exists()) {
            for (File fileEntry : folder.listFiles()) {
                if (fileEntry != null && !fileEntry.isDirectory()) {
                    addToZipFile(folder.getPath(), fileEntry.getName(), zos);
                }
            }
       }

        try {
            zos.close();
           fos.close();

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new CloudControllerException(e.getMessage(), e);
        }
        byte [] payloadData = null;
        File file = null;
        FileInputStream fileInputStream = null;
        try {
            file = new File(payloadFilePath);
            payloadData = new byte[(int)file.length()];
            fileInputStream = new FileInputStream(file);
            try {
                fileInputStream.read(payloadData);
            } catch (IOException e) {
                 log.error(e.getMessage());
            throw new CloudControllerException(e.getMessage(), e);
            }
        } catch (FileNotFoundException e) {
             log.error(e.getMessage());
            throw new CloudControllerException(e.getMessage(), e);
        }
        try {
            fileInputStream.close();
            file.delete();
        } catch (IOException e) {
             log.error(e.getMessage());
            throw new CloudControllerException(e.getMessage(), e);
        }

        return payloadData;
    }

      /**
     * Adds content to a zip file
     *
     * @param dir Name of directory
     * @param fileName Name of file to add
     * @param zos ZipOutputStream subscription to write
     * @throws CloudControllerException in an error
     */
    private void addToZipFile(String dir, String fileName, ZipOutputStream zos) throws CloudControllerException {

        log.info("Writing '" + fileName + "' to zip file");

        File file = new File(dir + File.separator + fileName);
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);

        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
            throw new CloudControllerException(e.getMessage(), e);
        }

        ZipEntry zipEntry = new ZipEntry(fileName);
        try {
            zos.putNextEntry(zipEntry);

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new CloudControllerException(e.getMessage(), e);
        }

        byte[] bytes = new byte[1024];
        int length;

            try {
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new CloudControllerException(e.getMessage(), e);
            }

        try {
            zos.closeEntry();
            fis.close();

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new CloudControllerException(e.getMessage(), e);
        }
    }

  	public boolean equals(Object obj) {
		if (obj instanceof ServiceContext) {
			return this.clusterId.equals(((ServiceContext) obj).getClusterId());
		}
		return false;
	}

    public int hashCode() {
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
            append(clusterId).
                toHashCode();
    }

    public String getAutoScalerPolicyName() {
        return autoScalerPolicyName;
    }

    public void setAutoScalerPolicyName(String autoScalerPolicyName) {
        this.autoScalerPolicyName = autoScalerPolicyName;
    }

    public List<Partition> getPartitionList() {
        return partitionList;
    }

    public void setPartitionList(List<Partition> partitionList) {
        this.partitionList = partitionList;
    }

    public void addPartition(Partition partition) {
        this.partitionList.add(partition);
    }

    public void removePartition(Partition partition) {
        this.partitionList.remove(partition);
    }
}
