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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.ui.deployment.beans.Component;
import org.wso2.carbon.ui.deployment.beans.Menu;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wso2.carbon.CarbonConstants.COMPONENT_ELE;
import static org.wso2.carbon.CarbonConstants.GENERAL_ELE;
import static org.wso2.carbon.CarbonConstants.JS_FILES_ELE;
import static org.wso2.carbon.CarbonConstants.TAG_LIBS_ELE;
import static org.wso2.carbon.ui.Utils.transform;

/**
 * Deploy the component
 */
public class ComponentDeployer {
    /**
     *
     */
    private final String[] mainTemplateSuffixes =
            new String[]{"script_header", "menu", "main_layout"};

    /**
     *
     */
    private static Log log = LogFactory.getLog(ComponentDeployer.class);

    /**
     *
     */
    private Bundle componentBundle;

    /**
     *
     */
    private static final Map<String, String> processedFileMap = new HashMap<String, String>();


    public ComponentDeployer(Bundle componentBundle) {
        this.componentBundle = componentBundle;
    }

    public void layout(Map<Long, Component> componentMap) throws CarbonException {
        Collection<Component> componentCollection = componentMap.values();
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMElement componentEle = fac.createOMElement(new QName(COMPONENT_ELE));
        OMElement tagLibsEle = fac.createOMElement(new QName(TAG_LIBS_ELE));
        componentEle.addChild(tagLibsEle);
        OMElement jsFilesEle = fac.createOMElement(new QName(JS_FILES_ELE));
        OMElement generalEle = fac.createOMElement(new QName(GENERAL_ELE));
        componentEle.addChild(jsFilesEle);
        componentEle.addChild(generalEle);
//        for (Component component : componentCollection) {
//            constructIntermediateStruecte(component, tagLibsEle, jsFilesEle, fac);
//        }
        //leveling the menus first before adding to ims
        List<Menu> menuList = new ArrayList<Menu>();
        for (Component component : componentCollection) {
            menuList.addAll(component.getMenusList());
        }
        Collections.sort(menuList, new Comparator<Menu>() {
            public int compare(Menu m1, Menu m2) {
                return m1.compareTo(m2);
            }
        });
//        for (Menu menu : menuList) {
//            OMElement menuEle = fac.createOMElement(new QName(MENUE_ELE));
//            generalEle.addChild(menuEle);
//            Action action = menu.getAction();
//            menuEle.addAttribute(ACTION_REF_ATTR, action.getName(), fac.createOMNamespace("", ""));
//            menuEle.addAttribute(NAME_ATTR, menu.getName(), fac.createOMNamespace("", ""));
//            menuEle.addAttribute(LEVEL_ATTR, Integer.toString(menu.getLevel()),
//                                 fac.createOMNamespace("", ""));
//        }


        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        if(log.isDebugEnabled()){
            log.debug("intermediate : " + componentEle);
        }
        
        try {
            componentEle.serializeAndConsume(bos);
        } catch (XMLStreamException e) {
            e.printStackTrace();
            throw new CarbonException(e);
        }
        byte[] bytes = bos.toByteArray();

        try {
            // Transform
            for (String templatSuffix : mainTemplateSuffixes) {
                String xslResourceName = "ui/" + templatSuffix + ".xsl";
                URL xslResource = componentBundle.getResource(xslResourceName);
                if (xslResource == null) {
                    throw new CarbonException(
                            xslResourceName + " is not avaiable in component bundle");
                }
                ByteArrayOutputStream jspBos = new ByteArrayOutputStream();
                transform(new ByteArrayInputStream(bytes), xslResource.openStream(), jspBos);
                processedFileMap
                        .put("web/" + templatSuffix + ".jsp", new String(jspBos.toByteArray()));
            }
        } catch (TransformerException e) {
            e.printStackTrace();
            throw new CarbonException(e);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new CarbonException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CarbonException(e);
        }


    }

    /**
     * Processed map
     *
     * @param key key
     * @return value
     */
    public static String getFragment(String key) {
        return processedFileMap.get(key);
    }

//    private void constructIntermediateStruecte(Component component,
//                                               OMElement tabLibsEle,
//                                               OMElement jsFilesEle,
//                                               OMFactory fac) {
//        String effectivePath = component.getName() + "/" + component.getVersion() + "/";
//        List<TagLib> tagLibList = component.getTagLibList();
//        List<String> jsFilesList = component.getJsFilesList();
//        for (TagLib tagLib : tagLibList) {
//            OMElement tagLibEle = fac.createOMElement(new QName(TAG_LIB_ELE));
//            tagLibEle.addAttribute(URL_ATTR, tagLib.getUrl(), fac.createOMNamespace("", ""));
//            tagLibEle.addAttribute(PREFIX_ATTR, tagLib.getPrefix(), fac.createOMNamespace("", ""));
//            tabLibsEle.addChild(tagLibEle);
//        }
//        for (String fileName : jsFilesList) {
//            OMElement fileNameEle = fac.createOMElement(new QName(JS_FILE_ELE));
//            fileNameEle.setText(effectivePath + fileName);
//            jsFilesEle.addChild(fileNameEle);
//        }
//
//    }

}
