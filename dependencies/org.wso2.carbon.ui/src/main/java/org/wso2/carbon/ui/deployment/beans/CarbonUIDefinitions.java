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
package org.wso2.carbon.ui.deployment.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.ui.MenuAdminClient;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CarbonUIDefinitions {
    private static Log log = LogFactory.getLog(CarbonUIDefinitions.class);
    private List<Menu> menuDefinitions = new CopyOnWriteArrayList<Menu> ();
    private List<Servlet> servletDefinitions = new ArrayList<Servlet>();
    private HashMap<String, String> unauthenticatedUrls = new HashMap<String, String>();
    private HashMap<String, String> skipTilesUrls = new HashMap<String, String>();
    private HashMap<String, String> httpUrls = new HashMap<String, String>();
    private HashMap<String, Context> contexts = new HashMap<String, Context>();

    public void addContexts(List<Context> contexts) {
        for (Context ctx : contexts){
            addContexts(ctx.getContextId(), ctx);
        }
    }

    public void removeContexts(List<Context> contexts){
        for (Context ctx : contexts){
            removeContexts(ctx);
        }
    }

    public void addContexts(String contextId, Context context) {
        contexts.put(contextId, context);
    }

    public void removeContexts(Context context){
        contexts.remove(context.getContextId());
    }

    public HashMap<String, Context> getContexts() {
        return contexts;
    }

    public void addHttpUrls(List<String> urls) {
         for(String url : urls){
            addHttpUrls(url);
        }
    }

    public void removeHttpUrls(List<String> urls) {
        for(String url : urls){
            removeHttpUrls(url);
        }
    }

    public void addHttpUrls(String url) {
        httpUrls.put(url, url);
    }

    public void removeHttpUrls(String url){
        httpUrls.remove(url);
    }

    public HashMap<String, String> getHttpUrls() {
        return httpUrls;
    }

    public void addUnauthenticatedUrls(List<String> urls) {
        for (String url : urls) {
            addUnauthenticatedUrls(url);
        }
    }

    public void removeUnauthenticatedUrls(List<String> urls) {
        for (String url : urls) {
            removeUnauthenticatedUrls(url);
        }
    }

    public void addUnauthenticatedUrls(String url) {
        unauthenticatedUrls.put(url, url);
    }

    public void removeUnauthenticatedUrls(String url) {
        unauthenticatedUrls.remove(url);
    }

    public HashMap<String, String> getUnauthenticatedUrls() {
        return unauthenticatedUrls;
    }

    public void addSkipTilesUrls(List<String> urls) {
         for(String url : urls){
            addSkipTilesUrls(url);
        }
    }


    public void removeSkipTilesUrls(List<String> urls){
        for(String url : urls){
           removeSkipTilesUrls(url);
        }
    }

    public void addSkipTilesUrls(String url) {
        skipTilesUrls.put(url, url);
    }

    public void removeSkipTilesUrls(String url){
        skipTilesUrls.remove(url);
    }

    public HashMap<String, String> getSkipTilesUrls() {
        return skipTilesUrls;
    }


    /**
     * Removes a given menu item from menu definitions.
     * Menu items available to all users will be effected.
     *
     * @param menuId
     * @see CarbonUIUtil#removeMenuDefinition(String, javax.servlet.http.HttpServletRequest)
     */
    public void removeMenuDefinition(String menuId) {  //did not change the paramType to Menu
        //TODO : consider removing child menu items as well
        if (menuId != null && menuId.trim().length() > 0) {
            for (Menu menu : menuDefinitions) {
                    if (menu != null && menuId.equals(menu.getId())) {
                        menuDefinitions.remove(menu);
                        if (log.isDebugEnabled()) {
                            log.debug("Removing menu item : " + menuId);
                        }
                    }

            }
        }
    }

    /**
     * @param loggedInUserName
     * @param userPermissions
     * @param request
     * @return
     */
    public Menu[] getMenuDefinitions(String loggedInUserName,
                                     boolean isSuperTenant,
                                     ArrayList<String> userPermissions,
                                     HttpServletRequest request) {
        return getMenuDefinitions(loggedInUserName, isSuperTenant, userPermissions, request,
                menuDefinitions.toArray(new Menu[0]));
    }

    /**
     * Get menu definitions based on Logged in user & user permission
     *
     * @param loggedInUserName
     * @param userPermissions
     * @return
     */
    public Menu[] getMenuDefinitions(String loggedInUserName,
                                     boolean isSuperTenant,
                                     ArrayList<String> userPermissions,
                                     HttpServletRequest request,
                                     Menu[] currentMenuItems) {
        if (loggedInUserName != null) {
            //If a user is logged in, send filtered set of menus
            ArrayList<Menu> filteredMenuDefs = new ArrayList<Menu>();

            //Get permissions granted for logged in user
            //HashMap<String,String> grantedPermissions = getGrantedPermissions(loggedInUserName, userPermissions);

            for (int a = 0; a < currentMenuItems.length; a++) {
                String serverInServiceMode = ServerConfiguration.getInstance().
                        getFirstProperty(CarbonConstants.IS_CLOUD_DEPLOYMENT);
                Menu menu = currentMenuItems[a];
                boolean continueAdding = true;
                if (menu.isRequireSuperTenant() && !isSuperTenant) {
                    continueAdding = false;
                    if (log.isDebugEnabled()) {
                        log.debug("User : " + loggedInUserName +
                                " is not logged in from super tenant to access menu -> " + menu);
                    }
                } else if (menu.isRequireNotSuperTenant() && isSuperTenant) {
                    continueAdding = false;
                    if (log.isDebugEnabled()) {
                        log.debug("User : " + loggedInUserName +
                                " is not logged in from non super tenant to access menu -> " + menu);
                    }

                } else if (menu.isRequireNotLoggedIn() && loggedInUserName != null) {
                    continueAdding = false;
                    if (log.isDebugEnabled()) {
                        log.debug("User : " + loggedInUserName +
                                " is logged in, the menu only shows when not logged in -> " + menu);
                    }

                } else if (menu.isRequireCloudDeployment() && !"true".equals(serverInServiceMode)) {
                    continueAdding = false;
                    if (log.isDebugEnabled()) {
                        log.debug("Server is not running in a cloud deployment, " +
                                  "the menu is only shown when running in cloud deployment -> " + menu);
                    }
                }
                if (continueAdding) {
                    String[] requiredPermissions = menu.getRequirePermission();
                    int grantCount = 0;
                    for (String requiredPermission : requiredPermissions) {
                        int temp = grantCount;
                        for (String grantedPermission : userPermissions) {
                            if ("*".equals(requiredPermission)) {
                                grantCount++;
                                break;
                            } else {
                                if (!requiredPermission.startsWith("/")) {
                                    grantCount = requiredPermissions.length;
                                    log.error(" Attention :: Permission issue in Menu item "
                                            + menu.getId());
                                    break;
                                }

                                if (requiredPermission.startsWith(grantedPermission)) {
                                    grantCount++;
                                    break;
                                }
                            }
                        }
                        if (temp == grantCount && !menu.isAtLeastOnePermissionsRequired()) {
                            grantCount = 0;
                            break;
                        }
                    }
                    if (grantCount >= requiredPermissions.length || grantCount > 0 &&
                            menu.isAtLeastOnePermissionsRequired()) {
                        filteredMenuDefs.add(menu);
                    }

                }
            }
            Menu[] filteredMenus = new Menu[filteredMenuDefs.size()];
            request.getSession().setAttribute(MenuAdminClient.USER_MENU_ITEMS_FILTERED, "true");
            return filteredMenuDefs.toArray(filteredMenus);

        } else {
            request.getSession().setAttribute(MenuAdminClient.USER_MENU_ITEMS_FILTERED, "false");
            return currentMenuItems;
        }
    }


    private HashMap<String, String> getGrantedPermissions(String userName,
                                                          ArrayList<String> userPermissions) {
        HashMap<String, String> userRoles = new HashMap<String, String>();
        Iterator<String> iterator = userPermissions.iterator();
        while (iterator.hasNext()) {
            String permission = iterator.next();
            if (log.isDebugEnabled()) {
                log.debug("User : " + userName + " has permission : " + permission);
            }
            userRoles.put(permission, permission);
        }
        return userRoles;
    }



    public void addMenuItems(Menu[] newMenuDefinitions) {
        for (Menu menu : newMenuDefinitions) {
            menuDefinitions.add(menu);
            if (log.isDebugEnabled()) {
            log.debug("Adding new menu itesm : "+ menu);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Listing all menu items as of now...");
            for (Menu menu : menuDefinitions) {
                log.debug("--->" + menu);
            }
        }
    }

    /**
     * @param menuDefinitionsToBeRemoved
     */
    public void removeMenuItems(Menu[] menuDefinitionsToBeRemoved) {
       for(Menu menu : menuDefinitionsToBeRemoved){
           removeMenuDefinition(menu.getId());
       }
    }

    /**
     * @return
     */
    public Servlet[] getServletDefinitions() {
        return servletDefinitions.toArray(new Servlet[0]);
    }




    /**
     * @param newServletDefinitions
     */
    public void addServletItems(Servlet[] newServletDefinitions) {

        for (Servlet servlet : newServletDefinitions) {
            this.servletDefinitions.add(servlet);
            if(log.isDebugEnabled()){
            log.debug("Added new servlet definition " + servlet);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Listing all Servlet items as of now...");
            for (Servlet servlet : servletDefinitions ) {
                log.debug("--->" + servlet);
            }
        }
    }

    public  void removeServletItems(Servlet[] servletDefinitionsToRemoved) {
        for(Servlet servlet : servletDefinitionsToRemoved){
            if(this.servletDefinitions.contains(servlet)) {
                this.servletDefinitions.remove(servlet);
            }
            if(log.isDebugEnabled()){
                log.debug("removing the servlet definition : " + servlet);

            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Listing all Servlet items as of now...");
            for (Servlet servlet : servletDefinitions ) {
                log.debug("--->" + servlet);
            }
        }

    }
}
