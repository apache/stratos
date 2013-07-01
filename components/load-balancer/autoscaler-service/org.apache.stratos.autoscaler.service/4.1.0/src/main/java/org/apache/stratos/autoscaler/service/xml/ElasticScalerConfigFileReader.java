/*
 *  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.service.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.apache.stratos.autoscaler.service.exception.MalformedConfigurationFileException;
import org.apache.stratos.autoscaler.service.util.AutoscalerConstant;
import org.apache.stratos.autoscaler.service.util.IaasProvider;
import org.apache.stratos.autoscaler.service.util.ServiceTemplate;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

/**
 * Responsible for reading the Elastic scaler configuration file.
 * Following is a sample XML.
 *
 * &lt;elasticScalerConfig&gt;
 *      &lt;iaasProviders&gt;
 *          &lt;iaasProvider name="ec2"&gt;
 *              &lt;provider&gt;aws-ec2&lt;/provider&gt;
 *              &lt;identity&gt;aaa&lt;/identity&gt;
 *              &lt;credential&gt;aaaa&lt;/credential&gt;
 *              &lt;scaleUpOrder&gt;1&lt;/scaleUpOrder&gt;
 *              &lt;scaleDownOrder&gt;2&lt;/scaleDownOrder&gt;
 *              &lt;property name="A" value="a"/&gt;
 *              &lt;property name="B" value="b"/&gt;
 *              &lt;template&gt;temp1&lt;/template&gt;
 *          &lt;/iaasProvider&gt;
 *          
 *          &lt;iaasProvider name="lxc"&gt;
 *              &lt;provider&gt;aws-ec2&lt;/provider&gt;
 *              &lt;identity&gt;aaa&lt;/identity&gt;
 *              &lt;credential&gt;aaaa&lt;/credential&gt;
 *              &lt;scaleUpOrder&gt;2&lt;/scaleUpOrder&gt;
 *              &lt;scaleDownOrder&gt;1&lt;/scaleDownOrder&gt;
 *              &lt;property name="X" value="x"/&gt;
 *              &lt;property name="Y" value="y"/&gt;
 *              &lt;template&gt;temp2&lt;/template&gt;
 *          &lt;/iaasProvider&gt;
 *      &lt;/iaasProviders&gt;
 *      $lt;services&gt;
 *          $lt;default&gt;
 *              $lt;property name="availabilityZone" value="us-east-1c"/&gt;
 *              $lt;property name="securityGroups" value="manager,cep,mb,default"/&gt;
 *              $lt;property name="instanceType" value="m1.large"/&gt;
 *              $lt;property name="keyPair" value="aa"/&gt;
 *          $lt;/default&gt;
 *          $lt;service domain="wso2.as.domain"&gt;
 *              $lt;property name="securityGroups" value="manager,default"/&gt;
 *              $lt;property name="payload" value="resources/as.zip"/&gt;
 *          $lt;/service&gt;
 *      $lt;/services&gt;
 *  &lt;/elasticScalerConfig&gt;
 */
public class ElasticScalerConfigFileReader {
	
	private static final Log log = LogFactory.getLog(ElasticScalerConfigFileReader.class);

	//get the factory
	private DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private Document dom;
	private Element docEle;

	/**
	 * Path to elastic-scaler-config XML file, which specifies the Iaas specific details and 
	 * services related details.
	 */
	private String elasticScalerConfigFile;
	
	
	public ElasticScalerConfigFileReader(){
	    
	    elasticScalerConfigFile = CarbonUtils.getCarbonConfigDirPath() +
	            File.separator + "elastic-scaler-config.xml";
	    
		/**
		 * Parse the configuration file.
		 */
		try {
			//Using factory, get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();

			//parse using builder to get DOM representation of the XML file
			dom = db.parse(elasticScalerConfigFile);
			

		}catch(Exception ex) {
			String msg = "Error occurred while parsing the "+elasticScalerConfigFile+".";
			handleException(msg, ex);
		}
	}
	
	/**
	 * Constructor to be used in the test cases.
	 * @param file path to elastic-scaler-config xml file.
	 */
	public ElasticScalerConfigFileReader(String file) {
        
        /**
         * Parse the configuration file.
         */
        try {
            //Using factory, get an instance of document builder
            DocumentBuilder db = dbf.newDocumentBuilder();
            
            //parse using builder to get DOM representation of the XML file
            dom = db.parse(file);
            

        }catch(Exception ex) {
            String msg = "Error occurred when parsing the "+file+".";
            handleException(msg, ex);
        }
    }
	
