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

package org.apache.stratos.autoscaler.partition.deployers;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.cloud.controller.pojo.Property;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.jaxen.JaxenException;

/**
 * 
 * The Reader class for Deployment-policy definitions.
 */
public class PartitionReader{
	
	private static final Log log = LogFactory.getLog(PartitionReader.class);
	private static OMElement documentElement;
	private File partitionFIle;
	
	public PartitionReader(File partitionFile){
		this.partitionFIle = partitionFile;
	}
	
	public List<Partition> getPartitionList(){
			this.parse(this.partitionFIle);
			String partitionXpath = AutoScalerConstants.PARTITIONS_ELEMENT + "/"+AutoScalerConstants.PARTITION_ELEMENT;
			List<OMNode> partitionXMLNodes = getMatchingNodes(partitionXpath);
			Iterator<OMNode> itr = partitionXMLNodes.iterator();
			List<Partition> partitonList = new ArrayList<Partition>();
			while(itr.hasNext()){
				 OMNode node  = itr.next();
				 //System.out.println("node " + node);
				 partitonList.add(this.getPartition(node));
			 }
		return partitonList;
	}
	
	private  Partition getPartition(final OMNode item) {
        Partition partition = null;
        String id = null;

        if (item.getType() == OMNode.ELEMENT_NODE) {

            OMElement iaasElt = (OMElement) item;
            Iterator<?> it =
                    iaasElt.getChildrenWithName(new QName(AutoScalerConstants.ID_ELEMENT));

            if (it.hasNext()) {
                OMElement providerElt = (OMElement) it.next();
                id = providerElt.getText();
            }

            if (it.hasNext()) {
                log.warn( " contains more than one " + AutoScalerConstants.ID_ELEMENT +
                        " elements!" + " Elements other than the first will be neglected.");
            }

            if (id == null) {
                String msg ="Essential " + AutoScalerConstants.ID_ELEMENT + "element " +
                                "has not specified in ";
                // handleException(msg);
            }         
            	
            partition = new Partition();               
            partition.setId(id);
            partition.setProperties(getProperties(iaasElt)); 
                                    
        }
        return partition;
    }
	
	public void parse(File xmlSource){

        if (xmlSource.exists()) {
            try {
                documentElement = new StAXOMBuilder(xmlSource.getPath()).getDocumentElement();

            } catch (Exception ex) {
                String msg = "Error occurred when parsing the " + xmlSource.getPath() + ".";
                //handleException(msg, ex);
            }
        } else {
            String msg = "Configuration file cannot be found : " + xmlSource.getPath();
            //handleException(msg);
        }
    }
	/**
     * @param xpath XPATH expression to be read.
     * @return List matching OMNode list
     */
    @SuppressWarnings("unchecked")
    public List<OMNode> getMatchingNodes(final String xpath) {

        AXIOMXPath axiomXpath;
        List<OMNode> nodeList = null;
        try {
            axiomXpath = new AXIOMXPath(xpath);
            nodeList = axiomXpath.selectNodes(documentElement);
        } catch (JaxenException e) {
            String msg = "Error occurred while reading the Xpath (" + xpath + ")";
            //log.error(msg, e);
        }

        return nodeList;
    }
    
    private Properties getProperties(final OMElement elt) {
    	
    	        Iterator<?> it = elt.getChildrenWithName(new QName(AutoScalerConstants.PROPERTY_ELEMENT));
    	        ArrayList<Property> propertyList = new ArrayList<Property>();
    	        
    	        while (it.hasNext()) {
    	            OMElement prop = (OMElement) it.next();
    	
    	            if (prop.getAttribute(new QName(AutoScalerConstants.PROPERTY_NAME_ATTR)) == null ||
    	                    prop.getAttribute(new QName(AutoScalerConstants.PROPERTY_VALUE_ATTR)) == null) {
    	
    	                String msg =
    	                        "Property element's, name and value attributes should be specified " +
    	                                "in ";
    	
    	                //handleException(msg);
    	            }
    	            
    	            String name = prop.getAttribute(new QName(AutoScalerConstants.PROPERTY_NAME_ATTR)).getAttributeValue();
    	            String value = prop.getAttribute(new QName(AutoScalerConstants.PROPERTY_VALUE_ATTR)).getAttributeValue();
    	            
    	            Property property = new Property();
    	            property.setName(name);
    	            property.setValue(value);    	            
    	            propertyList.add(property);
    	        }

    	        Property[] propertyArray = propertyList.toArray(new Property[propertyList.size()]);    	        
    	        Properties preoperties = new Properties();
    	        preoperties.setProperties(propertyArray);
    	        return preoperties;
    	    }

}
