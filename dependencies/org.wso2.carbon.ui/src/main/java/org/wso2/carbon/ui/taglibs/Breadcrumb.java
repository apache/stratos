/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.ui.taglibs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.ui.BreadCrumbGenerator;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.ui.deployment.beans.BreadCrumbItem;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Breadcrumb extends BodyTagSupport {
	private static final long serialVersionUID = 3086447243740241245L;
	private static final Log log = LogFactory.getLog(Breadcrumb.class);
	private String label;
	private String resourceBundle;
	private boolean topPage;
	private HttpServletRequest request;
	private boolean hidden = false;
	private String disableBreadCrumbsProperty="org.wso2.carbon.ui.disableBreadCrumbs";
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getResourceBundle() {
		return resourceBundle;
	}

	public void setResourceBundle(String resourceBundle) {
		this.resourceBundle = resourceBundle;
	}

	public boolean isTopPage() {
		return topPage;
	}

	public void setTopPage(boolean topPage) {
		this.topPage = topPage;
	}

	public HttpServletRequest getRequest() {
		return request;
	}

	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}
    
	public boolean getHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	/**
	 * JSP end tag for breadcrumb tag
	 */
	public int doEndTag() throws JspException {
	    String disableBreadCrumbs=System.getProperty(disableBreadCrumbsProperty);
	    boolean isBreadCrumbDisabled = (disableBreadCrumbs==null) ? false : disableBreadCrumbs.equalsIgnoreCase("true");
		String breadcrumbConent = "";
		String cookieContent = "";
		JspWriter writer = pageContext.getOut();
	    if (isBreadCrumbDisabled){
            try {
			    writer.write("");
		    } catch (IOException e) {
		    	//do nothing
		    }
	        return 0;
        }
        
		StringBuffer content = new StringBuffer();
		

		if (request != null) {
			String retainLastBreadcrumbStr = request.getParameter("retainlastbc");
			if(log.isDebugEnabled()){
				log.debug("BreadcrumbTag : " + request.getPathTranslated());				
			}

            String path = (String) request.getAttribute("javax.servlet.include.request_uri");

			// now path contains value similar to following. eg: path =
			// /carbon/userstore/index.jsp
			// Find last occurance of "carbon". This is the starting of web app context.
			int carbonLocation = path.lastIndexOf("carbon");
			String jspFilePath = path.substring(carbonLocation, path.length());			
			// now, jspFilePath = carbon/service-mgt/list_service_main.jsp
			// now, replace 'carbon' and you get path to real file name
			jspFilePath = jspFilePath.replaceFirst("carbon", "..");
			
			//Find subcontext before jsp file
			int lastIndexofSlash = jspFilePath.lastIndexOf('/');
			String subContextToJSP = jspFilePath.substring(0,lastIndexofSlash);

			//Find jsp file name
			String jspFileName = jspFilePath.substring(lastIndexofSlash+1, jspFilePath.length());
			//save query string for current url
			String queryString = request.getQueryString();
			
			//creating a new breadcrumb item for page request
			BreadCrumbItem breadCrumbItem = new BreadCrumbItem();			
			//creating breadcrumb id using jsp file path
			//This is guaranteed to be unique for a subcontext (eg: /modulemgt,/service-listing)
			breadCrumbItem.setId(jspFileName);
			
            Locale locale = CarbonUIUtil.getLocaleFromSession(request);
            String text = CarbonUIUtil.geti18nString(label, resourceBundle, locale);
			breadCrumbItem.setConvertedText(text);	
			
			//if request contains parameter 'toppage', override the value of this.topPage with
			//the value set in request.
			//This is useful when same page is being used @ different levels. 
			//eg: wsdl2code/index.jsp
			//This page is being called from Tools -> WSDL2Code & Service Details -> Generate Client
			String topPageParameter = request.getParameter("toppage");
			if(topPageParameter != null){
				boolean topPageParamValue = Boolean.valueOf(topPageParameter).booleanValue();
				if(log.isDebugEnabled()){
					log.debug("toppage value set from request parameter.("+topPageParamValue+").");
				}
				this.topPage = topPageParamValue;
			}

			if(! topPage){
				// need to add this url as a breadcrumb
				HashMap<String,List<BreadCrumbItem>> links = (HashMap<String,List<BreadCrumbItem>>) request
						.getSession().getAttribute("page-breadcrumbs");
				
				String partUrl = "";
				if(queryString != null){
					partUrl = jspFilePath + "?" + queryString ;
				}else{
					partUrl = jspFilePath;
				}
				
				if (links != null) {
					//check if a breadcrumb exists for given sub context
					List<BreadCrumbItem> breadcrumbsForSubContext = links.get(subContextToJSP);
					int size = 0;
					if(breadcrumbsForSubContext != null){
						int sizeOfSubContextBreadcrumbs = breadcrumbsForSubContext.size();
						//removing to stop this array getting grown with duplicates
						ArrayList idsToRemove = new ArrayList();						
						for(int a = 0;a < sizeOfSubContextBreadcrumbs;a++){
							if(breadcrumbsForSubContext.get(a).getId().equals(jspFileName)){
								idsToRemove.add(a);
							}
						}
						if(idsToRemove.size() > 0){
                            for (Object anIdsToRemove : idsToRemove) {
                                Integer i = (Integer) anIdsToRemove;
                                breadcrumbsForSubContext.remove(i.intValue());
                            }
						}
						
						size = breadcrumbsForSubContext.size();
						breadCrumbItem.setOrder(size + 1);
						breadCrumbItem.setLink(partUrl);
						breadcrumbsForSubContext.add(breadCrumbItem);
						links.put(subContextToJSP,breadcrumbsForSubContext);					
						request.getSession().setAttribute("page-breadcrumbs", links);				
					}else{
						breadcrumbsForSubContext = new ArrayList<BreadCrumbItem>();
						breadCrumbItem.setOrder(size + 1);
						breadCrumbItem.setLink(partUrl);
						breadcrumbsForSubContext.add(breadCrumbItem);
						links.put(subContextToJSP,breadcrumbsForSubContext);					
						request.getSession().setAttribute("page-breadcrumbs", links);			
					}
				} else {
					HashMap<String,List<BreadCrumbItem>> tmp = new HashMap<String,List<BreadCrumbItem>>();
					// Going inside for the first time
					breadCrumbItem.setOrder(1);
					breadCrumbItem.setLink(partUrl);
					List<BreadCrumbItem> list = new ArrayList<BreadCrumbItem>();
					list.add(breadCrumbItem);
					tmp.put(subContextToJSP,list);
					request.getSession().setAttribute("page-breadcrumbs", tmp);
				}				
			}
			boolean retainLastBreadcrumb = false;
			if(retainLastBreadcrumbStr != null){
				retainLastBreadcrumb = Boolean.parseBoolean(retainLastBreadcrumbStr);
			}
			
			BreadCrumbGenerator breadCrumbGenerator = new BreadCrumbGenerator();			
			HashMap<String,String> generatedContent = breadCrumbGenerator.getBreadCrumbContent(
					request, breadCrumbItem,jspFilePath,topPage,retainLastBreadcrumb);
			breadcrumbConent = generatedContent.get("html-content");
			cookieContent = generatedContent.get("cookie-content");
		}

        content.append("<script type=\"text/javascript\">\n");
        content.append("    setCookie('current-breadcrumb', '"+cookieContent+"');\n");
        content.append("    document.onload=setBreadcrumDiv();\n");
		content.append("    function setBreadcrumDiv () {\n");
		content.append("        var breadcrumbDiv = document.getElementById('breadcrumb-div');\n");
		if(! hidden){
			content.append("        breadcrumbDiv.innerHTML = '" + breadcrumbConent + "';\n");			
		}else{
			//do not print breadcrumb
			content.append("        breadcrumbDiv.innerHTML = '';\n");			
		}
		content.append("    }\n");
		content.append("</script>\n");

		try {
			writer.write(content.toString());
		} catch (IOException e) {
			String msg = "Cannot write breadcrumb tag content";
			log.error(msg, e);

			try {
				//exit gracefully
				writer.write(""); 
			} catch (IOException e1) {
			    //do nothing
			}
		}
		return 0;
	}

	/**
	 * replaces backslash with forward slash
	 * @param str
	 * @return
	 */
	private static String replaceBacklash(String str){
	    StringBuilder result = new StringBuilder();
	    StringCharacterIterator iterator = new StringCharacterIterator(str);
	    char character =  iterator.current();
	    while (character != CharacterIterator.DONE ){	     
	      if (character == '\\') {
	         result.append("/");
	      }else {
	        result.append(character);
	      }	      
	      character = iterator.next();
	    }
	    return result.toString();
	}	

}
