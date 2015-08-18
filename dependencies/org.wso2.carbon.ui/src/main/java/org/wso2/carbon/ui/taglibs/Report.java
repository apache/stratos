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

import org.wso2.carbon.ui.CarbonUIUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;

/**
 * used to generate reporting UI
 */
public class Report extends BodyTagSupport {

    private String component;
    private String template;
    private boolean pdfReport;
    private boolean htmlReport;
    private boolean excelReport;
    private String reportDataSession;

    public String getReportDataSession() {
        return reportDataSession;
    }

    public void setReportDataSession(String reportDataSession) {
        this.reportDataSession = reportDataSession;
    }




    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public boolean isPdfReport() {
        return pdfReport;
    }

    public void setPdfReport(boolean pdfReport) {
        this.pdfReport = pdfReport;
    }

    public boolean isHtmlReport() {
        return htmlReport;
    }

    public void setHtmlReport(boolean htmlReport) {
        this.htmlReport = htmlReport;
    }

    public boolean isExcelReport() {
        return excelReport;
    }

    public void setExcelReport(boolean excelReport) {
        this.excelReport = excelReport;
    }


    public int doStartTag() throws JspException {
        //check permission.
        HttpServletRequest req = (HttpServletRequest)
                pageContext.getRequest();
        if(!CarbonUIUtil.isUserAuthorized(req, "/permission/admin/manage/report")){
          return EVAL_PAGE;
        }
        JspWriter writer = pageContext.getOut();

        String context = "<div style='float:right;padding-bottom:5px;padding-right:15px;'>";

        if(pdfReport){
           context  = context+ "<a target='_blank' class='icon-link' style='background-image:url(../admin/images/pdficon.gif);' href=\"../report" + "?" +"reportDataSession="+ reportDataSession + "&component=" + component + "&template=" + template + "&type=pdf" +  "\">Generate Pdf Report</a>";
        }
        if(htmlReport){
            context  = context+ "<a target='_blank' class='icon-link' style='background-image:url(../admin/images/htmlicon.gif);' href=\"../report" + "?" + "reportDataSession="+ reportDataSession + "&component=" + component + "&template=" + template + "&type=html" + "\">Generate Html Report</a>";

        }
        if(excelReport){
            context  = context+ "<a target='_blank' class='icon-link' style='background-image:url(../admin/images/excelicon.gif);' href=\"../report" + "?" + "reportDataSession="+ reportDataSession + "&component=" + component + "&template=" + template + "&type=excel" +"\">Generate Excel Report</a>";

        }
        context  = context + "</div><div style='clear:both;'></div>";

        try {
            writer.write(context);
        } catch (IOException e) {
            String msg = "Cannot write reporting tag content";

            throw new JspException(msg, e);
        }
        return EVAL_PAGE;


    }
}