	/**
	 * Returns the serialization directory specified in the configuration file.
	 * @return the path to the directory or an empty string if element cannot be found.
	 */
	public String getSerializationDir() {
        
	    docEle = dom.getDocumentElement();
	    NodeList nl = docEle.getElementsByTagName(AutoscalerConstant.SERIALIZATION_DIR_ELEMENT);

        // there should be only one serializationDir element, we neglect all the others
        if (nl != null && nl.getLength() > 0) {
            
            if (nl.item(0).getNodeType() == Node.ELEMENT_NODE) {
                Element prop = (Element) nl.item(0);

                return prop.getTextContent();

            }
        }
        
        return "";
    }
	
	/**
	 * Load all IaasProviders from the configuration file and returns a list.
	 * @return a list of IaasProvider instances.
	 */
	public List<IaasProvider> getIaasProvidersList() {
	    List<IaasProvider> iaasProviders = new ArrayList<IaasProvider>();
	    
	    docEle = dom.getDocumentElement();
	    NodeList nl = docEle.getElementsByTagName(AutoscalerConstant.IAAS_PROVIDER_ELEMENT);
	    
	    if (nl != null && nl.getLength() > 0) {
	        
	        for(int i=0; i< nl.getLength() ; i++){
	            iaasProviders.add(getIaasProvider(nl.item(i)));
	        }
	        
	    }
	    else{
	        String msg = "Essential '"+AutoscalerConstant.IAAS_PROVIDER_ELEMENT+"' element cannot" +
	        		" be found in "+elasticScalerConfigFile;
	        handleException(msg);
	    }
	    
	    return iaasProviders;
	    
    }

    private IaasProvider getIaasProvider(Node item) {

        IaasProvider iaas = new IaasProvider();
        
        if (item.getNodeType() == Node.ELEMENT_NODE) {
            Element iaasElt = (Element) item;
            iaas.setType(iaasElt.getAttribute(AutoscalerConstant.IAAS_PROVIDER_TYPE_ATTR));
            
            if("".equals(iaas.getType())){
                String msg = "'"+AutoscalerConstant.IAAS_PROVIDER_ELEMENT+"' element's '"+
            AutoscalerConstant.IAAS_PROVIDER_TYPE_ATTR+"' attribute should be specified!";
                
                handleException(msg);

            }
            
            // this is not mandatory
            iaas.setName(iaasElt.getAttribute(AutoscalerConstant.IAAS_PROVIDER_NAME_ATTR));
            
            iaas.setProperties(loadProperties(iaasElt));
            loadTemplate(iaas, iaasElt);
            loadScalingOrders(iaas, iaasElt);
            loadProvider(iaas, iaasElt);
            loadIdentity(iaas, iaasElt);
            loadCredentials(iaas, iaasElt);
        }
        
        
        return iaas;
    }
    
    /**
     * Load all ServiceTemplates from the configuration file and returns a list.
     * @return a list of ServiceTemplate instances.
     */
    public List<ServiceTemplate> getTemplates() {
        
        List<ServiceTemplate> templates = new ArrayList<ServiceTemplate>();
        
        // build default template object
        ServiceTemplate template = new ServiceTemplate();
        
        Element docEle = dom.getDocumentElement();
        NodeList nl = docEle.getElementsByTagName(AutoscalerConstant.DEFAULT_SERVICE_ELEMENT);
        
        if (nl != null && nl.getLength() > 0) {

            Node item = nl.item(0);

            if (item.getNodeType() == Node.ELEMENT_NODE) {
                Element defaultElt = (Element) item;
                template.setProperties(loadProperties(defaultElt));
            }

        }
        
        // append / overwrite the default template object with values in each domain
        nl = docEle.getElementsByTagName(AutoscalerConstant.SERVICE_ELEMENT);
        
        if (nl != null && nl.getLength() > 0) {

            for (int i = 0; i < nl.getLength(); i++) {
                Node item = nl.item(i);

                // clone the default template to an independent object
                try {
                    ServiceTemplate temp = (ServiceTemplate) template.clone();

                    if (item.getNodeType() == Node.ELEMENT_NODE) {
                        Element imageElt = (Element) item;

                        if ("".equals(imageElt.getAttribute(
                                             AutoscalerConstant.SERVICE_DOMAIN_ATTR))) {
                            String msg =
                                "Essential '"+AutoscalerConstant.SERVICE_DOMAIN_ATTR+"' " +
                                		"attribute of '"+AutoscalerConstant.SERVICE_ELEMENT+
                                		"' element cannot be found in " + elasticScalerConfigFile;

                            handleException(msg);
                        }

                        // set domain name
                        temp.setDomainName(imageElt.getAttribute(AutoscalerConstant.SERVICE_DOMAIN_ATTR));
                        
                        // set sub domain
                        temp.setSubDomainName(imageElt.getAttribute(
                                              AutoscalerConstant.SERVICE_SUB_DOMAIN_ATTR));
                        
                        // load custom properties
                        Map<String, String> customProperties = loadProperties(imageElt);
                        
                        // add custom properties (overwrite default properties where necessary)
                        for (Entry<String, String> pair : customProperties.entrySet()) {
                            temp.setProperty(pair.getKey(), pair.getValue());
                        }

                    }
                    
                    // add each domain specific template to list
                    templates.add(temp);

                } catch (CloneNotSupportedException e) {
                    String msg = "This is extraordinary!! ";
                    handleException(msg, e);
                }
            }
        }
        
        return templates;
    }
    
