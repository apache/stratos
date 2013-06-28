package org.wso2.carbon.s2.gitblit.service;


import java.io.File;
import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;
import org.wso2.carbon.adc.repository.mgt.service.AuthenticationAdminAuthenticationExceptionException;
import org.wso2.carbon.adc.repository.mgt.service.AuthenticationAdminStub;

import com.gitblit.GitBlit;
import com.gitblit.GitblitUserService;
import com.gitblit.IStoredSettings;
import com.gitblit.models.UserModel;

public class GitAuthService extends GitblitUserService {
	 
	public GitAuthService() {
	    super();
    }

	
	@Override
    public void setup(IStoredSettings settings) {
        String file = "/opt/GitBlit/data/users.conf";
        File realmFile = GitBlit.getFileOrFolder(file);
        serviceImpl = createUserService(realmFile);
        
    }
	
	@Override
	public UserModel authenticate(char[] cookie) {
	    return super.authenticate(cookie);
	}
	

	
	@Override
	public UserModel authenticate(String username, char[] password) {
		
		System.out.println("Authenticating..");

		UserModel user = new UserModel(username);

		String passwordString = new String(password);
		System.out.println("username: " + username);
		System.out.println("pwd: " + passwordString);

		boolean isLoggedin = false;

		try {
			String trustStorePath = "/opt/GitBlit/wso2carbon.jks";
			System.setProperty("javax.net.ssl.trustStore", trustStorePath);
			System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
			AuthenticationAdminStub stub =
			                               new AuthenticationAdminStub(
			                                                           "https://localhost:9445/services/AuthenticationAdmin");
			isLoggedin = stub.login(username, passwordString, "127.0.0.1");
			System.out.println("Login state : " + isLoggedin);
		} catch (Exception e) {
			System.out.println("Exception occurred.. " + e.getMessage());
			e.printStackTrace();
		}

		if (isLoggedin) {
			System.out.println("Successfully authenticated via Authentication Admin..");
			user.canAdmin = true;
			return user;
		} else {
			System.out.println("Authentication admin authentication failed. Trying internal user service");
			user = super.authenticate(username, password);
			user.canAdmin = true;
			return user;
		}
	}
	
	
		
}

