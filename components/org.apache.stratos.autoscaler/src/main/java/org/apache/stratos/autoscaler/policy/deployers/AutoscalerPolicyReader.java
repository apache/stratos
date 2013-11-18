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

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.deployment.DeploymentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.policy.InvalidPolicyException;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.policy.model.LoadAverage;
import org.apache.stratos.autoscaler.policy.model.LoadThresholds;
import org.apache.stratos.autoscaler.policy.model.MemoryConsumption;
import org.apache.stratos.autoscaler.policy.model.RequestsInFlight;

/**
 * 
 * The Reader class for Autoscale-policy definitions.
 */
public class AutoscalerPolicyReader extends AbstractPolicyReader<AutoscalePolicy>  {
	
	private static final Log log = LogFactory.getLog(AutoscalerPolicyReader.class);
	
	public AutoscalerPolicyReader(File file) {
		super(file);
	}
	
	public AutoscalePolicy read() throws InvalidPolicyException{
		AutoscalePolicy policy = new AutoscalePolicy();
		try {
			OMElement docEle = getDocument();
			if("autoscalePolicy".equalsIgnoreCase(docEle.getLocalName())){
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
				OMElement loadThresholdsEle = docEle.getFirstChildWithName(new QName("loadThresholds"));
				LoadThresholds loadThresholds = new LoadThresholds();
				
				//RequestsInFlight
				OMElement reqInFlightEle = loadThresholdsEle.getFirstChildWithName(new QName("requestsInFlight"));
				RequestsInFlight reqInFlight = new RequestsInFlight();
				reqInFlight.setAverage(Float.valueOf(readValueAttr(reqInFlightEle,"average")));
				reqInFlight.setGradient(Float.valueOf(readValueAttr(reqInFlightEle,"gradient")));
				reqInFlight.setSecondDerivative(Float.valueOf(readValueAttr(reqInFlightEle,"secondDerivative")));
                reqInFlight.setScaleDownMarginOfGradient(Float.valueOf(readValueAttr(reqInFlightEle,"scaleDownMarginOfGradient")));
                reqInFlight.setScaleDownMarginOfSecondDerivative(Float.valueOf(readValueAttr(reqInFlightEle,"scaleDownMarginOfSecondDerivative")));
				loadThresholds.setRequestsInFlight(reqInFlight);
				
				//MemoryConsumption
				OMElement memConsumptionEle = loadThresholdsEle.getFirstChildWithName(new QName("memoryConsumption"));
				MemoryConsumption memConsumption = new MemoryConsumption();
				memConsumption.setAverage(Float.valueOf(readValueAttr(memConsumptionEle,"average")));
				memConsumption.setGradient(Float.valueOf(readValueAttr(memConsumptionEle,"gradient")));
				memConsumption.setSecondDerivative(Float.valueOf(readValueAttr(memConsumptionEle,"secondDerivative")));
                memConsumption.setScaleDownMarginOfGradient(Float.valueOf(readValueAttr(memConsumptionEle,"scaleDownMarginOfGradient")));
                memConsumption.setScaleDownMarginOfSecondDerivative(Float.valueOf(readValueAttr(memConsumptionEle,"scaleDownMarginOfSecondDerivative")));
				loadThresholds.setMemoryConsumption(memConsumption);
				
				//LoadAverage
				OMElement loadAvrEle = loadThresholdsEle.getFirstChildWithName(new QName("loadAverage"));
				LoadAverage loadAvr = new LoadAverage();
				loadAvr.setAverage(Float.valueOf(readValueAttr(loadAvrEle,"average")));
				loadAvr.setGradient(Float.valueOf(readValueAttr(loadAvrEle,"gradient")));
				loadAvr.setSecondDerivative(Float.valueOf(readValueAttr(loadAvrEle,"secondDerivative")));
                loadAvr.setScaleDownMarginOfGradient(Float.valueOf(readValueAttr(loadAvrEle,"scaleDownMarginOfGradient")));
                loadAvr.setScaleDownMarginOfSecondDerivative(Float.valueOf(readValueAttr(loadAvrEle,"scaleDownMarginOfSecondDerivative")));
				loadThresholds.setLoadAverage(loadAvr);
				
				policy.setLoadThresholds(loadThresholds);
				
			} else{
				throw new DeploymentException("File is not a valid autoscale policy");
			}

		} catch (Exception e){
			log.error("Malformed autoscale policy file", e);
			throw new InvalidPolicyException("Malformed autoscale policy file",e);
		} finally{
			closeStream();
		}
		return policy;
	}

}
