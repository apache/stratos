package org.apache.stratos.rest.endpoint.bean;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "error")
public class Error {
    private int errorCode;
    private String errorMessage;

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
