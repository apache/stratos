/*                                                                             
 * Copyright 2004,2005 The Apache Software Foundation.                         
 *                                                                             
 * Licensed under the Apache License, Version 2.0 (the "License");             
 * you may not use this file except in compliance with the License.            
 * You may obtain a copy of the License at                                     
 *                                                                             
 *      http://www.apache.org/licenses/LICENSE-2.0                             
 *                                                                             
 * Unless required by applicable law or agreed to in writing, software         
 * distributed under the License is distributed on an "AS IS" BASIS,           
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    
 * See the License for the specific language governing permissions and         
 * limitations under the License.                                              
 */
package org.wso2.carbon.ui.taglibs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.ui.CarbonUIUtil;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A tag for selecting & deselecting a group of items & applying some common operation
 * on these items
 */
public class ItemGroupSelector extends SimpleItemGroupSelector {

    private static final Log log = LogFactory.getLog(ItemGroupSelector.class);
    private String selectAllInPageFunction;
    private String addRemoveKey;
    private String addRemoveFunction;
    private String addRemoveButtonId;
    private String selectAllInPageKey;
    private int numberOfPages = 1;
    private String extraHtml;

    public String getSelectAllInPageFunction() {
        return selectAllInPageFunction;
    }

    public void setSelectAllInPageFunction(String selectAllInPageFunction) {
        this.selectAllInPageFunction = selectAllInPageFunction;
    }

    public String getAddRemoveFunction() {
        return addRemoveFunction;
    }

    public void setAddRemoveFunction(String addRemoveFunction) {
        this.addRemoveFunction = addRemoveFunction;
    }

    public String getAddRemoveButtonId() {
        return addRemoveButtonId;
    }

    public void setAddRemoveButtonId(String addRemoveButtonId) {
        this.addRemoveButtonId = addRemoveButtonId;
    }

    public String getSelectAllInPageKey() {
        return selectAllInPageKey;
    }

    public void setSelectAllInPageKey(String selectAllInPageKey) {
        this.selectAllInPageKey = selectAllInPageKey;
    }

    public String getAddRemoveKey() {
        return addRemoveKey;
    }

    public void setAddRemoveKey(String addRemoveKey) {
        this.addRemoveKey = addRemoveKey;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public String getExtraHtml() {
        return extraHtml;
    }

    public void setExtraHtml(String extraHtml) {
        this.extraHtml = extraHtml;
    }

    public int doEndTag() throws JspException {
        
        String selectAllInPage = (selectAllInPageKey != null) ? selectAllInPageKey : "Select all in this page";
        String selectAll = (selectAllKey != null) ? selectAllKey : "Select all in all pages";
        String selectNone = (selectNoneKey != null) ? selectNoneKey : "Select none";
        String addRemove = (addRemoveKey != null) ? addRemoveKey : "Remove";

        if (resourceBundle != null) {
            try {
                Locale locale = JSi18n.getLocaleFromPageContext(pageContext);
                ResourceBundle bundle = ResourceBundle.getBundle(resourceBundle,locale);
                selectAllInPage = bundle.getString(selectAllInPageKey);
                selectAll = bundle.getString(selectAllKey);
                selectNone = bundle.getString(selectNoneKey);
                if (addRemoveKey != null) {
                    addRemove = bundle.getString(addRemoveKey);
                } else {
                    addRemove = "";
                }
            } catch (Exception e) {
                log.warn("Error while i18ning ItemGroupSelector", e);
            }
        }
        
        JspWriter writer = pageContext.getOut();

        String content = "<table>" +
                         "<tr>" +
                         "<td><a href=\"#\" onclick=\"" + selectAllInPageFunction + ";return false;\"  " +
                         "style=\"cursor:pointer\">" + selectAllInPage + "</a>&nbsp<b>|</b>&nbsp;" +
                         "</td>";
        if (numberOfPages > 1) {
            content += "<td><a href=\"#\" onclick=\"" + selectAllFunction + ";return false;\"  " +
                       "style=\"cursor:pointer\">" + selectAll + "</a>&nbsp<b>|</b>&nbsp;</td>";
        }

        content += "<td><a href=\"#\" onclick=\"" + selectNoneFunction + ";return false;\"  " +
                   "style=\"cursor:pointer\">" + selectNone + "</a></td>" +
                   "<td width=\"20%\">&nbsp;</td>";

        if(addRemoveButtonId != null){

            content += ("<td><a href='#' id=\"" + addRemoveButtonId + "\" onclick=\"" + addRemoveFunction + ";return false;\">" +addRemove );
            content +=  "</a></td>";

        }
            content += ( extraHtml != null ? "<td>" + extraHtml + "</td>" : "") +
                   "</tr>" +
                   "</table>";
        try {
            writer.write(content);
        } catch (IOException e) {
            String msg = "Cannot write ItemSelector tag content";
            log.error(msg, e);
            throw new JspException(msg, e);
        }
        return 0;
    }
}
