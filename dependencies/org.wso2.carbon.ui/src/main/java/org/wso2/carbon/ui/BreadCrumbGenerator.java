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
package org.wso2.carbon.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.ui.deployment.beans.BreadCrumbItem;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Locale;

public class BreadCrumbGenerator {
	private static Log log = LogFactory.getLog(BreadCrumbGenerator.class);

	/**
	 * Generates breadcrumb html content.
	 * Please do not add line breaks to places where HTML content is written.
	 * It makes debugging difficult.
	 * @param request
	 * @param currentPageHeader
	 * @return String
	 */
	public HashMap<String,String> getBreadCrumbContent(HttpServletRequest request
			,BreadCrumbItem currentBreadcrumbItem
			,String jspFilePath
			,boolean topPage
			,boolean removeLastItem){
		String breadcrumbCookieString = "";
		//int lastIndexofSlash = jspFilePath.lastIndexOf(System.getProperty("file.separator"));
		//int lastIndexofSlash = jspFilePath.lastIndexOf('/');

		StringBuffer content = new StringBuffer();
		StringBuffer cookieContent = new StringBuffer();
		HashMap<String,String> breadcrumbContents = new HashMap<String,String>();
		HashMap<String, BreadCrumbItem> breadcrumbs =
			(HashMap<String, BreadCrumbItem>) request.getSession().getAttribute("breadcrumbs");

		String menuId = request.getParameter("item");
		String region = request.getParameter("region");
		String ordinalStr = request.getParameter("ordinal");

		if(topPage){
			//some wizards redirect to index page of the component after doing some operations.
			//Hence a map of region & menuId for component/index page is maintained & retrieved
			//by passing index page (eg: ../service-mgt/index.jsp)
			//This logic should run only for pages marked as toppage=true
			if(menuId == null && region == null){
				HashMap<String, String> indexPageBreadcrumbParamMap =
					(HashMap<String, String>) request.getSession().getAttribute("index-page-breadcrumb-param-map");
				//eg: indexPageBreadcrumbParamMap contains ../service-mgt/index.jsp : region1,services_list_menu pattern

				if(indexPageBreadcrumbParamMap != null && !(indexPageBreadcrumbParamMap.isEmpty())){
					String params = indexPageBreadcrumbParamMap.get(jspFilePath);
					if(params != null){
						region = params.substring(0, params.indexOf(','));
						menuId = params.substring(params.indexOf(',')+1);
					}
				}
			}
		}

		if (menuId != null && region != null) {
			String key = region.trim() + "-" + menuId.trim();
			HashMap<String, String> breadcrumbMap =
				(HashMap<String, String>) request.getSession().getAttribute(region + "menu-id-breadcrumb-map");

			String breadCrumb = "";
			if (breadcrumbMap != null && !(breadcrumbMap.isEmpty())) {
				breadCrumb = breadcrumbMap.get(key);
			}
			if (breadCrumb != null) {
				content.append("<table cellspacing=\"0\"><tr>");
                Locale locale = CarbonUIUtil.getLocaleFromSession(request);
                String homeText = CarbonUIUtil.geti18nString("component.home", "org.wso2.carbon.i18n.Resources", locale);
                content.append("<td class=\"breadcrumb-link\"><a href=\"" + CarbonUIUtil.getHomePage() + "\">"+homeText+"</a></td>");
				cookieContent.append(breadCrumb);
				cookieContent.append("#");
				generateBreadcrumbForMenuPath(content, breadcrumbs, breadCrumb,true);
			}
		}else{
			HashMap<String,List<BreadCrumbItem>> links = (HashMap<String,List<BreadCrumbItem>>) request
			.getSession().getAttribute("page-breadcrumbs");

			//call came within a page. Retrieve the breadcrumb cookie
			Cookie[] cookies = request.getCookies();
			for (int a = 0; a < cookies.length; a++) {
				Cookie cookie = cookies[a];
				if("current-breadcrumb".equals(cookie.getName())){
					breadcrumbCookieString = cookie.getValue();
					//bringing back the ,
					breadcrumbCookieString = breadcrumbCookieString.replace("%2C", ",");
					//bringing back the #
					breadcrumbCookieString = breadcrumbCookieString.replace("%23", "#");
					if(log.isDebugEnabled()){
						log.debug("cookie :"+cookie.getName()+" : "+breadcrumbCookieString);
					}
				}
			}


			if(links != null){
				if(log.isDebugEnabled()){
					log.debug("size of page-breadcrumbs is : "+links.size());
				}

				content.append("<table cellspacing=\"0\"><tr>");
                Locale locale = CarbonUIUtil.getLocaleFromSession(request);
                String homeText = CarbonUIUtil.geti18nString("component.home", "org.wso2.carbon.i18n.Resources", locale);
				content.append("<td class=\"breadcrumb-link\"><a href=\"" + CarbonUIUtil.getHomePage() + "\">"+homeText+"</a></td>");

				String menuBreadcrumbs = "";
				if(breadcrumbCookieString.indexOf('#') > -1){
					menuBreadcrumbs = breadcrumbCookieString.substring(0, breadcrumbCookieString.indexOf('#'));
				}
				cookieContent.append(menuBreadcrumbs);
				cookieContent.append("#");

				generateBreadcrumbForMenuPath(content, breadcrumbs,
						menuBreadcrumbs,false);

				int clickedBreadcrumbLocation = 0;
				if (ordinalStr != null) {
					//only clicking on already made page breadcrumb link will send this via request parameter
					try {
						clickedBreadcrumbLocation = Integer.parseInt(ordinalStr);
					} catch (NumberFormatException e) {
						// Do nothing
						log.warn("Found String for breadcrumb ordinal");
					}
				}

				String pageBreadcrumbs = "";
				if(breadcrumbCookieString.indexOf('#') > -1){
					pageBreadcrumbs = breadcrumbCookieString.substring(breadcrumbCookieString.indexOf('#')+1);
				}
				StringTokenizer st2 = new StringTokenizer(pageBreadcrumbs,"*");
				String[] tokens = new String[st2.countTokens()];
				int count = 0;
				String previousToken = "";
				while(st2.hasMoreTokens()){
					String currentToken = st2.nextToken();
					//To avoid page refresh create breadcrumbs
					if(! currentToken.equals(previousToken)){
						previousToken = currentToken;
						tokens[count] = currentToken;
						count++;
					}
				}


				//jspSubContext should be the same across all the breadcrumbs
				//(cookie is updated everytime a page is loaded)
				List<BreadCrumbItem> breadcrumbItems = null;
//				if(tokens != null && tokens.length > 0){
					//String token = tokens[0];
					//String jspSubContext = token.substring(0, token.indexOf('+'));
					//breadcrumbItems = links.get("../"+jspSubContext);
//				}

				LinkedList<String> tokenJSPFileOrder = new LinkedList<String>();
				LinkedList<String> jspFileSubContextOrder = new LinkedList<String>();
				HashMap<String,String> jspFileSubContextMap = new HashMap<String,String>();
				for(int a = 0;a < tokens.length;a++){
					String token = tokens[a];
					if(token != null){
						String jspFileName = token.substring(token.indexOf('+')+1);
						String jspSubContext = token.substring(0, token.indexOf('+'));
						jspFileSubContextMap.put(jspFileName, jspSubContext);
						tokenJSPFileOrder.add(jspFileName);
						jspFileSubContextOrder.add(jspSubContext+"^"+jspFileName);
					}
				}

				if(clickedBreadcrumbLocation > 0){
					int tokenCount = tokenJSPFileOrder.size();
					while (tokenCount > clickedBreadcrumbLocation) {
						String lastItem = tokenJSPFileOrder.getLast();
						if(log.isDebugEnabled()){
							log.debug("Removing breacrumbItem : "+ lastItem);
						}
						tokenJSPFileOrder.removeLast();
						jspFileSubContextOrder.removeLast();
						tokenCount = tokenJSPFileOrder.size();
					}
				}

				boolean lastBreadcrumbItemAvailable = false;
				if(clickedBreadcrumbLocation == 0){
					String tmp = getSubContextFromUri(currentBreadcrumbItem.getLink())+"+"+currentBreadcrumbItem.getId();
					if(! previousToken.equals(tmp)){ //To prevent page refresh
						lastBreadcrumbItemAvailable = true;
					}
				}


				if(tokenJSPFileOrder != null){
					//found breadcrumb items for given sub context
					for(int i = 0;i < jspFileSubContextOrder.size(); i++){
						String token = tokenJSPFileOrder.get(i);
						//String jspFileName = token.substring(token.indexOf('+')+1);
						//String jspSubContext = jspFileSubContextMap.get(jspFileName);

						String fileContextToken = jspFileSubContextOrder.get(i);
						String jspFileName = fileContextToken.substring(fileContextToken.indexOf('^')+1);
						String jspSubContext = fileContextToken.substring(0,fileContextToken.indexOf('^'));

						if(jspSubContext != null){
							breadcrumbItems = links.get("../"+jspSubContext);
						}
						if(breadcrumbItems != null){
							int bcSize = breadcrumbItems.size();
							for (int a = 0; a < bcSize ; a++) {
								BreadCrumbItem tmp = breadcrumbItems.get(a);
								if(tmp.getId().equals(jspFileName)){
									if(tmp.getLink().startsWith("#")){
										content.append("<td class=\"breadcrumb-link\">&nbsp;>&nbsp;"+tmp.getConvertedText()+"</td>");
									}else{
										//if((a+1) == bcSize){
										//if((a+1) == bcSize && clickedBreadcrumbLocation > 0){
										if((((a+1) == bcSize) && !(lastBreadcrumbItemAvailable))
												|| removeLastItem){
											content.append("<td class=\"breadcrumb-link\">&nbsp;>&nbsp;"+tmp.getConvertedText()+"</td>");
										}else{
											content.append("<td class=\"breadcrumb-link\">&nbsp;>&nbsp;<a href=\""+appendOrdinal(tmp.getLink(),i+1)+"\">"+tmp.getConvertedText()+"</a></td>");
										}
									}
									cookieContent.append(getSubContextFromUri(tmp.getLink())+"+"+token+"*");
								}
							}
						}
					}
				}

				//add last breadcrumb item
				if(lastBreadcrumbItemAvailable && !(removeLastItem)){
					String tmp = getSubContextFromUri(currentBreadcrumbItem.getLink())+"+"+currentBreadcrumbItem.getId();
					cookieContent.append(tmp);
					cookieContent.append("*");
					content.append("<td class=\"breadcrumb-link\">&nbsp;>&nbsp;"+currentBreadcrumbItem.getConvertedText()+"</td>");
				}
				content.append("</tr></table>");
			}
		}
		breadcrumbContents.put("html-content", content.toString());

		String finalCookieContent = cookieContent.toString();
		if(removeLastItem && breadcrumbCookieString != null && 
		    breadcrumbCookieString.trim().length() > 0){
				finalCookieContent = breadcrumbCookieString;
		}
		breadcrumbContents.put("cookie-content", finalCookieContent);
		return breadcrumbContents;
	}

