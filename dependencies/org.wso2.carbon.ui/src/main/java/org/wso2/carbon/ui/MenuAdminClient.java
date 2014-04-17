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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.wso2.carbon.ui.deployment.beans.BreadCrumbItem;
import org.wso2.carbon.ui.deployment.beans.CarbonUIDefinitions;
import org.wso2.carbon.ui.deployment.beans.Menu;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class MenuAdminClient {
    private static Log log = LogFactory.getLog(MenuAdminClient.class);
    private Menu[] menus;
    private StringBuffer menuContent;
    private Map<String, Menu> parentMenuItems;
    private Map<String, ArrayList<Menu>> childMenuItems;
    private Map<String, String> breadcrumbMap = new HashMap<String, String>();
    //eg: holds ../service-mgt/index.jsp : region1,services_list_menu
    private Map<String, String> indexPageBreadcrumbParamMap;
    public static final String USER_MENU_ITEMS = "UserMenuItems";
    public static final String USER_CUSTOM_MENU_ITEMS = "UserCustomMenuItems";
    public static final String USER_MENU_ITEMS_FILTERED = "UserMenuItemsFiltered";
    
    public MenuAdminClient() {

    }

    public Map<String, String> getBreadcrumbMap() {
        return breadcrumbMap;
    }

    public void setBreadcrumbMap(HashMap<String, String> breadcrumbMap) {
        this.breadcrumbMap = breadcrumbMap;
    }

    /**
     * Calls the OSGi service for UI & retrieves Menu definitions
     */
    private void populateMenuDefinitionsFromOSGiService(String loggedInUserName
            ,boolean isSuperTenant
    		,ArrayList<String> userPermission
    		,HttpServletRequest request) {

    	if(loggedInUserName != null){
    		//A user has logged in, get menus from user's session
            Menu[] userMenus = (Menu[]) request.getSession().getAttribute(USER_MENU_ITEMS);
            if (userMenus != null) {
                Set<Menu> menuList = new LinkedHashSet<Menu>();
                menuList.addAll(Arrays.<Menu>asList(userMenus));
                Menu[] customMenus = (Menu[]) request.getSession().getAttribute(USER_CUSTOM_MENU_ITEMS);
                if (customMenus != null) {
                    menuList.addAll(Arrays.<Menu>asList(customMenus));
                }
                menus = menuList.toArray(new Menu[menuList.size()]);
            }
            if (menus == null){
                setDefaultMenus(loggedInUserName, isSuperTenant, userPermission, request);
            }
            
    		String filtered = (String)request.getSession().getAttribute(MenuAdminClient.USER_MENU_ITEMS_FILTERED);
    		if("false".equals(filtered)){
    			CarbonUIDefinitions o = new CarbonUIDefinitions();
    			Menu[] filteredMenus = o.getMenuDefinitions(loggedInUserName, isSuperTenant, userPermission, request, menus);
    			menus = filteredMenus;
    			request.getSession().setAttribute(USER_MENU_ITEMS,menus);
    		}
    		if(menus != null){
    			if(log.isDebugEnabled()){
    				log.debug("Loaded menu items from user session");
    			}
    			return;
    		}
    	}

        setDefaultMenus(loggedInUserName, isSuperTenant, userPermission, request);
    }

    private void setDefaultMenus(String loggedInUserName,
                                 boolean isSuperTenant,
                                 ArrayList<String> userPermission,
                                 HttpServletRequest request) {
        BundleContext bundleContext = CarbonUIUtil.getBundleContext();
        if (bundleContext != null) {
            ServiceReference reference = bundleContext.getServiceReference(CarbonUIDefinitions.class.getName());
            CarbonUIDefinitions carbonUIDefinitions;
            if (reference != null) {
                carbonUIDefinitions = (CarbonUIDefinitions) bundleContext.getService(reference);
                Menu[] userMenus = carbonUIDefinitions.getMenuDefinitions(loggedInUserName,
                            isSuperTenant, userPermission,request);
                if (userMenus != null) {
                    Set<Menu> menuList = new LinkedHashSet<Menu>();
                    menuList.addAll(Arrays.<Menu>asList(userMenus));
                    Menu[] customMenus =
                            (Menu[]) request.getSession().getAttribute(USER_CUSTOM_MENU_ITEMS);
                    if (customMenus != null) {
                        menuList.addAll(Arrays.<Menu>asList(customMenus));
                    }
                    menus = menuList.toArray(new Menu[menuList.size()]);
                	if (log.isDebugEnabled()) {
                        log.debug("Found exiting menu items in OSGI context");
                    }
                }
            }
        }
    }

    private String findNavigationPathFromRoot(String lastMenuItem, String path) {
        for (int a = 0; a < menus.length; a++) {
            Menu menu = menus[a];
            if (log.isDebugEnabled()) {
                log.debug(a + " : " + lastMenuItem + " : " + menu.getId());
            }
            if (menu.getId() != null && menu.getId().equals(lastMenuItem)) {
                //path = ":"+menu.getI18nKey()+"#"+menu.getI18nBundle() + path;
                path = "," + menu.getId() + path;
                String parentMenuId = menu.getParentMenu();
                if (parentMenuId.trim().length() > 0) {
                    path = findNavigationPathFromRoot(parentMenuId, path);
                } else {
                    break;
                }
            }
        }
        return path;
    }

    /**
     *
     */
    public void setBreadCrumbMap(HttpServletRequest request) {
        Locale locale;
        HashMap<String, BreadCrumbItem> breadCrumbs = new HashMap<String, BreadCrumbItem>();
        if (menus != null) {
            for (int a = 0; a < menus.length; a++) {
                Menu menu = menus[a];
                if (menu.getId() != null) {
                    BreadCrumbItem bc = new BreadCrumbItem();
                    CarbonUIUtil.setLocaleToSession(request);

                    locale = CarbonUIUtil.getLocaleFromSession(request);
                    java.util.ResourceBundle bundle = null;
                    try {
                    	if(menu.getI18nBundle() != null){
                            bundle = java.util.ResourceBundle.getBundle(menu.getI18nBundle(), locale);
                    	}
                    } catch (java.util.MissingResourceException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Cannot find resource bundle : " + menu.getI18nBundle());
                        }
                    }

                    String menuText = menu.getI18nKey();
                    if (bundle != null) {
                        String tmp = null;
                        try {
                        	if(menu.getI18nKey() != null){
                                tmp = bundle.getString(menu.getI18nKey());                        		
                        	}
                        } catch (java.util.MissingResourceException e) {
                            //Missing key should not be a blocking factor for UI rendering
                            if (log.isDebugEnabled()) {
                                log.debug("Cannot find resource for key :" + menu.getI18nKey());
                            }
                        }
                        if (tmp != null) {
                            menuText = tmp;
                        }
                    }

                    bc.setConvertedText(menuText);
                    bc.setI18nBundle(menu.getI18nBundle());
                    bc.setI18nKey(menu.getI18nKey());
                    bc.setId(menu.getId());
                    bc.setLink(menu.getLink() + "?region=" + menu.getRegion() + "&amp;item=" +
                            menu.getId() + (menu.getUrlParameters() != null ?
                            "&amp;" + menu.getUrlParameters() : "") + "&amp;ordinal=0");
                    breadCrumbs.put(menu.getId(), bc);
                }
            }
        }
        request.getSession().setAttribute("breadcrumbs", breadCrumbs);
    }

    /**
     * Returns left hand side menu as a String
     *
     * @param region
     */
    public String getMenuContent(String region, HttpServletRequest request) {
        boolean authenticated = isAuthenticated(request);
        ArrayList<String> userPermission = new ArrayList<String>();

        String loggedInUserName = null;
        boolean isSuperTenant = false;
        if (authenticated) {
            loggedInUserName = (String) request.getSession().getAttribute(CarbonSecuredHttpContext.LOGGED_USER);
            userPermission = (ArrayList<String>) request.getSession().getAttribute(ServerConstants.USER_PERMISSIONS);
            isSuperTenant = CarbonUIUtil.isSuperTenant(request);
        }
        

        populateMenuDefinitionsFromOSGiService(loggedInUserName,isSuperTenant,userPermission,request);
        //populate();
        checkForIndexPageBreadcrumbParamMap(request);
        if (menus != null && menus.length > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Size of menu items for region : " + region + " is " + menus.length);
                for (int a = 0; a < menus.length; a++) {
                    log.debug(menus[a]);
                }
            }

            menuContent = new StringBuffer();
            appendMenuHeader();
            if (region.equals("region1")) {
                Locale locale =CarbonUIUtil.getLocaleFromSession(request);
                appendHomeLink(locale);
            }

            //build a hierarchy of MenuItems
            parentMenuItems = new HashMap<String, Menu>();
            childMenuItems = new HashMap<String, ArrayList<Menu>>();

            for (int a = 0; a < menus.length; a++) {
                boolean display = false;
                if (!authenticated) {
                    if (!menus[a].getRequireAuthentication()) {
                        display = true;
                    }
                } else {
                    //display all menu items for logged in user
                    display = true;
                }

                if (region.equals(menus[a].getRegion()) && display) {
                    if ("".equals(menus[a].getParentMenu())) {
                        parentMenuItems.put(menus[a].getId(), menus[a]);
                    } else {
                        ArrayList<Menu> childMenus = (ArrayList) childMenuItems.get(menus[a].getParentMenu());
                        if (childMenus != null && childMenus.size() > 0) {
                            childMenus.add(menus[a]);
                        } else {
                            ArrayList<Menu> tmp = new ArrayList();
                            tmp.add(menus[a]);
                            childMenuItems.put(menus[a].getParentMenu(), tmp);
                        }
                    }
                }
            }

            //Iterate through the parent menu items & build the menu style
            String[] sortedParentMenuIds = sortMenuItems(parentMenuItems);
            for (int a = 0; a < sortedParentMenuIds.length; a++) {
                String key = sortedParentMenuIds[a];
                Menu menu = (Menu) parentMenuItems.get(key);
                ArrayList childMenusForParent = (ArrayList) childMenuItems.get(menu.getId());
                if(childMenusForParent != null){ //if no child menu items, do not print the parent
                    menuContent.append(getHtmlForMenuItem(menu, request));
                    prepareHTMLForChildMenuItems(menu.getId(), request);
                }
            }
            //Old way of generating menu items without ordering
/*
			Iterator itrParentMenuKeys = parentMenuItems.keySet().iterator();
			while(itrParentMenuKeys.hasNext()){
				String key = (String)itrParentMenuKeys.next();
				Menu menu = (Menu)parentMenuItems.get(key);
				menuContent.append(getHtmlForMenuItem(menu,locale));
				prepareHTMLForChildMenuItems(key,locale);
			}
*/
            appendMenuFooter();
            request.getSession().setAttribute(region + "menu-id-breadcrumb-map", breadcrumbMap);
            request.getSession().setAttribute("index-page-breadcrumb-param-map", indexPageBreadcrumbParamMap);

            return menuContent.toString();
        } else {
            //no menu, return empty String
            return "";
        }
    }

    private void checkForIndexPageBreadcrumbParamMap(HttpServletRequest request) {
        HashMap<String, String> tmp = (HashMap<String, String>) request.getSession().getAttribute("index-page-breadcrumb-param-map");
        if (tmp != null) {
            this.indexPageBreadcrumbParamMap = tmp;
        } else {
            this.indexPageBreadcrumbParamMap = new HashMap<String, String>();
        }
    }

    /**
     * check for authenticated session
     *
     * @param request
     * @return
     */
    private boolean isAuthenticated(HttpServletRequest request) {
        Boolean authenticatedObj = (Boolean) request.getSession().getAttribute("authenticated");
        boolean authenticated = false;
        if (authenticatedObj != null) {
            authenticated = authenticatedObj.booleanValue();
        }
        return authenticated;
    }

    /**
     * @param menuItems
     * @return
     */
    private String[] sortMenuItems(Map menuItems) {
        Iterator itrMenuKeys = menuItems.keySet().iterator();
        int[] menuOrder = new int[menuItems.size()];
        String[] menuIds = new String[menuItems.size()];

        int index = 0;
        while (itrMenuKeys.hasNext()) {
            String key = (String) itrMenuKeys.next();
            Menu menu = (Menu) menuItems.get(key);
            int ordinal;
            try {
                ordinal = Integer.parseInt(menu.getOrder());
            } catch (NumberFormatException e) {
                //if provided value is NaN, this menu item deserves the last position ;-)
                ordinal = 200;
                log.debug("Hey...whoever defined the menu item : " + menu.getId()
                        + ",please provide a integer value for 'order'", e);
            }
            menuOrder[index] = ordinal;
            menuIds[index] = menu.getId();
            index++;
        }
        sortArray(menuOrder, menuIds);
        return menuIds;
    }

    /**
     * @param parentMenuId
     */
    private void prepareHTMLForChildMenuItems(String parentMenuId, HttpServletRequest request) {
        Locale locale = request.getLocale();
        ArrayList childMenusForParent = (ArrayList) childMenuItems.get(parentMenuId);
        if (childMenusForParent != null) {
            //create a hashmap of child Menu items for parent
            Iterator itrChildMenusForParent = childMenusForParent.iterator();
            HashMap childMenus = new HashMap();
            for (; itrChildMenusForParent.hasNext();) {
                Menu menu = (Menu) itrChildMenusForParent.next();
                childMenus.put(menu.getId(), menu);
            }
            String[] sortedMenuIds = sortMenuItems(childMenus);

            if (sortedMenuIds.length > 0) {
                menuContent.append("<li class=\"normal\">");
                menuContent.append("<ul class=\"sub\">");
                for (int a = 0; a < sortedMenuIds.length; a++) {
                    Menu menu = (Menu) childMenus.get(sortedMenuIds[a]);
                    
                    ArrayList childs = (ArrayList) childMenuItems.get(menu.getId());
                    if(childs == null){
                    	if(! menu.getLink().equals("#") && menu.getLink().trim().length() > 0){
                    		//This is the last menu item, print it
                    		menuContent.append(getHtmlForMenuItem(menu, request));
                    	}                    	
                    }else{
                    	//If no childs & current menu item does not contain an link
                    	//do not print                    
                        menuContent.append(getHtmlForMenuItem(menu, request));
                        prepareHTMLForChildMenuItems(menu.getId(), request);                    	
                    }                 
                }
                menuContent.append("</ul>");
                menuContent.append("</li>");
            }
        }
    }

    /**
     * @param menu
     * @return
     */
    private String getHtmlForMenuItem(Menu menu, HttpServletRequest request) {
        Locale locale;
        CarbonUIUtil.setLocaleToSession(request);
        locale = CarbonUIUtil.getLocaleFromSession(request);
        java.util.ResourceBundle bundle = null;
        try {
            if (menu.getI18nBundle() != null) {
                bundle = java.util.ResourceBundle.getBundle(menu.getI18nBundle(), locale);
            }
        } catch (MissingResourceException e) {
            if(log.isDebugEnabled()){
                log.debug("Cannot find resource bundle : "+menu.getI18nBundle());
            }
        }

        String menuText = menu.getI18nKey();
        if (bundle != null) {
            String tmp = null;
            try {
                tmp = bundle.getString(menu.getI18nKey());
            } catch (MissingResourceException e) {
                //Missing key should not be a blocking factor for UI rendering

                if(log.isDebugEnabled()){
                    log.debug("Cannot find resource for key :"+menu.getI18nKey());
                }
            }
            if (tmp != null) {
                menuText = tmp;
            }
        }

        String html = "";
        if (menu.getParentMenu().trim().length() == 0
                || menu.getLink().trim().length() == 0) {
            html = "<li id=\""
            	+menu.getRegion()
            	+"_"+ menu.getId()
            	+"\" class=\"menu-header\"  onclick=\"mainMenuCollapse(this.childNodes[0])\" style=\"cursor:pointer\">" 
            	+ "<img src=\"../admin/images/up-arrow.gif\" " +
            			"class=\"mMenuHeaders\" id=\""+menu.getRegion()+"_"+ menu.getId()+"\"/>"
            	+ menuText            	
            	+ "</li>";
        } else {
        	String link = menu.getLink() + "?region=" + menu.getRegion() + "&amp;item=" + menu.getId();

        	//if user has set url parameters, append it to link
        	String urlParameters = menu.getUrlParameters();
        	if(urlParameters != null && urlParameters.trim().length() > 0){
        		link = link +"&amp;"+menu.getUrlParameters();
        	}
        	
            String iconPath = "../admin/images/default-menu-icon.gif";
            if (menu.getIcon() != null && menu.getIcon().trim().length() > 0) {
                iconPath = menu.getIcon();
            }
            String breadCrumbPath = findNavigationPathFromRoot(menu.getId(), "");
            breadCrumbPath = breadCrumbPath.replaceFirst(",", "");
            breadcrumbMap.put(menu.getRegion().trim() + "-" + menu.getId().trim(), breadCrumbPath);
            String params = menu.getRegion().trim() + "," + menu.getId().trim();
            indexPageBreadcrumbParamMap.put(link, params);

            String style = "";
            if (menu.getLink().equals("#")) {
                style = "menu-disabled-link";
                html = "<li class=\"" + style + "\" style=\"background-image: url(" + iconPath + ");\">"+ menuText + "</li>" + "\n";
            } else {
                style = "menu-default";
                html = "<li><a href=\"" + link + "\" class=\"" + style + "\" style=\"background-image: url(" + iconPath + ");\">" + menuText + "</a></li>" + "\n";
            }           
            
            //html = "<li><a href=\"" + menu.getLink() + "?region=" + menu.getRegion() + "&amp;item=" + menu.getId() + "\" class=\"" + style + "\" style=\"background-image: url(" + iconPath + ");\">" + menuText + "</a></li>" + "\n";
        }
        return html;
    }


    private void appendHomeLink(Locale locale) {
    	String homeText = CarbonUIUtil.geti18nString("component.home", "org.wso2.carbon.i18n.Resources", locale);
        menuContent.append("<li><a href=\""+ CarbonUIUtil.getHomePage() +"\" class=\"menu-home\">"+homeText+"</a></li>");
    }

    /**
     *
     */
    private void appendMenuHeader() {
        menuContent.append("<div id=\"menu\">");
        menuContent.append(" <ul class=\"main\">");
    }

    /**
     *
     */
    private void appendMenuFooter() {
        menuContent.append(" </ul>");
        menuContent.append("</div>");
    }

    /**
     * @param a
     * @param names
     */
    static void sortArray(int[] a, String[] names) {
        for (int i = 0; i < a.length - 1; i++) {
            for (int j = i + 1; j < a.length; j++) {
                if (a[i] > a[j]) {
                    int temp = a[i];
                    String name = names[i];
                    a[i] = a[j];
                    names[i] = names[j];
                    a[j] = temp;
                    names[j] = name;
                }
            }
        }
    }


    /**
     * To be used only for testing purposes
     *
     * @param region
     * @param locale
     * @return
     */
    /*
     static void printArray(int[] a) {
         for (int i = 0; i < a.length; i++)
             System.out.print(" " + a[i]);
         System.out.print("\n");
     }

     static void printArray(String[] a) {
         for (int i = 0; i < a.length; i++)
             System.out.print(" " + a[i]);
         System.out.print("\n");
     }

     public static void main(String[] args) {
         int[] num = {1,7,4,3,6};
         String[] names = {"nandika","dinuka","prabath","sumedha","chamara"};
         printArray(num);
         sortArray(num,names);
         printArray(num);
         printArray(names);
     }

     public String getStaticMenuContent(String region,Locale locale){
         String menuContent = "";
         if("region1".equals(region)){
             menuContent =
                 "<div id=\"menu\">"+
                 "<div class=\"menu-content\">"+
                 " <ul class=\"main\">"+
                 "	<li class=\"home\"><a href=\"../server-admin/index.jsp\">Home</a></li>"+
                 "	<li class=\"manage\">Manage</li>"+
                 "	<li class=\"normal\">"+
                 "	<ul class=\"sub\">"+
                 "		<li class=\"list-ds\"><a href=\"../service-mgt/service_mgt.jsp\">Services</a></li>"+
                 "		<li>"+
                 "		<ul class=\"sub\">"+
                 "			<li class=\"list-ds\"><a href=\"../axis1/index.jsp\">Axis1</a></li>"+
                 "			<li class=\"list-ds\"><a href=\"../bpel/index.jsp\">BPEL</a></li>"+
                 "			<li class=\"list-ds\"><a href=\"../ejb/index.jsp\">EJB</a></li>"+
                 "			<li class=\"list-ds\"><a href=\"../jaxws/index.jsp\">JAXWS</a></li>"+
                 "			<li class=\"list-ds\"><a href=\"../pojo/index.jsp\">POJO</a></li>"+
                 "			<li class=\"list-ds\"><a href=\"../spring/index.jsp\">JAXWS</a></li>"+
                 "		</ul>"+
                 "		</li>"+
                 "		<li class=\"global\"><a href=\"../modulemgt/index.jsp\">Global Configurations</a></li>"+
                 "		<li class=\"logging\"><a href=\"../log-admin/index.html\" >Logging</a></li>"+
                 "		<li class=\"transports\"><a href=\"../transport-mgt/index.html\" >Transports</a></li>"+
                 "		<li class=\"security\">Security</li>"+
                 "		<li>"+
                 "		<ul class=\"sub\">"+
                 "			<li class=\"user-stores\"><a href=\"../security/userstoremgt/userstore-mgt.jsp\">User Stores</a></li>"+
                 "			<li class=\"user-groups\"><a href=\"../security/usergroupmgt/usergroup-mgt.jsp\" >User Groups</a></li>"+
                 "			<li class=\"keystores\"><a href=\"../security/keystoremgt/keystore-mgt.jsp\"  >Keystores</a></li>"+
                 "			<li class=\"users\"><a href=\"../security/usermgt/user-mgt.jsp\"  >Users</a></li>"+
                 "		</ul>"+
                 "		</li>"+
                 "		<li class=\"restart\"><a href=\"../server-admin/shutdown.jsp\">Shutdown/Restart</a></li>"+
                 "		<li class=\"restart\"><a href=\"../viewflows/index.jsp\">Flows</a></li>"+
                 "	</ul>"+
                 "	</li>"+
                 "	<li class=\"monitor\">Monitor</li>"+
                 "	<li class=\"normal\">"+
                 "	<ul class=\"sub\">"+
                 "		<li class=\"logs\"><a href=\"../log-view/index.html\" >Logs</a></li>"+
                 "		<li class=\"tracer\"><a href=\"../tracer/index.html\" >Tracer</a></li>"+
                 "		<li class=\"stats\"><a href=\"../statistics/index.html\" >Statistics</a></li>"+
                 "	</ul>"+
                 "	</li>"+
                 " </ul>"+
                 "</div>"+
                 "<div class=\"menu-bottom\"></div>"+
                 "</div>";
         }
         return menuContent;
     }

     */

    /*
        public static void main(String[] args) {
            MenuAdminClient a = new MenuAdminClient();
            a.populate();
            System.out.println(a.getMenuContent("region1",Locale.getDefault()));
            //System.out.println(a.findNavigationPathFromRoot("security_user_stores",""));
        }

        private void populate(){
            menus = new Menu[6];

            menus[0] = new Menu();
            menus[0].setId("root_home");
            menus[0].setI18nKey("component.home");
            menus[0].setI18nBundle("org.wso2.carbon.i18n.Resources");
            menus[0].setParentMenu("");
            menus[0].setLink("../home/link.html");
            menus[0].setRegion("region1");
            menus[0].setOrder("1");
            menus[0].setIcon("../home/home.ico");
            menus[0].setStyleClass("home");

            menus[1] = new Menu();
            menus[1].setId("root_manage");
            menus[1].setI18nKey("component.manage");
            menus[1].setI18nBundle("org.wso2.carbon.i18n.Resources");
            menus[1].setParentMenu("");
            menus[1].setLink("#");
            menus[1].setRegion("region1");
            menus[1].setOrder("2");
            menus[1].setIcon("../home/manage.ico");
            menus[1].setStyleClass("manage");

            menus[2] = new Menu();
            menus[2].setId("root_monitor");
            menus[2].setI18nKey("component.monitor");
            menus[2].setI18nBundle("org.wso2.carbon.i18n.Resources");
            menus[2].setParentMenu("");
            menus[2].setLink("#");
            menus[2].setRegion("region1");
            menus[2].setOrder("2");
            menus[2].setIcon("../home/monitor.ico");
            menus[2].setStyleClass("monitor");

            menus[3] = new Menu();
            menus[3].setId("manage_security");
            menus[3].setI18nKey("manage.security");
            menus[3].setI18nBundle("org.wso2.carbon.i18n.Resources");
            menus[3].setParentMenu("root_manage");
            menus[3].setLink("#");
            menus[3].setRegion("region1");
            menus[3].setOrder("2");
            menus[3].setIcon("../home/security.ico");
            menus[3].setStyleClass("security");


            menus[4] = new Menu();
            menus[4].setId("manage_transports");
            menus[4].setI18nKey("manage.transports");
            menus[4].setI18nBundle("org.wso2.carbon.i18n.Resources");
            menus[4].setParentMenu("root_manage");
            menus[4].setLink("#");
            menus[4].setRegion("region1");
            menus[4].setOrder("2");
            menus[4].setIcon("../home/transports.ico");
            menus[4].setStyleClass("transports");


            menus[5] = new Menu();
            menus[5].setId("security_user_stores");
            menus[5].setI18nKey("security.user-stores");
            menus[5].setI18nBundle("org.wso2.carbon.i18n.Resources");
            menus[5].setParentMenu("manage_security");
            menus[5].setLink("../security/userStore.jsp");
            menus[5].setRegion("region1");
            menus[5].setOrder("2");
            menus[5].setIcon("../home/transports.ico");
            menus[5].setStyleClass("user-stores");



            menus[6] = new Menu();
            menus[6].setId("security_user_groups");
            menus[6].setI18nKey("security.user-groups");
            menus[6].setI18nBundle("org.wso2.carbon.i18n.Resources");
            menus[6].setParentMenu("manage_security");
            menus[6].setLink("../security/userGroups.jsp");
            menus[6].setRegion("region1");
            menus[6].setOrder("2");
            menus[6].setIcon("../home/transports.ico");
            menus[6].setStyleClass("user-groups");

            menus[7] = new Menu();
            menus[7].setId("root_manage");
            menus[7].setI18nKey("component.manage");
            menus[7].setI18nBundle("org.wso2.carbon.i18n.Resources");
            menus[7].setParentMenu("");
            menus[7].setLink("#");
            menus[7].setRegion("region2");
            menus[7].setOrder("2");
            menus[7].setIcon("../home/manage.ico");
            menus[7].setStyleClass("manage");

            menus[8] = new Menu();
            menus[8].setId("security_user_stores");
            menus[8].setI18nKey("security.user-stores");
            menus[8].setI18nBundle("org.wso2.carbon.i18n.Resources");
            menus[8].setParentMenu("manage_security");
            menus[8].setLink("../security/userStore.jsp");
            menus[8].setRegion("region2");
            menus[8].setOrder("2");
            menus[8].setIcon("../home/transports.ico");
            menus[8].setStyleClass("user-stores");

            menus[9] = new Menu();
            menus[9].setId("manage_security");
            menus[9].setI18nKey("manage.security");
            menus[9].setI18nBundle("org.wso2.carbon.i18n.Resources");
            menus[9].setParentMenu("root_manage");
            menus[9].setLink("#");
            menus[9].setRegion("region2");
            menus[9].setOrder("2");
            menus[9].setIcon("../home/security.ico");
            menus[9].setStyleClass("security");
        }
    */



}
