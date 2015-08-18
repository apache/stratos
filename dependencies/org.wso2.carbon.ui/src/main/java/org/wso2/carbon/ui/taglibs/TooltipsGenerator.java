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
import org.apache.taglibs.standard.tag.common.fmt.BundleSupport;
import org.wso2.carbon.ui.CarbonUIUtil;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This Tag Handler class is used to add a tooltips for a image.
 */
public class TooltipsGenerator extends BodyTagSupport {

    private static final Log log = LogFactory.getLog(TooltipsGenerator.class);
    //image location. tooltips appears when mouse over on this image.
    private String image;

    //tooltips body content.
    private String description;

    //no of words in a single line. this use to design the tooltips body. default value has set to 10.
    private int noOfWordsPerLine = 10;

    //resource bundle name.
    private String resourceBundle;

    //element in resource file.
    private String key;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getNoOfWordsPerLine() {
        return noOfWordsPerLine;
    }

    public void setNoOfWordsPerLine(int noOfWordsPerLine) {
        this.noOfWordsPerLine = noOfWordsPerLine;
    }

    public String getResourceBundle() {
        return this.resourceBundle;
    }

    public void setResourceBundle(String resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * This method is invoked when the custom end tag is encountered.
     * The main function of this method is to add a image and display a tooltips when mouse over
     * that image. This is done by calling the 'showTooltips' method in ../admin/js/widgets.js
     * java script file.
     * @return return EVAL_PAGE - continue processing the page.
     * @throws JspException
     */
    public int doEndTag() throws JspException {
        JspWriter writer = pageContext.getOut();
        String context = "<link rel='stylesheet' type='text/css' " +
                        "href='../yui/build/container/assets/skins/sam/container.css'>\n" +
                        "<script type=\"text/javascript\" " +
                        "src=\"../yui/build/yahoo-dom-event/yahoo-dom-event.js\"></script>\n" +
                        "<script type=\"text/javascript\" " +
                        "src=\"../yui/build/container/container-min.js\"></script>\n" +
                        "<script type=\"text/javascript\" " +
                        "src=\"../yui/build/element/element-min.js\"></script>\n" +
                        "<script type=\"text/javascript\" src=\"../admin/js/widgets.js\"></script>\n";


        if((getResourceBundle()!= null && getKey() != null) ||
                (getResourceBundle() == null && getKey() != null)) { //create tooltips content from resource bundle.
            context = context + "<a target='_blank' class='icon-link' " +
                            "onmouseover=\"showTooltip(this,'" + getTooltipsContentFromKey() + "')\" " +
                            "style='background-image:url(\"" + getImage() + "\")' ></a>";
        }
        else if(this.description != null) { //create the tooltips content from user given text.
            context = context + "<a target='_blank' class='icon-link' " +
                            "onmouseover=\"showTooltip(this,'" + createTooltipsBody(getNoOfWordsPerLine()) +
                    "')\" " +"style='background-image:url(\"" + getImage() + "\")' ></a>";
        }
         
        try {
            writer.write(context);
        } catch (IOException e) {
            String msg = "Cannot write tag content";
            throw new JspException(msg, e);
        }
        return EVAL_PAGE;
    }



    /**
     * This method is used to design the tooltips content display window size.
     * This takes the no of words that should contains in a single line as a argument.
     * So when we create tooltips body, its size vary accordingly.
     *
     * @param noOfWordsPerLine no of words that should contain in single line of tooltips body.
     * @return return the tooltips body content in displayable way in tooltips.
     */
    public String createTooltipsBody(int noOfWordsPerLine) {
        String toolTipContent = getDescription();
        if (toolTipContent != null) {
            String[] words = toolTipContent.split(" ");
            if (words.length > noOfWordsPerLine) {
                int countWords = 0;
                String descriptionNew = "";
                for (String word : words) {
                    if (countWords != noOfWordsPerLine) {
                        descriptionNew = descriptionNew + " " + word;
                        countWords++;
                    } else {
                        descriptionNew = descriptionNew + "<br>" + word;
                        countWords = 0;
                    }
                }
                setDescription(descriptionNew);
            }
        }
        return getDescription();
    }

    /**
     * This method is used to get the tooltips content from resource file.
     * @return return the tooltips body content in displayable way in tooltips.
     * @throws javax.servlet.jsp.JspException
     */
    public String getTooltipsContentFromKey() throws JspException {
        String toolTipsContent;
        ResourceBundle bundle = null;
        try {
            Locale locale = JSi18n.getLocaleFromPageContext(pageContext);
            if (getResourceBundle() != null && getKey() != null) {
                    bundle = ResourceBundle.getBundle(getResourceBundle(),locale);
            }
            //get the default bundle define in jsp page when only key is provided.
            else if (getResourceBundle() == null && getKey() != null) {
                    bundle = getCommonResourceBundleForJspPage();
            }
        } catch (Exception e) {
                log.warn("Error while i18ning.", e);
        }
        if (bundle != null) {
            toolTipsContent = bundle.getString(getKey());
            setDescription(toolTipsContent);
        }
        return createTooltipsBody(getNoOfWordsPerLine());
    }

    /**
     * This method is used to get the resource bundle define in a jsp page.
     * @return return the resource bundle
     */
    public ResourceBundle getCommonResourceBundleForJspPage() {
        Tag t = findAncestorWithClass(this, BundleSupport.class);
        LocalizationContext localizationContext;
        if (t != null) {
            // use resource bundle from parent <bundle> tag
            BundleSupport parent = (BundleSupport) t;
            localizationContext = parent.getLocalizationContext();

	    }  else {
            localizationContext = BundleSupport.getLocalizationContext(pageContext);
        }
        return localizationContext.getResourceBundle();
    }
}

