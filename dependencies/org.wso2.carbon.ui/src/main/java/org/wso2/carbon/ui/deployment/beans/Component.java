/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.ui.deployment.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Component {
    private List<Menu> menusList;
    private List<Servlet> servletList;
    private List<FileUploadExecutorConfig> fUploadExecConfigList;
    private String name;
    private String version;
    //menu entries containing skip-authentication=false in component.xml
    private List<String> unauthenticatedUrlList;
    private List<String> skipTilesUrlList;
    private List<String> skipHttpsUrlList;

    private Map<String, String> customViewUIMap = new HashMap<String, String>();
    private Map<String, String> customAddUIMap = new HashMap<String, String>();

    private List<Context> contextsList;

    public Component() {
        this.menusList = new ArrayList<Menu>();
        this.servletList = new ArrayList<Servlet>();
        this.fUploadExecConfigList = new ArrayList<FileUploadExecutorConfig>();
        this.unauthenticatedUrlList = new ArrayList<String>();
        this.skipTilesUrlList = new ArrayList<String>();
        this.skipHttpsUrlList = new ArrayList<String>();
        this.contextsList = new ArrayList<Context>();
    }

    public List<Context> getContextsList() {
        return contextsList;
    }

    public void addContext(Context context) {
        this.contextsList.add(context);
    }

    public List<String> getSkipHttpsUrlList() {
        return skipHttpsUrlList;
    }

    public void addSkipHttpsUrlList(String skipHttpUrl) {
        skipHttpsUrlList.add(skipHttpUrl);
    }

    public List<String> getUnauthenticatedUrlList() {
        return unauthenticatedUrlList;
    }

    public void addUnauthenticatedUrl(String unauthenticatedUrl) {
        unauthenticatedUrlList.add(unauthenticatedUrl);
    }

    public List<String> getSkipTilesUrlList() {
        return skipTilesUrlList;
    }

    public void addSkipTilesUrl(String skipTilesUrl) {
        skipTilesUrlList.add(skipTilesUrl);
    }

    public void addMenu(Menu menu) {
        menusList.add(menu);
    }

    public List<Menu> getMenusList() {
        return menusList;
    }

    public void addServlet(Servlet servlet) {
        servletList.add(servlet);
    }

    public List<Servlet> getServletList() {
        return servletList;
    }

    public Servlet[] getServlets() {
        if (servletList != null) {
            Servlet[] servlets = new Servlet[servletList.size()];
            servletList.toArray(servlets);
            return servlets;
        } else {
            return null;
        }
    }

    public Menu[] getMenus() {
        if (menusList != null) {
            Menu[] menus = new Menu[menusList.size()];
            menusList.toArray(menus);
            return menus;
        } else {
            return null;
        }
    }

    public FileUploadExecutorConfig[] getFileUploadExecutorConfigs() {
        FileUploadExecutorConfig[] fUploadExecConfigs = new FileUploadExecutorConfig[fUploadExecConfigList.size()];
        fUploadExecConfigList.toArray(fUploadExecConfigs);
        return fUploadExecConfigs;
    }

    public void addFileUploadExecutorConfig(FileUploadExecutorConfig fileUploadExecutorConfig) {
        this.fUploadExecConfigList.add(fileUploadExecutorConfig);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getCustomViewUIMap() {
        return customViewUIMap;
    }

    public void addCustomViewUI(String mediaType, String uiPath) {
        customViewUIMap.put(mediaType, uiPath);
    }

    public Map<String, String> getCustomAddUIMap() {
        return customAddUIMap;
    }

    public void addCustomAddUI(String mediaType, String uiPath) {
        customAddUIMap.put(mediaType, uiPath);
    }
}

