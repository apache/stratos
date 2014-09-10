package org.apache.stratos.rest.endpoint.bean.cartridge.definition;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "container")
public class ContainerBean {

	public String imageName;
	public String dockerfileRepo;
	public List<PropertyBean> property;
}
