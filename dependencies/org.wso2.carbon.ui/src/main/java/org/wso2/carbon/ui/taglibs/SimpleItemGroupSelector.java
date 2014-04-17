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
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * A tag for selecting & deselecting a group of items. This does not have any fancy functionality
 * like {@link org.wso2.carbon.ui.taglibs.ItemGroupSelector}. It simply select & deselect a group
 * of items.
 */
public class SimpleItemGroupSelector extends BodyTagSupport {

    private static final Log log = LogFactory.getLog(SimpleItemGroupSelector.class);
    protected String selectAllFunction;
    protected String selectNoneFunction;
    protected String selectAllKey;
    protected String selectNoneKey;
    protected String resourceBundle;

    public String getResourceBundle() {
        return resourceBundle;
    }

    public void setResourceBundle(String resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public String getSelectAllFunction() {
        return selectAllFunction;
    }

    public void setSelectAllFunction(String selectAllFunction) {
        this.selectAllFunction = selectAllFunction;
    }

    public String getSelectNoneFunction() {
        return selectNoneFunction;
    }

    public void setSelectNoneFunction(String selectNoneFunction) {
        this.selectNoneFunction = selectNoneFunction;
    }

    public String getSelectAllKey() {
        return selectAllKey;
    }

    public void setSelectAllKey(String selectAllKey) {
        this.selectAllKey = selectAllKey;
    }

    public String getSelectNoneKey() {
        return selectNoneKey;
    }

    public void setSelectNoneKey(String selectNoneKey) {
        this.selectNoneKey = selectNoneKey;
    }

    public int doEndTag() throws JspException {
        String selectAll = "Select all in all pages";
        String selectNone = "Select none";
        if (resourceBundle != null) {
            try {
                Locale locale = JSi18n.getLocaleFromPageContext(pageContext);
                ResourceBundle bundle = ResourceBundle.getBundle(resourceBundle,locale);
                selectAll = bundle.getString(selectAllKey);
                selectNone = bundle.getString(selectNoneKey);
            } catch (Exception e) {
                log.warn("Error while i18ning SimpleItemGroupSelector", e);
            }
        }

        JspWriter writer = pageContext.getOut();

        String content = "<a href=\"#\" onclick=\"" +
                         selectAllFunction + ";return false;\"  " +
                         "style=\"cursor:pointer\">" + selectAll + "</a>&nbsp<b>|</b>&nbsp;" +
                         "<a href=\"#\" onclick=\"" + selectNoneFunction + ";return false;\"  " +
                         "style=\"cursor:pointer\">" + selectNone + "</a>";
        try {
            writer.write(content);
        } catch (IOException e) {
            String msg = "Cannot write SimpleItemGroupSelector tag content";
            log.error(msg, e);
            throw new JspException(msg, e);
        }
        return 0;
    }
}