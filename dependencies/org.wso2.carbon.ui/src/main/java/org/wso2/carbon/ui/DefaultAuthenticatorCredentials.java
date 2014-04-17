package org.wso2.carbon.ui;

public class DefaultAuthenticatorCredentials {

    private String userName;
    private String password;

    public DefaultAuthenticatorCredentials(String userName, String password) {
        super();
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

}
