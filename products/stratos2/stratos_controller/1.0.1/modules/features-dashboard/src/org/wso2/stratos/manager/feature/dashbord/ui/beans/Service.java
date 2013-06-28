package org.wso2.stratos.manager.feature.dashbord.ui.beans;

import java.util.HashMap;
import java.util.Map;

public class Service {

	private String name;
	private String link;
	private String key;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	private Map<String, Story> stories;

	public Service() {
		this.stories = new HashMap<String, Story>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Story> getStories() {
		return stories;
	}

	public void setStories(Map<String, Story> stories) {
		this.stories = stories;
	}

	public void addStories(Story story) {
		this.getStories().put(story.getTitle(), story);
	}

}