    private void loadCredentials(IaasProvider iaas, Element iaasElt) {

        NodeList nl = iaasElt.getElementsByTagName(AutoscalerConstant.CREDENTIAL_ELEMENT);

        // there should be only one credential element, we neglect all the others
        if (nl != null && nl.getLength() > 0) {
            
            if (nl.getLength() > 1){
                log.warn(elasticScalerConfigFile +" contains more than one "+
                         AutoscalerConstant.CREDENTIAL_ELEMENT+" elements!" +
                        " Elements other than the first will be neglected.");
            }
            
            if (nl.item(0).getNodeType() == Node.ELEMENT_NODE) {

                // retrieve the value using secure vault
                SecretResolver secretResolver = SecretResolverFactory.create(docEle, false);
                String alias;

                // FIXME following is a hack to find the correct alias.
                if (iaas.getProvider().contains("ec2")) {
                    alias = AutoscalerConstant.EC2_CREDENTIAL_ALIAS;
                } else {
                    alias = AutoscalerConstant.OPENSTACK_CREDENTIAL_ALIAS;
                }

                // retrieve the secured password
                if (secretResolver != null && secretResolver.isInitialized() && 
                        secretResolver.isTokenProtected(alias)) {

                    iaas.setCredential(secretResolver.resolve(alias));

                }

            }
        }
        else{
            String msg = "Essential '"+AutoscalerConstant.CREDENTIAL_ELEMENT+"' element" +
            		" has not specified in "+elasticScalerConfigFile;
            handleException(msg);
        }
    }

    private void loadIdentity(IaasProvider iaas, Element iaasElt) {

        NodeList nl = iaasElt.getElementsByTagName(AutoscalerConstant.IDENTITY_ELEMENT);

        // there should be only one identity element, we neglect all the others
        if (nl != null && nl.getLength() > 0) {
            
            if (nl.getLength() > 1){
                log.warn(elasticScalerConfigFile +" contains more than one "+
                        AutoscalerConstant.IDENTITY_ELEMENT+" elements!" +
                        " Elements other than the first will be neglected.");
            }
            
            if (nl.item(0).getNodeType() == Node.ELEMENT_NODE) {

                // retrieve the value using secure vault
                SecretResolver secretResolver = SecretResolverFactory.create(docEle, false);
                String alias;
                
                //FIXME following is a hack to find the correct alias.
                if(iaas.getProvider().contains("ec2")){
                    alias = AutoscalerConstant.EC2_IDENTITY_ALIAS;
                }
                else{
                    alias = AutoscalerConstant.OPENSTACK_IDENTITY_ALIAS;
                }

                // retrieve the secured password
                if (secretResolver != null && secretResolver.isInitialized() && 
                        secretResolver.isTokenProtected(alias)) {

                    iaas.setIdentity(secretResolver.resolve(alias));

                }

            }
        }
        else{
            String msg = "Essential '"+AutoscalerConstant.IDENTITY_ELEMENT+"' element" +
            		" has not specified in "+elasticScalerConfigFile;
            handleException(msg);
        }
    }

    private void loadProvider(IaasProvider iaas, Element iaasElt) {

        NodeList nl = iaasElt.getElementsByTagName(AutoscalerConstant.PROVIDER_ELEMENT);

        // there should be only one provider element, we neglect all the others
        if (nl != null && nl.getLength() > 0) {
            
            if (nl.getLength() > 1){
                log.warn(elasticScalerConfigFile +" contains more than one "+
                        AutoscalerConstant.PROVIDER_ELEMENT+" elements!" +
                        " Elements other than the first will be neglected.");
            }
            
            if (nl.item(0).getNodeType() == Node.ELEMENT_NODE) {
                Element prop = (Element) nl.item(0);

                iaas.setProvider(prop.getTextContent());

            }
        }
        else{
            String msg = "Essential '"+AutoscalerConstant.PROVIDER_ELEMENT+"' element " +
            		"has not specified in "+elasticScalerConfigFile;
            handleException(msg);
        }
    }

