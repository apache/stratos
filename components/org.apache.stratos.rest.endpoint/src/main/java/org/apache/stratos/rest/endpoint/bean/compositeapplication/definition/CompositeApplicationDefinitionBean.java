package org.apache.stratos.rest.endpoint.bean.compositeapplication.definition;

import javax.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement(name = "applicationDefinitionBean")

public class CompositeApplicationDefinitionBean {
	
	public String applicationId;
	public String alias;
	public List<ComponentDefinition> components;
	public List<CartridgeDefinition> cartridges;
	

}
