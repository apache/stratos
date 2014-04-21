/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
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

public class ResourcePaginator extends Paginator {

    private static final String END = "end";

	private static final String MIDDLE = "middle";

	private static final String START = "start";

	private static final Log log = LogFactory.getLog(ResourcePaginator.class);

    private String paginationFunction = null;
    private int tdColSpan = 0;

    public String getPaginationFunction() {
        return paginationFunction;
    }

    public void setPaginationFunction(String paginationFunction) {
        this.paginationFunction = paginationFunction;
    }

    public int getTdColSpan() {
        return tdColSpan;
    }

    public void setTdColSpan(int tdColSpan) {
        this.tdColSpan = tdColSpan;
    }

    public int doEndTag() throws JspException {
        if (getNumberOfPages() < 2) {
            // Pagination is not required.
            return 0;
        }
        String next = "Next";
        String prev = "Prev";
        String pageName = "Page {0}";
        if (getResourceBundle() != null) {
            try {
                Locale locale = JSi18n.getLocaleFromPageContext(pageContext);
                ResourceBundle bundle = ResourceBundle.getBundle(getResourceBundle(),locale);
                next = bundle.getString(getNextKey());
                prev = bundle.getString(getPrevKey());
            } catch (Exception e) {
                log.warn("Error while i18ning paginator", e);
            }
        }

        JspWriter writer = pageContext.getOut();

        StringBuffer content = new StringBuffer("<tr><td ");
        if (tdColSpan > 0) {
            content.append("colspan=\"").append(tdColSpan).append("\" ");
        }
        content.append("class=\"pagingRow\" style=\"text-align:center;padding-top:10px; padding-bottom:10px;\">");

        if (getPageNumber() == 1) {
            content.append("<span class=\"disableLink\">< ").append(prev).append("</span>");
        } else {
            content.append("<a class=\"pageLinks\" title=\"").append(pageName.replace("{0}",
                    Integer.toString(getPageNumber() - 1))).append("\"");
            if (getPaginationFunction() != null) {
                content.append("onclick=\"").append(getPaginationFunction().replace("{0}",
                        Integer.toString(getPageNumber() - 1))).append("\"");
            }
            content.append(">< ").append(prev).append("</a>");
        }
        if (getNumberOfPages() <= 10) {
            for (int pageItem = 1; pageItem <= getNumberOfPages(); pageItem++) {
                content.append("<a title=\"").append(pageName.replace("{0}",
                    Integer.toString(pageItem))).append("\" class=\"");
                if (getPageNumber()==pageItem) {
                    content.append("pageLinks-selected\"");
                } else {
                    content.append("pageLinks\"");
                }
                if (getPaginationFunction() != null) {
                    content.append("onclick=\"").append(getPaginationFunction().replace("{0}",
                            Integer.toString(pageItem))).append("\"");
                }
                content.append(">").append(pageItem).append("</a>");
            }
        } else {
            // FIXME: The equals comparisons below looks buggy. Need to test whether the desired
            // behaviour is met, when there are more than ten pages.
            String place = MIDDLE;
            int pageItemFrom = getPageNumber() - 2;
            int pageItemTo = getPageNumber() + 2;

            if (getNumberOfPages() - getPageNumber() <= 5) {
            	place = END;
            }
            if (getPageNumber() <= 5) {
            	place = START;
            }

            if (START.equals(place)) {
                pageItemFrom = 1;
                pageItemTo = 7;
            }
            if (END.equals(place)) {
                pageItemFrom = getNumberOfPages() - 7;
                pageItemTo = getNumberOfPages();
            }

            if (END.equals(place)  || MIDDLE.equals(place)) {
                for (int pageItem = 1; pageItem <= 2; pageItem++) {
                    content.append("<a title=\"").append(pageName.replace("{0}",
                            Integer.toString(pageItem))).append("\" class=\"pageLinks\"");
                    if (getPaginationFunction() != null) {
                        content.append("onclick=\"").append(getPaginationFunction().replace("{0}",
                                Integer.toString(pageItem))).append("\"");
                    }
                    content.append(">").append(pageItem).append("</a>");
                }
                content.append("...");
            }

            for (int pageItem = pageItemFrom; pageItem <= pageItemTo; pageItem++) {
                content.append("<a title=\"").append(pageName.replace("{0}",
                    Integer.toString(pageItem))).append("\" class=\"");
                if (getPageNumber()==pageItem) {
                    content.append("pageLinks-selected\"");
                } else {
                    content.append("pageLinks\"");
                }
                if (getPaginationFunction() != null) {
                    content.append("onclick=\"").append(getPaginationFunction().replace("{0}",
                            Integer.toString(pageItem))).append("\"");
                }
                content.append(">").append(pageItem).append("</a>");
            }

            if (START.equals(place) || MIDDLE.equals(place)) {
                content.append("...");
                for (int pageItem = (getNumberOfPages() - 1); pageItem <= getNumberOfPages(); pageItem++) {

                    content.append("<a title=\"").append(pageName.replace("{0}",
                            Integer.toString(pageItem))).append("\" class=\"pageLinks\"");
                    if (getPaginationFunction() != null) {
                        content.append("onclick=\"").append(getPaginationFunction().replace("{0}",
                                Integer.toString(pageItem))).append("\"");
                    }
                    content.append("style=\"margin-left:5px;margin-right:5px;\">").append(
                            pageItem).append("</a>");
                }
            }
        }
        if (getPageNumber() == getNumberOfPages()) {
           content.append("<span class=\"disableLink\">").append(next).append(" ></span>");
        } else {
            content.append("<a class=\"pageLinks\" title=\"").append(pageName.replace("{0}",
                    Integer.toString(getPageNumber() + 1))).append("\"");
            if (getPaginationFunction() != null) {
                content.append("onclick=\"").append(getPaginationFunction().replace("{0}",
                        Integer.toString(getPageNumber() + 1))).append("\"");
            }
            content.append(">").append(next).append(" ></a>");
        }
        content.append("<span id=\"xx").append(getPageNumber()).append(
                "\" style=\"display:none\" /></td></tr>");
        try {
            writer.write(content.toString());
        } catch (IOException e) {
            String msg = "Cannot write paginator tag content";
            log.error(msg, e);
            throw new JspException(msg, e);
        }
        return 0;
    }

}