	/**
	 *
	 * @param uri
	 * @return
	 */
	private static String getSubContextFromUri(String uri){
		//eg: uri = ../service-mgt/service_info.jsp?serviceName=HelloService&ordinal=1
		if(uri != null){
			int jspExtensionLocation = uri.indexOf(".jsp");
			String tmp = uri.substring(0, jspExtensionLocation);
			tmp = tmp.replace("../", "");
			int firstSlashLocation = tmp.indexOf('/');
			return tmp.substring(0, firstSlashLocation);
		}else{
			return "";
		}
	}

	/**
	 *
	 * @param url
	 * @param ordinal
	 * @return
	 */
	private static String appendOrdinal(String url, int ordinal) {
		if(url != null){
			if (url.indexOf('?') > -1) {
				return url + "&ordinal=" + ordinal;
			} else {
				return url + "?ordinal=" + ordinal;
			}
		}else{
			return "#";
		}
	}

	/**
	 * Generates breadcrumb for menu navigation path
	 * @param content
	 * @param breadcrumbs
	 * @param breadCrumb
	 */
	private void generateBreadcrumbForMenuPath(StringBuffer content,
			HashMap<String, BreadCrumbItem> breadcrumbs, String breadCrumb,boolean clickFromMenu) {
		StringTokenizer st = new StringTokenizer(breadCrumb, ",");
		int tokenCount = st.countTokens();
		int count = 0;
		while (st.hasMoreTokens()) {
			count++;
			String token = st.nextToken();
			BreadCrumbItem breadcrumbItem = (BreadCrumbItem) breadcrumbs.get(token);
			if (breadcrumbItem != null) {
				//if (count == tokenCount) {
				//	content.append("<td class=\"breadcrumb-current-page\"><a href=\""+breadcrumbItem.getLink()+"\">"+breadcrumbItem.getConvertedText()+"</a></td>");
				//} else {
					if (breadcrumbItem.getLink().startsWith("#")) {
						content.append("<td class=\"breadcrumb-link\">"+"&nbsp;>&nbsp;"+breadcrumbItem.getConvertedText()+"</td>");
					} else {
						if(count == tokenCount && (clickFromMenu)){//if last breadcrumb item, do not put the link
							content.append("<td class=\"breadcrumb-link\">&nbsp;>&nbsp;"+breadcrumbItem.getConvertedText()+"</td>");
						}else{
							content.append("<td class=\"breadcrumb-link\">&nbsp;>&nbsp;<a href=\""+breadcrumbItem.getLink()+"\">"+breadcrumbItem.getConvertedText()+"</a></td>");
						}
					}
				//}
			}
		}
	}
}
