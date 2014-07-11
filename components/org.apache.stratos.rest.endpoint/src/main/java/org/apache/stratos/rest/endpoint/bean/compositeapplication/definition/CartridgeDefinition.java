package org.apache.stratos.rest.endpoint.bean.compositeapplication.definition;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "cartridge")
public class CartridgeDefinition {
	public String alias;
    public String type;
    private String deploymentPolicy;
    private String autoscalingPolicy;
    private String repoUrl;
    private boolean privateRepo;
    private String username;
    private String password;
}
