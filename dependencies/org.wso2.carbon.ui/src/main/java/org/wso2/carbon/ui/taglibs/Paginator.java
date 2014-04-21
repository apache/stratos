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
 * Implementation of the Paginator taglib
 */
public class Paginator extends BodyTagSupport {

    private static final Log log = LogFactory.getLog(Paginator.class);
    private int pageNumber;
    private int numberOfPages;
    private int noOfPageLinksToDisplay = 5;
    private String page;
    private String pageNumberParameterName;
    private String resourceBundle;
    private String nextKey;
    private String prevKey;
    private String action;
    private String parameters = "";
    private boolean showPageNumbers = true;


    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getPageNumberParameterName() {
        return pageNumberParameterName;
    }

    public void setPageNumberParameterName(String pageNumberParameterName) {
        this.pageNumberParameterName = pageNumberParameterName;
    }

    public String getResourceBundle() {
        return resourceBundle;
    }

    public void setResourceBundle(String resourceBundle) {
        this.resourceBundle = resourceBundle;
    }

    public String getNextKey() {
        return nextKey;
    }

    public void setNextKey(String nextKey) {
        this.nextKey = nextKey;
    }

    public String getPrevKey() {
        return prevKey;
    }

    public void setPrevKey(String prevKey) {
        this.prevKey = prevKey;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getShowPageNumbers() {
        return ""+ showPageNumbers;
    }

    public void setShowPageNumbers(String showPageNumbers) {
        this.showPageNumbers = Boolean.valueOf(showPageNumbers);
    }

    public int getNoOfPageLinksToDisplay() {
        return noOfPageLinksToDisplay;
    }

    public void setNoOfPageLinksToDisplay(int noOfPageLinksToDisplay) {
        this.noOfPageLinksToDisplay = noOfPageLinksToDisplay;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int doEndTag() throws JspException {
        String next = "next";
        String prev = "prev";
        if (resourceBundle != null) {
            try {
                Locale locale = JSi18n.getLocaleFromPageContext(pageContext);
                ResourceBundle bundle = ResourceBundle.getBundle(resourceBundle,locale);
                next = bundle.getString(nextKey);
                prev = bundle.getString(prevKey);
            } catch (Exception e) {
                log.warn("Error while i18ning paginator", e);
            }
        }

        JspWriter writer = pageContext.getOut();

        String content = "<table><tr>";
        if (numberOfPages > 1) {
            if (pageNumber > 0) {
                if(!"post".equals(action)){
                    content += "<td><strong><a href=\"" + page + "?" +
                            pageNumberParameterName + "=0" + "&" + parameters + "\">&lt;&lt;first" +
                            "&nbsp;&nbsp;</a></strong></td>" +
                            "<td><strong><a href=\"" + page + "?" +
                            pageNumberParameterName + "=" + (pageNumber - 1) + "&" + parameters + "\">"
                            + "&lt;&nbsp;" + prev + "&nbsp;&nbsp;</a></strong></td>";
                } else {
                    content += "<td><strong><a href=# onclick=\"doPaginate('" + page + "','" + pageNumberParameterName + "','" + (0) +"')\">&lt;&lt;first" +
                            "&nbsp;&nbsp;</a></strong></td>" +
                            "<td><strong><a href=# onclick=\"doPaginate('" + page + "','" + pageNumberParameterName + "','" + (pageNumber -1) +"')\">"
                            + "&lt;&nbsp;" + prev + "&nbsp;&nbsp;</a></strong></td>";
                }
            } else {
                content += "<td ><strong ><span style=\"color:gray\">" + "&lt;&lt; first " +
                        "&nbsp;&nbsp;&lt;" + prev + "&nbsp;&nbsp;</span></strong></td>";
            }

            if (showPageNumbers) {
                    int firstLinkNo;
                    int lastLinkNo;
                    if (noOfPageLinksToDisplay % 2 == 0) {

                        if ((pageNumber - (noOfPageLinksToDisplay / 2 - 1)) < 0) {
                            firstLinkNo = 0;
                        } else {
                            firstLinkNo = pageNumber - (noOfPageLinksToDisplay / 2 - 1);
                        }

                        if ((pageNumber + noOfPageLinksToDisplay / 2) > numberOfPages - 1) {
                            lastLinkNo = numberOfPages - 1;
                        } else {
                            lastLinkNo = pageNumber + noOfPageLinksToDisplay / 2;
                        }

                    } else {
                        if ((pageNumber - (int) Math.floor(noOfPageLinksToDisplay / 2)) < 0) {
                            firstLinkNo = 0;
                        } else {
                            firstLinkNo = pageNumber - (int) Math.floor(noOfPageLinksToDisplay / 2);
                        }

                        if ((pageNumber + (int) Math.floor(noOfPageLinksToDisplay / 2)) >
                                numberOfPages - 1) {
                            lastLinkNo = numberOfPages - 1;
                        } else {
                            lastLinkNo = pageNumber + (int) Math.floor(noOfPageLinksToDisplay / 2);
                        }
                    }
                    if (firstLinkNo != 0) {
                        content += "<td><strong> ... &nbsp;&nbsp;</strong></td> ";
                    }
                    for (int i = firstLinkNo; i <= lastLinkNo; i++) {
                        if (i == pageNumber) {
                            content += "<td><strong>" + (i + 1) + "&nbsp;&nbsp;</strong></td>";
                        } else {
                            if(!"post".equals(action)){
                                content += "<td><strong><a href=\"" + page + "?" +
                                        pageNumberParameterName + "=" + i + "&" + parameters + "\">" +
                                        (i + 1) + " &nbsp;&nbsp;</a></strong></td>";
                            } else {
                                content += "<td><strong>" +
                                        "<a href=# onclick=\"doPaginate('" + page + "','" + pageNumberParameterName + "','" + (i) +"')\">" +
                                        (i + 1) + " &nbsp;&nbsp;</a></strong></td>";
                            }
                        }
                    }

                    if (lastLinkNo != numberOfPages - 1) {
                        content += "<td><strong> ... &nbsp;&nbsp;</strong></td> ";
                    }
            } else {
                content += "<td><strong> Page &nbsp;&nbsp;" + (pageNumber + 1) + " of  " +
                        numberOfPages + " &nbsp;&nbsp;</strong></td>";
            }

            if (pageNumber < numberOfPages - 1) {
                if(!"post".equals(action)){
                    content += "<td ><strong ><a href =\"" + page + "?" +
                            pageNumberParameterName + "=" + (pageNumber + 1) + "&" + parameters + "\">"
                            + next + "&nbsp;&gt;</a></strong></td>"
                            + "<td ><strong ><a href =\"" + page + "?" +
                            pageNumberParameterName + "=" + (numberOfPages - 1) + "&" + parameters
                            + "\">" + "&nbsp;&nbsp;last" + "&nbsp;&gt;&gt;</a></strong></td>";
                } else {
                    content += "<td ><strong><a href=# onclick=\"doPaginate('" + page + "','" + pageNumberParameterName + "','" + (pageNumber +1) +"')\">"
                            + next + "&nbsp;&gt;</a></strong></td>"
                            + "<td ><strong ><a href=# onclick=\"doPaginate('" + page + "','" + pageNumberParameterName + "','" + (numberOfPages - 1) +"')\">" +
                            "&nbsp;&nbsp;last" + "&nbsp;&gt;&gt;</a></strong></td>";
                }
            } else {
                content += "<td ><strong ><span style=\"color:gray\">" + next + " &gt;&nbsp;&nbsp;"
                        + "last" + "&gt;&gt; " + "</span></strong></td>";
            }
        }
        content += "</tr ></table > ";
        try {
            writer.write(content);
        } catch (IOException e) {
            String msg = "Cannot write paginator tag content";
            log.error(msg, e);
            throw new JspException(msg, e);
        }
        return 0;
    }
}