    private void loadScalingOrders(IaasProvider iaas, Element iaasElt) {

        NodeList nl = iaasElt.getElementsByTagName(AutoscalerConstant.SCALE_UP_ORDER_ELEMENT);

        // there should be only one scaleUpOrder element, we neglect all the others
        if (nl != null && nl.getLength() > 0) {
            
            if (nl.getLength() > 1){
                log.warn(elasticScalerConfigFile +" contains more than one "+
                        AutoscalerConstant.SCALE_UP_ORDER_ELEMENT+" elements!" +
                        " Elements other than the first will be neglected.");
            }
            
            if (nl.item(0).getNodeType() == Node.ELEMENT_NODE) {
                Element prop = (Element) nl.item(0);

                try {
                    iaas.setScaleUpOrder(Integer.parseInt(prop.getTextContent()));
                }catch (NumberFormatException e) {
                    String msg = AutoscalerConstant.SCALE_UP_ORDER_ELEMENT+" element contained" +
                    		" in "+elasticScalerConfigFile +"" +
                    		" has a value which is not an Integer value.";
                    handleException(msg, e);
                }

            }
        }
        else{
            String msg = "Essential '"+AutoscalerConstant.SCALE_UP_ORDER_ELEMENT+"' element" +
            		" has not specified in "+elasticScalerConfigFile;
            handleException(msg);
        }
        
        
        nl = iaasElt.getElementsByTagName(AutoscalerConstant.SCALE_DOWN_ORDER_ELEMENT);

        // there should be only one scaleDownOrder element, we neglect all the others
        if (nl != null && nl.getLength() > 0) {
            
            if (nl.getLength() > 1){
                log.warn(elasticScalerConfigFile +" contains more than one "+
                        AutoscalerConstant.SCALE_DOWN_ORDER_ELEMENT+" elements!" +
                        " Elements other than the first will be neglected.");
            }
            
            if (nl.item(0).getNodeType() == Node.ELEMENT_NODE) {
                Element prop = (Element) nl.item(0);

                try {
                    iaas.setScaleDownOrder(Integer.parseInt(prop.getTextContent()));
                }catch (NumberFormatException e) {
                    String msg = AutoscalerConstant.SCALE_DOWN_ORDER_ELEMENT+" element contained" +
                            " in "+elasticScalerConfigFile +"" +
                            " has a value which is not an Integer value.";
                    handleException(msg, e);
                }

            }
        }
        else{
            String msg = "Essential '"+AutoscalerConstant.SCALE_DOWN_ORDER_ELEMENT+"' element" +
                    " has not specified in "+elasticScalerConfigFile;
            handleException(msg);
        }
    }

    private void loadTemplate(IaasProvider iaas, Element iaasElt) {

        NodeList nl = iaasElt.getElementsByTagName(AutoscalerConstant.IMAGE_ID_ELEMENT);

        // there should be only one imageId element, we neglect all the others
        if (nl != null && nl.getLength() > 0) {
            
            if (nl.getLength() > 1){
                log.warn(elasticScalerConfigFile +" contains more than one "+
                        AutoscalerConstant.IMAGE_ID_ELEMENT+" elements!" +
                        " Elements other than the first will be neglected.");
            }
            
            if (nl.item(0).getNodeType() == Node.ELEMENT_NODE) {
                Element prop = (Element) nl.item(0);

                iaas.setTemplate(prop.getTextContent());

            }
        }
        else{
            String msg = "Essential '"+AutoscalerConstant.IMAGE_ID_ELEMENT+"' element" +
                    " has not specified in "+elasticScalerConfigFile;
            handleException(msg);
        }
    }

    private Map<String, String> loadProperties(Element iaasElt) {

        Map<String, String> propertyMap = new HashMap<String, String>();
        
        NodeList nl = iaasElt.getElementsByTagName(AutoscalerConstant.PROPERTY_ELEMENT);
        
        if (nl != null && nl.getLength() > 0) {
            for(int i=0; i< nl.getLength() ; i++){
                
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Element prop = (Element) nl.item(i);
                    
                    if("".equals(prop.getAttribute(AutoscalerConstant.PROPERTY_NAME_ATTR)) ||
                            "".equals(prop.getAttribute(AutoscalerConstant.PROPERTY_VALUE_ATTR))){
                        
                        String msg ="Property element's, name and value attributes should be specified " +
                        		"in "+elasticScalerConfigFile;

                        handleException(msg);
                    }
                    propertyMap.put(prop.getAttribute(AutoscalerConstant.PROPERTY_NAME_ATTR), 
                                    prop.getAttribute(AutoscalerConstant.PROPERTY_VALUE_ATTR));
                    
                }
            }
        }
        
        return propertyMap;
    }
    
    private void handleException(String msg){
        log.error(msg);
        throw new MalformedConfigurationFileException(msg);
    }
    
    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new MalformedConfigurationFileException(msg, e);
    }
	
    
}
