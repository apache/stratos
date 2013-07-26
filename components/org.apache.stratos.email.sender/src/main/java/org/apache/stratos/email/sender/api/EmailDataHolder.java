package org.apache.stratos.email.sender.api;

import java.util.Map;

//This holds the email address and email parameter map
public class EmailDataHolder {

    private String email;
    private Map<String, String> emailParameters;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Map<String, String> getEmailParameters() {
        return emailParameters;
    }

    public void setEmailParameters(Map<String, String> emailParameters) {
        this.emailParameters = emailParameters;
    }
}
