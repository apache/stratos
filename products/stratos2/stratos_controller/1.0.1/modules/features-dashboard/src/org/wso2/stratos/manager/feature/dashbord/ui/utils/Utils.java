package org.wso2.stratos.manager.feature.dashbord.ui.utils;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.wso2.stratos.manager.feature.dashbord.ui.beans.Data;
import org.wso2.stratos.manager.feature.dashbord.ui.beans.Link;
import org.wso2.stratos.manager.feature.dashbord.ui.beans.Service;
import org.wso2.stratos.manager.feature.dashbord.ui.beans.Story;

public class Utils {

	public static Data pupulateDashboardFeatures() {
		File featureDashboardXML = new File(getFeatureXMLFilePath());
		//File featureDashboardXML = new File("features-dashboard.xml");
		
		Data data = new  Data();
		try {
			XMLStreamReader parser = xmlInputFactory.createXMLStreamReader(new FileInputStream(
					featureDashboardXML));
			StAXOMBuilder builder = new StAXOMBuilder(parser);
			OMElement documentElement =  builder.getDocumentElement();
			documentElement.build();
			Iterator<OMElement> txElements = documentElement.getChildrenWithName(new QName("service"));
		
			while (txElements.hasNext()) {
				OMElement txEle = txElements.next();
				data.addService(getService(txEle));
			}
			Service service = data.getService("manager");

//			Map<String, Story> stories = service.getStories();
//			System.out.println(stories.size());
//			int index = 1;
//			for (Map.Entry<String, Story> entry : stories.entrySet()) {
//				String divClassName = "story col" + index;
//				Story tempStory = entry.getValue();
//				String storyName = tempStory.getTitle();
//				String storyContent = tempStory.getContent();
//				System.out.println("Title : "+storyName+ " Content"+ storyContent);
				//Map<String, Link> links = tempStory.getLinks();
				//System.out.println("Size "+links.size());
//			}
//			String serviceNames [] = data.getKeys();
//			for (String name : serviceNames) {
//				System.out.println(name);
				
//				Service myservice =  data.getService(name);
//				String name1 = myservice.getName();
//				String link = myservice.getLink();
//			    System.out.println(name1 + "/" + link);
//			}
//			 Map<String, Service> allServices= data.getServices();
//			System.out.println(allServices.size());
//			for (Map.Entry<String, Service> entry : allServices.entrySet())
//			{
//				Service myservice =  entry.getValue();
//				String name = myservice.getName();
//				String link = myservice.getLink();
//			    System.out.println(name + "/" + link);
//			}


		 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return data;
	
	}
// + File.separator + "lib" + File.separator + 
	public static String getFeatureXMLFilePath() {
        String carbonHome = System.getProperty("carbon.home");
        if (carbonHome == null) {
            carbonHome = System.getenv("CARBON_HOME");
            System.setProperty("carbon.home", carbonHome);
        }
        return carbonHome+"/repository/conf/multitenancy/features-dashboard.xml";
    }
	private static Service getService(OMElement txEle) {
		OMAttribute serviceEle = txEle.getAttribute(new QName("name"));
		OMAttribute serviceLinkEle = txEle.getAttribute(new QName("link"));
		OMAttribute serviceKeyEle = txEle.getAttribute(new QName("key"));
		Service serviceInfo = new Service();
		serviceInfo.setName(serviceEle.getAttributeValue());
		serviceInfo.setLink(serviceLinkEle.getAttributeValue());
		serviceInfo.setKey(serviceKeyEle.getAttributeValue());
		Iterator<OMElement> storyElements = txEle.getChildrenWithName(new QName("story"));
		while (storyElements.hasNext()) {
			OMElement storyEle = storyElements.next();
     		serviceInfo.addStories(getStory(storyEle));
		}
		return serviceInfo;
		
	}
	
	private static Link getLink(OMElement linEle) {
	//	OMElement linkContentEle = linEle.getFirstChildWithName(new QName("link"));
		OMAttribute linkAttribtute = linEle.getAttribute(new QName("url"));
		Link link = new Link();
		link.setDescription(linEle.getText());
		link.setUrl(linkAttribtute.getAttributeValue());
		return link;
	}
	
	private static Story getStory(OMElement storyEle) {
		OMAttribute storyAttribtute = storyEle.getAttribute(new QName("title"));
		Story storyInfo = new Story();
		storyInfo.setTitle(storyAttribtute.getAttributeValue());
		OMElement storyContentEle = storyEle.getFirstChildWithName(new QName("story-content"));
		if (storyContentEle != null) {
			storyInfo.setContent(storyContentEle.getText());
		}
		OMElement storyLinkEle = storyEle.getFirstChildWithName(new QName("story-links"));
		if (storyLinkEle != null) {
			Iterator<OMElement> linkElements = storyLinkEle.getChildrenWithName(new QName("link"));
			while (linkElements.hasNext()) {
				OMElement linkEle = linkElements.next();
				storyInfo.addLink(getLink(linkEle));
			}
		}
		
		return storyInfo;
	}
	
	public static void main(String args[]) {
		pupulateDashboardFeatures();
//		String mystr[] = {"a","b","c","d"};
//		String wntedString="";
//		for (int i =0; i< mystr.length; i++) {
//		
//			if (i == mystr.length-1) {
//				wntedString = wntedString+mystr[i];
//			} else {
//				wntedString = wntedString+mystr[i]+",";
//			}
//		}
//		System.out.println(wntedString);
	}

	private static XMLInputFactory xmlInputFactory;

	/** pre-fetch the XMLInputFactory */
	static {
		xmlInputFactory = XMLInputFactory.newInstance();
	}
}
