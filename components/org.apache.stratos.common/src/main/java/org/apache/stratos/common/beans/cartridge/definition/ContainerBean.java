package org.apache.stratos.common.beans.cartridge.definition;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement(name = "container")
public class ContainerBean {

	private String imageName;
	private String dockerfileRepo;
	private List<PropertyBean> property;

	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public String getDockerfileRepo() {
		return dockerfileRepo;
	}

	public void setDockerfileRepo(String dockerfileRepo) {
		this.dockerfileRepo = dockerfileRepo;
	}

	public List<PropertyBean> getProperty() {
		return property;
	}

	public void setProperty(List<PropertyBean> property) {
		this.property = property;
	}
}
