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

package org.apache.stratos.autoscaler.policy.deployers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.policy.InvalidPolicyException;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.policy.model.HAPolicy;
import org.apache.stratos.autoscaler.policy.model.LoadAverage;
import org.apache.stratos.autoscaler.policy.model.LoadThresholds;
import org.apache.stratos.autoscaler.policy.model.MemoryConsumption;
import org.apache.stratos.autoscaler.policy.model.Partition;
import org.apache.stratos.autoscaler.policy.model.RequestsInFlight;

/**
 * 
 * The Reader class for Autoscale-policy definitions.
 */
public class PolicyReader  {
	
	private static final Log log = LogFactory.getLog(PolicyReader.class);
	
	private File file;
	
	public PolicyReader(File file) {
		this.setFile(file);
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
	
	public AutoscalePolicy read() throws InvalidPolicyException{
		FileInputStream fStream = null;
		AutoscalePolicy policy = new AutoscalePolicy();
		try {
			fStream = new FileInputStream(file);
			XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(fStream);
			StAXOMBuilder builder = new StAXOMBuilder(parser);
			OMElement docEle = builder.getDocumentElement();
			if("AutoscalePolicy".equalsIgnoreCase(docEle.getLocalName())){
				policy.setId(docEle.getAttributeValue(new QName("id")));
				OMElement displayNameEle = docEle.getFirstChildWithName(new QName("displayName"));
				if(displayNameEle!=null){
					policy.setDisplayName(displayNameEle.getText());
				}
				OMElement descriptionEle = docEle.getFirstChildWithName(new QName("description"));
				if(descriptionEle!=null){
					policy.setDescription(descriptionEle.getText());
				}
				
				//LoadThresholds
				OMElement loadThresholdsEle = docEle.getFirstChildWithName(new QName("LoadThresholds"));
				LoadThresholds loadThresholds = new LoadThresholds();
				
				//RequestsInFlight
				OMElement reqInFlightEle = loadThresholdsEle.getFirstChildWithName(new QName("RequestsInFlight"));
				RequestsInFlight reqInFlight = new RequestsInFlight();
				reqInFlight.setAverage(Float.valueOf(readValueAttr(reqInFlightEle,"Average")));
				reqInFlight.setGradient(Float.valueOf(readValueAttr(reqInFlightEle,"Gradient")));
				reqInFlight.setSecondDerivative(Float.valueOf(readValueAttr(reqInFlightEle,"SecondDerivative")));
                reqInFlight.setScaleDownMarginOfGradient(Float.valueOf(readValueAttr(reqInFlightEle,"ScaleDownMarginOfGradient")));
                reqInFlight.setScaleDownMarginOfSecondDerivative(Float.valueOf(readValueAttr(reqInFlightEle,"ScaleDownMarginOfSecondDerivative")));
				loadThresholds.setRequestsInFlight(reqInFlight);
				
				//MemoryConsumption
				OMElement memConsumptionEle = loadThresholdsEle.getFirstChildWithName(new QName("MemoryConsumption"));
				MemoryConsumption memConsumption = new MemoryConsumption();
				memConsumption.setAverage(Float.valueOf(readValueAttr(memConsumptionEle,"Average")));
				memConsumption.setGradient(Float.valueOf(readValueAttr(memConsumptionEle,"Gradient")));
				memConsumption.setSecondDerivative(Float.valueOf(readValueAttr(memConsumptionEle,"SecondDerivative")));
                memConsumption.setScaleDownMarginOfGradient(Float.valueOf(readValueAttr(memConsumptionEle,"ScaleDownMarginOfGradient")));
                memConsumption.setScaleDownMarginOfSecondDerivative(Float.valueOf(readValueAttr(memConsumptionEle,"ScaleDownMarginOfSecondDerivative")));
				loadThresholds.setMemoryConsumption(memConsumption);
				
				//LoadAverage
				OMElement loadAvrEle = loadThresholdsEle.getFirstChildWithName(new QName("LoadAverage"));
				LoadAverage loadAvr = new LoadAverage();
				loadAvr.setAverage(Float.valueOf(readValueAttr(loadAvrEle,"Average")));
				loadAvr.setGradient(Float.valueOf(readValueAttr(loadAvrEle,"Gradient")));
				loadAvr.setSecondDerivative(Float.valueOf(readValueAttr(loadAvrEle,"SecondDerivative")));
                loadAvr.setScaleDownMarginOfGradient(Float.valueOf(readValueAttr(loadAvrEle,"ScaleDownMarginOfGradient")));
                loadAvr.setScaleDownMarginOfSecondDerivative(Float.valueOf(readValueAttr(loadAvrEle,"ScaleDownMarginOfSecondDerivative")));
				loadThresholds.setLoadAverage(loadAvr);
				
				policy.setLoadThresholds(loadThresholds);
				
				//HAPolicy
				OMElement haPolicyEle = docEle.getFirstChildWithName(new QName("HAPolicy"));
				HAPolicy haPolicy = new HAPolicy();
				haPolicy.setPartitionAlgo(readValue(haPolicyEle, "PartitionAlgo"));
				
				//Partitions
				OMElement partitions = haPolicyEle.getFirstChildWithName(new QName("Partitions"));
				Iterator<?> partitionItr = partitions.getChildrenWithLocalName("Partition");
				while(partitionItr.hasNext()){
					Object next = partitionItr.next();
					if(next instanceof OMElement){
						OMElement partitionEle = (OMElement) next;
						Partition partition = new Partition();
						partition.setIaas(partitionEle.getAttributeValue(new QName("iaas")));
						partition.setZone(partitionEle.getAttributeValue(new QName("zone")));
						partition.setId(partitionEle.getAttributeValue(new QName("id")));
						partition.setPartitionMax(Integer.valueOf(readValue(partitionEle, "PartitionMax")));
						partition.setPartitionMin(Integer.valueOf(readValue(partitionEle, "PartitionMin")));
						haPolicy.getPartitions().add(partition);
					}
				}
				
				policy.setHAPolicy(haPolicy);
				
			} else{
				throw new DeploymentException("File is not a valid autoscale policy");
			}

		} catch (Exception e){
			throw new InvalidPolicyException("Malformed policy file",e);
		} finally{
			try {
				if(fStream!=null){
					fStream.close();
				} 
			} catch (IOException e) {
				log.debug("Unable to close the input stream", e);
			}
		}
		return policy;
	}
	
	private String readValueAttr(OMElement ele, String qName){
		OMElement valueContainer = ele.getFirstChildWithName(new QName(qName));
		String value = valueContainer.getAttributeValue(new QName("value"));
		return value;
	}
	
	private String readValue(OMElement ele, String qName){
		OMElement valueContainer = ele.getFirstChildWithName(new QName(qName));
		String value = valueContainer.getText();
		return value;
	}

}
