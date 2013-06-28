package org.wso2.carbon.tenant.mgt.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

public class InMemoryPasswordcallbackHandler  implements CallbackHandler {

    private static Map<String, String> keystorePassword = new HashMap<String, String>();

    public void handle(Callback[] callbacks)
            throws IOException, UnsupportedCallbackException {

        for (int i = 0; i < callbacks.length; i++) {

            if (callbacks[i] instanceof WSPasswordCallback) {
                WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
                String id = pc.getIdentifier();
                if (keystorePassword.get(id) != null) {
                    pc.setPassword(keystorePassword.get(id));
                } else {
                    throw new UnsupportedCallbackException(callbacks[i], "no password found for " + id);
                }
            }

        }
    }

    public static void addUser(String username, String password) {
        keystorePassword.put(username, password);
    }
}
