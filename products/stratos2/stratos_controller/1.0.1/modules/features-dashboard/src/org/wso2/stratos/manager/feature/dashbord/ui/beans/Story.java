package org.wso2.stratos.manager.feature.dashbord.ui.beans;

import java.util.HashMap;
import java.util.Map;

public class Story {
	
	private String title;
	private Map<String, Link> links;
	private String content;
	
	public Story () {
		this.links =  new HashMap<String,Link>();
	}
	
	public Map<String, Link> getLinks() {
		return links;
	}

	public void setLinks(Map<String, Link> links) {
		this.links = links;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void addLink(Link link) {
		this.getLinks().put(link.getUrl(), link);
	}

}
